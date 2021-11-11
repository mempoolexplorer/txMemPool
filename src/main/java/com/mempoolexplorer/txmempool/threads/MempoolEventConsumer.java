package com.mempoolexplorer.txmempool.threads;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.mempoolexplorer.txmempool.bitcoindadapter.entites.Transaction;
import com.mempoolexplorer.txmempool.bitcoindadapter.entites.blockchain.Block;
import com.mempoolexplorer.txmempool.bitcoindadapter.entites.mempool.TxPoolChanges;
import com.mempoolexplorer.txmempool.components.MinerNameResolver;
import com.mempoolexplorer.txmempool.components.MisMinedTransactionsChecker;
import com.mempoolexplorer.txmempool.components.Tx10minBuffer;
import com.mempoolexplorer.txmempool.components.TxMemPool;
import com.mempoolexplorer.txmempool.components.alarms.AlarmLogger;
import com.mempoolexplorer.txmempool.components.containers.AlgorithmDiffContainer;
import com.mempoolexplorer.txmempool.components.containers.LiveMiningQueueContainer;
import com.mempoolexplorer.txmempool.components.containers.MempoolEventQueueContainer;
import com.mempoolexplorer.txmempool.components.containers.MinerNamesUnresolvedContainer;
import com.mempoolexplorer.txmempool.components.health.MempoolSyncedHealthIndicator;
import com.mempoolexplorer.txmempool.entites.AlgorithmDiff;
import com.mempoolexplorer.txmempool.entites.CoinBaseData;
import com.mempoolexplorer.txmempool.entites.IgnoringBlock;
import com.mempoolexplorer.txmempool.entites.MisMinedTransactions;
import com.mempoolexplorer.txmempool.entites.blocktemplate.BlockTemplate;
import com.mempoolexplorer.txmempool.entites.miningqueue.CandidateBlock;
import com.mempoolexplorer.txmempool.entites.miningqueue.MiningQueue;
import com.mempoolexplorer.txmempool.events.MempoolEvent;
import com.mempoolexplorer.txmempool.feinginterfaces.BitcoindAdapter;
import com.mempoolexplorer.txmempool.properties.TxMempoolProperties;
import com.mempoolexplorer.txmempool.services.IgnoredEntitiesService;
import com.mempoolexplorer.txmempool.services.StatisticsService;
import com.mempoolexplorer.txmempool.utils.SysProps;

