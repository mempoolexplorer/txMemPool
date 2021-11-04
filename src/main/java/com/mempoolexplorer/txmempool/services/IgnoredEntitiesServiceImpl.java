package com.mempoolexplorer.txmempool.services;

import java.util.Collection;
import java.util.List;

import com.mempoolexplorer.txmempool.components.TxMemPool;
import com.mempoolexplorer.txmempool.components.alarms.AlarmLogger;
import com.mempoolexplorer.txmempool.controllers.entities.PrunedIgnoringBlock;
import com.mempoolexplorer.txmempool.entites.AlgorithmType;
import com.mempoolexplorer.txmempool.entites.IgnoredTransaction;
import com.mempoolexplorer.txmempool.entites.IgnoredTxState;
import com.mempoolexplorer.txmempool.entites.IgnoringBlock;
import com.mempoolexplorer.txmempool.entites.NotMinedTransaction;
import com.mempoolexplorer.txmempool.entites.RepudiatedTransaction;
import com.mempoolexplorer.txmempool.properties.TxMempoolProperties;
import com.mempoolexplorer.txmempool.repositories.reactive.IgBlockReactiveRepository;
import com.mempoolexplorer.txmempool.repositories.reactive.IgTransactionReactiveRepository;
import com.mempoolexplorer.txmempool.repositories.reactive.RepudiatedTxReactiveRepository;

import org.apache.commons.lang.NotImplementedException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;

/**
 * The use of reactive repositories is purely testimonial. This is NOT the way
 * of using reactive streams. Must refactor all of this.
 */
@Service
@Slf4j
public class IgnoredEntitiesServiceImpl implements IgnoredEntitiesService {

    @Autowired
    private IgTransactionReactiveRepository igTxReactiveRepository;
    @Autowired
    private RepudiatedTxReactiveRepository repudiatedTxReactiveRepository;
    @Autowired
    private IgBlockReactiveRepository igBlockReactiveRepository;
    @Autowired
    private TxMempoolProperties txMempoolProperties;
    @Autowired
    private AlarmLogger alarmLogger;

    @Override
    public void onDeleteTx(String txId) {
        // Deletes txId from IgnoredTransactions db.
        // Changing tx.state & tx.finallyMinedOnBlock and save to historic is not
        // implemented yet.
        igTxReactiveRepository.deleteById(IgnoredTransaction.buildDBKey(txId, AlgorithmType.BITCOIND)).block();
        igTxReactiveRepository.deleteById(IgnoredTransaction.buildDBKey(txId, AlgorithmType.OURS)).block();
    }

    @Override
    public void onRecalculateBlockFromRecorder(IgnoringBlock igBlock) {
        igBlockReactiveRepository.save(igBlock).block();
    }

