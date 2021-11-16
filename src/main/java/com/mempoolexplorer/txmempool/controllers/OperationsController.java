package com.mempoolexplorer.txmempool.controllers;

import java.util.List;
import java.util.Optional;

import com.mempoolexplorer.txmempool.bitcoindadapter.entites.blockchain.Block;
import com.mempoolexplorer.txmempool.bitcoindadapter.entites.mempool.TxPoolChanges;
import com.mempoolexplorer.txmempool.components.MinerNameResolver;
import com.mempoolexplorer.txmempool.components.MisMinedTransactionsChecker;
import com.mempoolexplorer.txmempool.components.TxMemPool;
import com.mempoolexplorer.txmempool.components.containers.IgTxCacheContainer;
import com.mempoolexplorer.txmempool.components.containers.MinerNamesUnresolvedContainer;
import com.mempoolexplorer.txmempool.controllers.entities.RecalculateAllStatsResult;
import com.mempoolexplorer.txmempool.entites.CoinBaseData;
import com.mempoolexplorer.txmempool.entites.IgnoringBlock;
import com.mempoolexplorer.txmempool.entites.MisMinedTransactions;
import com.mempoolexplorer.txmempool.entites.blocktemplate.BlockTemplate;
import com.mempoolexplorer.txmempool.entites.miningqueue.CandidateBlock;
import com.mempoolexplorer.txmempool.entites.miningqueue.MiningQueue;
import com.mempoolexplorer.txmempool.feinginterfaces.MempoolRecorder;
import com.mempoolexplorer.txmempool.feinginterfaces.entities.FullStateOnNewBlock;
import com.mempoolexplorer.txmempool.properties.TxMempoolProperties;
import com.mempoolexplorer.txmempool.services.IgnoredEntitiesService;
import com.mempoolexplorer.txmempool.services.StatisticsService;
import com.mempoolexplorer.txmempool.utils.SysProps;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/operations")
@Slf4j
public class OperationsController {

	@Autowired
	private StatisticsService statisticsService;
	@Autowired
	private MempoolRecorder mempoolRecorder;// feing interface for this.
	@Autowired
	private TxMemPool txMemPool;
	@Autowired
	private TxMempoolProperties properties;
	@Autowired
	private MinerNameResolver minerNameResolver;
	@Autowired
	private MisMinedTransactionsChecker misMinedTransactionsChecker;
	@Autowired
	private MinerNamesUnresolvedContainer minerNamesUnresolvedContainer;
	@Autowired
	private IgnoredEntitiesService ignoredEntitiesService;
	@Autowired
	private IgTxCacheContainer igTxCacheContainer;

	private final String ERR_NO_DETACHED = "txMempool is not in detached state. Change txMempool properties.";

	@GetMapping("/hasBlock/{height}")
	public Boolean hasBlock(@PathVariable("height") int height) {
		return mempoolRecorder.getHasBlock(height);
	}

	@GetMapping("/cleanIgTxNotInMempool")
	public void cleanIgTxNotInMempool() {
		ignoredEntitiesService.cleanIgTxNotInMempool(txMemPool);
		igTxCacheContainer.calculate();
	}

	@GetMapping("/recalculateAllStats")
	public RecalculateAllStatsResult recalculateAllStats() {
		if (properties.isDetached()) {
			return statisticsService.recalculateAllStats();
		}
		RecalculateAllStatsResult res = new RecalculateAllStatsResult();
		res.getExecutionInfoList().add(ERR_NO_DETACHED);
		return res;
	}

	@GetMapping("/CalculateBlockFromRecorder/{height}")
	public String calculateBlockFromRecorder(@PathVariable("height") int height) {
		if (properties.isDetached()) {

			FullStateOnNewBlock fsonb = mempoolRecorder.getFullStateOnNewBlock(height);
			TxPoolChanges txpc = new TxPoolChanges();
			txpc.setNewTxs(fsonb.getMemPool());
			txMemPool.refresh(txpc);

			MiningQueue miningQueue = buildMiningQueue(fsonb.getBlock());
			CandidateBlock candidateBlock = miningQueue.getCandidateBlock(0).orElse(CandidateBlock.empty());
			boolean isCorrect = checkCandidateBlockIsCorrect(fsonb.getBlock().getHeight(), candidateBlock);

			if (!isCorrect) {
				log.error("Candidate block for height {} is not correct", height);
			}

			// When two blocks arrive without refreshing mempool this is ALWAYS empty
			Optional<BlockTemplate> blockTemplate = Optional.ofNullable(fsonb.getBlockTemplate());
			CoinBaseData coinBaseData = resolveMinerName(fsonb.getBlock());

			MisMinedTransactions mmtBlockTemplate = new MisMinedTransactions(txMemPool,
					blockTemplate.orElse(BlockTemplate.empty()), fsonb.getBlock(), fsonb.getBlockTxIds(), coinBaseData);
			MisMinedTransactions mmtCandidateBlock = new MisMinedTransactions(txMemPool, candidateBlock,
					fsonb.getBlock(), fsonb.getBlockTxIds(), coinBaseData);

			// Check for alarms or inconsistencies
			misMinedTransactionsChecker.check(mmtBlockTemplate);
			misMinedTransactionsChecker.check(mmtCandidateBlock);

			IgnoringBlock igBlockTemplate = new IgnoringBlock(mmtBlockTemplate, txMemPool);
			IgnoringBlock igBlockOurs = new IgnoringBlock(mmtCandidateBlock, txMemPool);
			ignoredEntitiesService.onRecalculateBlockFromRecorder(igBlockTemplate);
			ignoredEntitiesService.onRecalculateBlockFromRecorder(igBlockOurs);
			statisticsService.saveStatisticsToDB(igBlockTemplate, igBlockOurs);
			txMemPool.drop();
			return "Block " + height + "calculated from mempoolRecorder.";
		}
		return ERR_NO_DETACHED;
	}

	private MiningQueue buildMiningQueue(Block block) {
		MiningQueue miningQueue = MiningQueue.buildFrom(List.of(block.getCoinBaseTx().getWeight()), txMemPool,
				properties.getLiveMiningQueueMaxTxs(), 1, properties.getMaxTxsToCalculateTxsGraphs());
		if (miningQueue.isHadErrors()) {
			log.error("Mining Queue had errors when trying to build it");
		}
		return miningQueue;
	}

	private boolean checkCandidateBlockIsCorrect(int blockHeight, CandidateBlock candidateBlock) {
		Optional<Boolean> opIsCorrect = candidateBlock.checkIsCorrect();
		if (opIsCorrect.isPresent()) {
			boolean isCorrect = opIsCorrect.get();
			if (!isCorrect) {
				log.error("CandidateBlock is incorrect in block:" + blockHeight);
			}
			return isCorrect;
		} else {
			log.error("We can't determinate if CandidateBlock is incorrect in block:" + blockHeight);
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

}