import org.apache.commons.lang3.Validate;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class MempoolEventConsumer implements Runnable {

    // Thread-related flags.
    private boolean threadStarted = false;
    private boolean threadFinished = false;
    private Thread thread = null;
    protected boolean endThread;

    // Other flags
    private int lastBASequence = 0;// Last BitcoindAdapter Sequence. must be 0 not -1 (see method errorInSeqNumber)
    private boolean starting = true;
    private boolean lastSync = true;// This is really needed to avoid delete ignoredTransactions.
    private boolean syncronizedWithUpStream = false;

    @Autowired
    private MempoolEventQueueContainer mempoolEventQueueContainer;
    @Autowired
    private AlarmLogger alarmLogger;
    @Autowired
    private BitcoindAdapter bitcoindAdapter;
    @Autowired
    private TxMemPool txMemPool;
    @Autowired
    private AlgorithmDiffContainer algoDiffContainer;
    @Autowired
    private LiveMiningQueueContainer liveMiningQueueContainer;
    @Autowired
    private TxMempoolProperties txMempoolProperties;
    @Autowired
    private MinerNameResolver minerNameResolver;
    @Autowired
    private MinerNamesUnresolvedContainer minerNamesUnresolvedContainer;
    @Autowired
    private MisMinedTransactionsChecker misMinedTransactionsChecker;
    @Autowired
    private IgnoredEntitiesService ignoredEntitiesService;
    @Autowired
    private StatisticsService statisticsService;
    @Autowired
    private MempoolSyncedHealthIndicator mempoolSyncedHealthIndicator;
    @Autowired
    private Tx10minBuffer tx10minBuffer;

    public void start() {
        if (threadFinished)
            throw new IllegalStateException("This class only accepts only one start");
        thread = new Thread(this);
        thread.start();
        threadStarted = true;
    }

    public void shutdown() {
        if (!threadStarted)
            throw new IllegalStateException("This class is not started yet!");
        endThread = true;
        thread.interrupt();// In case thread is waiting for something.
        threadFinished = true;
    }

    @Override
    public void run() {
        // Until shutdown.
        while (!endThread) {
            try {
                MempoolEvent event = mempoolEventQueueContainer.getBlockingQueue().take();
                log.debug("This is the event: {}", event.toString());
                onEvent(event);
            } catch (InterruptedException e) {
                log.info("Thread interrupted for shutdown.");
                log.debug("", e);
                Thread.currentThread().interrupt();// It doesn't care, just to avoid sonar complaining.
            } catch (Exception e) {
                log.error("", e);
                alarmLogger.addAlarm("Fatal error" + ExceptionUtils.getStackTrace(e));
            }
        }
    }

    private void onEvent(MempoolEvent event) throws InterruptedException {
        // Various checks.
        checkSyncWithUpStream(event);
        checkForAllowRefreshLiveMiningQueue();

        if (starting) {
            // ResetContainers or Queries full mempool with mempoolSequence number.
            onEventonStarting(event);
            starting = false;
        }
        treatEvent(event);
        checkIfCleanIgnoredTx();
    }

    private void checkIfCleanIgnoredTx() {
        if (!lastSync && syncronizedWithUpStream) {
            log.info("Syncronization with upstream achived... Cleaning ignored Txs that are not in mempool...");
            ignoredEntitiesService.cleanIgTxNotInMempool(txMemPool);
            log.info("Clean complete");
        }
    }

    private void checkSyncWithUpStream(MempoolEvent event) throws InterruptedException {
        if (starting) {
            // When starting better stop for 1 second to let mempoolEventQueue to fill. And
            // then ask for its size==0 to ask if can allow refresh in
            // liveMiningQueueContainer.
            Thread.sleep(1000);
            return;
        }

        lastSync = syncronizedWithUpStream;

        syncronizedWithUpStream = (event.getMempoolSize() == (txMemPool.getTxNumber() + event.getMempoolDelta()));
        mempoolSyncedHealthIndicator.setMempoolSynced(syncronizedWithUpStream);

        if (lastSync && !syncronizedWithUpStream && !starting) {
            log.error("Syncronization with upStream lost!");
            alarmLogger.addAlarm("Syncronization with upstream lost!");
        }
    }

    private void checkForAllowRefreshLiveMiningQueue() {
        // if no pending Txs or blocks, then we can allow refresh.
        if (syncronizedWithUpStream && (!liveMiningQueueContainer.isAllowRefresh())) {
            liveMiningQueueContainer.setAllowRefresh(true);
            log.info("BlockTemplateRefresherJob started");
            // Execute ASAP. does not matter if scheduller also invokes it. It's thread
            // safe.
            liveMiningQueueContainer.forceRefresh();
        }
    }

    private void onEventonStarting(MempoolEvent event) throws InterruptedException {
        if (event.getSeqNumber() == 0) {
            // This avoids logging spam when mempoolRecorder sends data to us.
            if (lastBASequence == 0) {
                log.info("BitcoindAdapter is starting while we are already up");
            }
        } else {
            log.info("BitcoindAdapater is already working, asking for full mempool and mempoolSequence...");

            Map<String, Transaction> fullMemPoolMap = null;
            while (fullMemPoolMap == null) {
                try {
                    fullMemPoolMap = bitcoindAdapter.getFullMemPool();
                } catch (Exception e) {
                    log.warn("BitcoindAdapter is not ready yet, waiting 5 seconds...");
                    alarmLogger.addAlarm("BitcoindAdapter is not ready yet, waiting 5 seconds...");
                    Thread.sleep(5000);
                }
            }
            TxPoolChanges txpc = new TxPoolChanges();
            txpc.setNewTxs(new ArrayList<>(fullMemPoolMap.values()));
            txMemPool.refresh(txpc);
            log.info("Full mempool has been queried from bitcoindAdapter.");
        }
        // Fake a lastBASequence because we are starting
        lastBASequence = event.getSeqNumber() - 1;
    }

    private void treatEvent(MempoolEvent event) throws InterruptedException {
        if (errorInSeqNumber(event)) {
            onErrorInSeqNumber(event);// Makes a full reset
            return;
        }
        switch (event.getEventType()) {
        case NEW_BLOCK:
            // Block event has new block and refresh info, order matters
            onNewBlock(event);
            onRefreshEvent(event);
            liveMiningQueueContainer.forceRefresh();
            break;
        case REFRESH_POOL:
            onNewTx(event);
            onRefreshEvent(event);
            liveMiningQueueContainer.refreshIfNeeded();
            break;
        default:
            throw new IllegalArgumentException("WTF! MempoolEventType not valid");
        }
    }

    private void onNewTx(MempoolEvent event) {
        // Register the weight of incoming Tx to know incoming weight / 10 minutes.
        tx10minBuffer.register(event.getTxPoolChanges());
        // Deleted tx can be an ignored one. Delete from db in that case.
        event.getTxPoolChanges().getRemovedTxsId().stream().forEach(ignoredEntitiesService::onDeleteTx);
    }

    private void onNewBlock(MempoolEvent blockEvent) {
        Optional<Block> opBlock = blockEvent.getBlock();
        if (opBlock.isEmpty()) {
            alarmLogger.addAlarm("An empty block has come in a MempoolEvent.EventType.NEW_BLOCK");
            return;
        }
        Block block = opBlock.get();

        if (Boolean.FALSE.equals(block.getConnected())) {
            alarmLogger.addAlarm("A disconnected block has arrived and has been ignored, height: " + block.getHeight()
                    + ", hash: " + block.getHash());
            return;// Ignore it, this disconnected block txs are added to mempool in
                   // onRefreshEvent.
        }

        List<String> minedBlockTxIds = blockEvent.tryGetBlockTxIds().orElseThrow();
        log.info("New block(connected: {}, height: {}, hash: {}, txNum: {}) ---------------------------",
                block.getConnected(), block.getHeight(), block.getHash(), minedBlockTxIds.size());

        MiningQueue miningQueue = buildMiningQueue(block);
        // CandidateBlock can be empty
        CandidateBlock candidateBlock = miningQueue.getCandidateBlock(0).orElse(CandidateBlock.empty());
        boolean isCorrect = checkCandidateBlockIsCorrect(block.getHeight(), candidateBlock);

        // When two blocks arrive without refreshing mempool this is ALWAYS empty
        Optional<BlockTemplate> blockTemplate = blockEvent.getBlockTemplate();
        CoinBaseData coinBaseData = resolveMinerName(block);

        MisMinedTransactions mmtBlockTemplate = new MisMinedTransactions(txMemPool,
                blockTemplate.orElse(BlockTemplate.empty()), block, minedBlockTxIds, coinBaseData);
        MisMinedTransactions mmtCandidateBlock = new MisMinedTransactions(txMemPool, candidateBlock, block,
                minedBlockTxIds, coinBaseData);

        // Disabled for the moment.
        buildAndStoreAlgorithmDifferences(block, candidateBlock, blockTemplate.orElse(BlockTemplate.empty()),
                isCorrect);

        // Check for alarms or inconsistencies
        misMinedTransactionsChecker.check(mmtBlockTemplate);
        misMinedTransactionsChecker.check(mmtCandidateBlock);

        IgnoringBlock igBlockTemplate = new IgnoringBlock(mmtBlockTemplate, txMemPool);
        IgnoringBlock igBlockOurs = new IgnoringBlock(mmtCandidateBlock, txMemPool);

        // Save ignored and repudiated Txs, ignoringBlocks stats only if we are in sync
        if (syncronizedWithUpStream) {
            ignoredEntitiesService.onNewBlockConnected(igBlockTemplate, minedBlockTxIds,
                    mmtBlockTemplate.getIgnoredONRByMinerMapWD().getFeeableMap().values());
            ignoredEntitiesService.onNewBlockConnected(igBlockOurs, minedBlockTxIds,
                    mmtCandidateBlock.getIgnoredONRByMinerMapWD().getFeeableMap().values());
            statisticsService.saveStatisticsToDB(igBlockTemplate, igBlockOurs);
        }

        // If a connectedBlock event arrives with seqNumber==0 then is probably sent
        // from mempoolRecorder.
        // We have to reset because in between blocks, ALL mempool before block sent is
        // also sent by mempoolRecorder.
        // If this block is from bitcoindAdapter, it's the first one so we don't care.
        if (blockEvent.getSeqNumber() == 0) {
            log.info("Full reset because this block was sent by mempoolRecorder.");
            fullReset();
        }
    }

    private void onRefreshEvent(MempoolEvent refreshEvent) {
        TxPoolChanges txpc = refreshEvent.getTxPoolChanges();
        validate(txpc);
        txMemPool.refresh(txpc);
    }

    private boolean errorInSeqNumber(MempoolEvent event) {
        // As we will receive seqNumber=0 for all events from mempoolRecorder we must
        // allow it.
        if (event.getSeqNumber() == 0) {
            return false;
        }
        return ((++lastBASequence) != event.getSeqNumber());
    }

    private void onErrorInSeqNumber(MempoolEvent event) throws InterruptedException {
        // Somehow we have lost mempool events (Kafka guarrantees this not to happen).
        // We have to re-start again.
        log.error("We have lost a bitcoindAdapter MempoolEvent, sequence not expected: {}, "
                + "Reset and waiting for new full mempool and mempoolSequence...", event.getSeqNumber());
        fullReset();
        // Event is reintroduced and it's not lost never. This is a must when
        // bitcoindAdapter re-starts and send a sequenceEvent = 0
        onEvent(event);
    }

    private void fullReset() {
        // Do not allow liveMiningQueue refresh
        liveMiningQueueContainer.setAllowRefresh(false);
        resetContainers();
        // Reset downstream counter to provoke cascade resets.
        starting = true;
        lastBASequence = 0;// Last BitcoindAdapter Sequence
        syncronizedWithUpStream = false;
        mempoolSyncedHealthIndicator.setMempoolSynced(false);
    }

    private void resetContainers() {
        txMemPool.drop();
        algoDiffContainer.drop();
        liveMiningQueueContainer.drop();
        // Don't drop the data of MinerNamesUnresolvedContainer since it's useful.
    }

    private void validate(TxPoolChanges txpc) {
        txpc.getNewTxs().stream().forEach(this::validateTx);
    }

    private void validateTx(Transaction tx) {
        Validate.notNull(tx.getTxId(), "txId can't be null");
        Validate.notNull(tx.getTxInputs(), "txInputs can't be null");
        Validate.notNull(tx.getTxOutputs(), "txOutputs can't be null");
        Validate.notNull(tx.getWeight(), "weight can't be null");
        Validate.notNull(tx.getFees(), "Fees object can't be null");
        Validate.notNull(tx.getFees().getBase(), "Fees.base can't be null");
        Validate.notNull(tx.getFees().getModified(), "Fees.modified can't be null");
        Validate.notNull(tx.getFees().getAncestor(), "Fees.ancestor can't be null");
        Validate.notNull(tx.getFees().getDescendant(), "Fees.descendant can't be null");
        Validate.notNull(tx.getTimeInSecs(), "timeInSecs can't be null");
        Validate.notNull(tx.getTxAncestry(), "txAncestry can't be null");
        Validate.notNull(tx.getTxAncestry().getDescendantCount(), "descendantCount can't be null");
        Validate.notNull(tx.getTxAncestry().getDescendantSize(), "descendantSize can't be null");
        Validate.notNull(tx.getTxAncestry().getAncestorCount(), "ancestorCount can't be null");
        Validate.notNull(tx.getTxAncestry().getAncestorSize(), "ancestorSize can't be null");
        Validate.notNull(tx.getTxAncestry().getDepends(), "depends can't be null");
        Validate.notNull(tx.getBip125Replaceable(), "bip125Replaceable can't be null");
        Validate.notEmpty(tx.getHex(), "Hex can't be empty");

        tx.getTxInputs().forEach(input -> {
            if (input.getCoinbase() == null) {
                Validate.notNull(input.getTxId(), "input.txId can't be null");
                Validate.notNull(input.getVOutIndex(), "input.voutIndex can't be null");
                Validate.notNull(input.getAmount(), "input.amount can't be null");
                // Input address could be null in case of unrecognized input scripts
            }
        });

        tx.getTxOutputs().forEach(output -> {
            // addressIds can be null if script is not recognized.
            Validate.notNull(output.getAmount(), "amount can't be null in a TxOutput");
            Validate.notNull(output.getIndex(), "index can't be null in a TxOutput");
        });
    }

    private MiningQueue buildMiningQueue(Block block) {
        MiningQueue miningQueue = MiningQueue.buildFrom(List.of(block.getCoinBaseTx().getWeight()), txMemPool,
                txMempoolProperties.getLiveMiningQueueMaxTxs(), 1, txMempoolProperties.getMaxTxsToCalculateTxsGraphs());
        if (miningQueue.isHadErrors()) {
            alarmLogger.addAlarm("Mining Queue had errors, in OnNewBlock");
        }
        return miningQueue;
    }

    private boolean checkCandidateBlockIsCorrect(int blockHeight, CandidateBlock candidateBlock) {
        Optional<Boolean> opIsCorrect = candidateBlock.checkIsCorrect();
        if (opIsCorrect.isPresent()) {
            boolean isCorrect = opIsCorrect.get();
            if (!isCorrect) {
                alarmLogger.addAlarm("CandidateBlock is incorrect in block:" + blockHeight);
            }
            return isCorrect;
        } else {
            alarmLogger.addAlarm("We can't determinate if CandidateBlock is incorrect in block:" + blockHeight);
            return false;
        }
    }

    private CoinBaseData resolveMinerName(Block block) {
        CoinBaseData coinBaseData = minerNameResolver.resolveFrom(block.getCoinBaseTx().getvInField());

        if (coinBaseData.getMinerName().compareTo(SysProps.MINER_NAME_UNKNOWN) == 0) {
            minerNamesUnresolvedContainer.addCoinBaseField(coinBaseData.getAscciOfField(), block.getHeight());
        }
        return coinBaseData;
    }

    private void buildAndStoreAlgorithmDifferences(Block block, CandidateBlock candidateBlock,
            BlockTemplate blockTemplate, boolean isCorrect) {
        AlgorithmDiff ad = new AlgorithmDiff(txMemPool, candidateBlock, blockTemplate, block.getHeight(), isCorrect);
        // algoDiffContainer.put(ad);

        Optional<Long> bitcoindTotalBaseFee = ad.getBitcoindData().getTotalBaseFee();
        Optional<Long> oursTotalBaseFee = ad.getOursData().getTotalBaseFee();

        if (bitcoindTotalBaseFee.isPresent() && oursTotalBaseFee.isPresent()
                && bitcoindTotalBaseFee.get().longValue() > oursTotalBaseFee.get().longValue()) {
            alarmLogger.addAlarm("Bitcoind algorithm better than us in block: " + block.getHeight());
        }
    }
}