    @Override
    public void onNewBlockConnected(IgnoringBlock igBlock, List<String> minedBlockTxIds,
            Collection<NotMinedTransaction> ignoredTxs) {

        igBlockReactiveRepository.save(igBlock).block();

        if (log.isDebugEnabled()) {
            log.debug(igBlock.toString());
        }

        log.info("minedBlockTXIds.size(): {}", minedBlockTxIds.size());

        for (String igTxId : minedBlockTxIds) {
            igTxReactiveRepository.deleteById(IgnoredTransaction.buildDBKey(igTxId, igBlock.getAlgorithmUsed()))
                    .block();
        }

        for (NotMinedTransaction nmTx : ignoredTxs) {
            IgnoredTransaction igTx = igTxReactiveRepository
            .findById(IgnoredTransaction.buildDBKey(nmTx.getTxId(), igBlock.getAlgorithmUsed())).block();
            if (igTx == null) {
                igTx = new IgnoredTransaction();
                igTx.setTxId(nmTx.getTxId());
                igTx.setAType(igBlock.getAlgorithmUsed());
                igTx.setDbKey(igTx.buildDBKey());
                igTx.setState(IgnoredTxState.INMEMPOOL);
            }
            // This allows this method to be executed multiple times (from different
            // instances) and give the same result as beeing executed only once.
            if (isAlreadyModifiedForThisBlock(igTx, igBlock))
                continue;

            PrunedIgnoringBlock pigBlock = new PrunedIgnoringBlock();

            pigBlock.setCoinBaseData(igBlock.getMinedBlockData().getCoinBaseData());
            pigBlock.setHash(igBlock.getMinedBlockData().getHash());
            pigBlock.setHeight(igBlock.getMinedBlockData().getHeight());
            pigBlock.setPosInCandidateBlock(nmTx.getOrdinalPositionInBlock().orElse(-1));
            pigBlock.setTime(igBlock.getMinedBlockData().getChangeTime().toEpochMilli());
            pigBlock.setTxsInCandidateBlock(igBlock.getCandidateBlockData().getNumTxs());
            pigBlock.setTxsInMinedBlock(igBlock.getMinedBlockData().getFeeableData().getNumTxs().orElse(-1));

            igTx.getIgnoringBlocks().add(pigBlock);

            if (igTx.getIgnoringBlocks().size() == 1) {
                igTx.setTimeWhenShouldHaveBeenMined(igBlock.getMinedBlockData().getChangeTime());
            }

            double txSatVByte = nmTx.getTx().getSatvByte();
            long txFees = nmTx.getTx().getFees().getBase();

            igTx.setTotalSatvBytesLost(calculateTotalSatvBytesLost(igBlock, igTx, txSatVByte));
            igTx.setTotalFeesLost(calculateTotalFeesLost(igTx, txFees));

            igTxReactiveRepository.save(igTx).block();

            // Consider repudiated if needed
            if ((igTx.getIgnoringBlocks().size() >= txMempoolProperties.getNumTimesTxIgnoredToRaiseAlarm())
                    && igTx.getTotalSatvBytesLost() >= txMempoolProperties.getTotalSatVBLostToRaiseAlarm()) {
                repudiatedTxReactiveRepository.save(new RepudiatedTransaction(igTx)).block();
                alarmLogger.addAlarm(igBlock.getAlgorithmUsed().toString() + "-Repudiated transaction txId:" + igTx
                        + ". Has been ignored " + igTx.getIgnoringBlocks().size() + " times.");
            }
        }
    }

    @Override
    public void onNewBlockDisconnected(IgnoringBlock igBlock, List<String> blockTxIds,
            Collection<NotMinedTransaction> ignoredTxs) {
        throw new NotImplementedException();
    }

    @Override
    public void cleanIgTxNotInMempool(TxMemPool txMemPool) {
        igTxReactiveRepository
                .deleteAll(igTxReactiveRepository.findAll().filter(igTx -> !txMemPool.containsTxId(igTx.getTxId())))
                .block();
    }

    private boolean isAlreadyModifiedForThisBlock(IgnoredTransaction igTx, IgnoringBlock igBlock) {
        int igBlocksNum = igTx.getIgnoringBlocks().size();
        if (igBlocksNum == 0)
            return false;
        PrunedIgnoringBlock lastIgBlock = igTx.getIgnoringBlocks().get(igBlocksNum - 1);
        return lastIgBlock.getHash().compareTo(igBlock.getMinedBlockData().getHash()) == 0;
    }

    private double calculateTotalSatvBytesLost(IgnoringBlock ignoringBlock, IgnoredTransaction igTx,
            double txSatVByte) {
        double totalSatvBytesLost = igTx.getTotalSatvBytesLost();
        double blockSatvBytesLost = ignoringBlock.getMinedBlockData().getFeeableData().getMinSatVByte().orElse(0D);
        double diff = txSatVByte - blockSatvBytesLost;
        return totalSatvBytesLost + diff;
    }

    private long calculateTotalFeesLost(IgnoredTransaction igTx, long txFees) {
        return (long) (igTx.getTotalSatvBytesLost() * txFees);
    }

}
