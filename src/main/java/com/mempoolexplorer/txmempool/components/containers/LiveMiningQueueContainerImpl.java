package com.mempoolexplorer.txmempool.components.containers;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.IntStream;

import com.mempoolexplorer.txmempool.components.Tx10minBuffer;
import com.mempoolexplorer.txmempool.components.TxMemPool;
import com.mempoolexplorer.txmempool.components.alarms.AlarmLogger;
import com.mempoolexplorer.txmempool.controllers.entities.CandidateBlockHistogram;
import com.mempoolexplorer.txmempool.controllers.entities.CandidateBlockRecap;
import com.mempoolexplorer.txmempool.controllers.entities.CompleteLiveMiningQueueGraphData;
import com.mempoolexplorer.txmempool.controllers.entities.SatVByteHistogramElement;
import com.mempoolexplorer.txmempool.controllers.entities.TxIdAndWeight;
import com.mempoolexplorer.txmempool.entites.miningqueue.CandidateBlock;
import com.mempoolexplorer.txmempool.entites.miningqueue.LiveMiningQueue;
import com.mempoolexplorer.txmempool.entites.miningqueue.MiningQueue;
import com.mempoolexplorer.txmempool.properties.TxMempoolProperties;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class LiveMiningQueueContainerImpl implements LiveMiningQueueContainer {

	@Autowired
	private TxMempoolProperties txMempoolProperties;

	@Autowired
	private TxMemPool txMemPool;

	@Autowired
	private Tx10minBuffer tx10minBuffer;

	@Autowired
	private AlarmLogger alarmLogger;

	private AtomicReference<LiveMiningQueue> liveMiningQueueRef = new AtomicReference<>(LiveMiningQueue.empty());

	@Getter(onMethod = @__(@Override))
	private boolean allowRefresh = false;

	private AtomicBoolean refresh = new AtomicBoolean(false);

	@Scheduled(fixedDelayString = "${txmempool.liveMiningQueueRefreshEachMillis}")
	public void tryRefresh() {
		if (allowRefresh) {
			refresh.set(true);
		}
	}

	@Override
	public void setAllowRefresh(boolean allowRefresh){
		this.allowRefresh=allowRefresh;
		this.refresh.set(allowRefresh);
	}

	@Override
	public LiveMiningQueue atomicGet() {
		return liveMiningQueueRef.get();
	}

	//Called when new Tx
	@Override
	public void refreshIfNeeded() {
		if (refresh.getAndSet(false)) {
			updateLiveMiningQueue();
		}
	}

	//Called when new Block
	@Override
	public void forceRefresh() {
		if (allowRefresh) {
			updateLiveMiningQueue();
		}
	}

	@Override
	public void drop() {
		liveMiningQueueRef.set(LiveMiningQueue.empty());
	}

	// Create LiveMiningQueue. All Blocks are taken from MiningQueue which are
	// accurate (CPFP and block space left by big txs are taken into account). And
	// remaining blocks not in mining queue (not calculated) are not shown.
	private MiningQueue updateLiveMiningQueue() {
		log.debug("Updating live mining queue.");
		MiningQueue newMiningQueue = MiningQueue.buildFrom(new ArrayList<>(), txMemPool,
				txMempoolProperties.getMiningQueueNumTxs(), txMempoolProperties.getMiningQueueMaxNumBlocks());
		log.debug("Live mining queue updated.");

		if (newMiningQueue.isHadErrors()) {
			alarmLogger.addAlarm("Mining Queue had errors, in updateLiveMiningQueue");
			log.error("Mining Queue had errors, in updateLiveMiningQueue");
		}
		this.liveMiningQueueRef
				.set(new LiveMiningQueue(buildLiveMiningQueueGraphDataFrom(newMiningQueue), newMiningQueue));
		return newMiningQueue;
	}

	private CompleteLiveMiningQueueGraphData buildLiveMiningQueueGraphDataFrom(MiningQueue mq) {
		CompleteLiveMiningQueueGraphData lmq = new CompleteLiveMiningQueueGraphData();
		lmq.setLastModTime(Instant.now().toEpochMilli());
		lmq.setNumTxsInMempool(txMemPool.getTxNumber());
		lmq.setNumTxsInMiningQueue(mq.getNumTxs());
		lmq.setCandidateBlockRecapList(createCandidateBlockRecapList(mq));
		lmq.setCandidateBlockHistogramList(createCandidateBlockHistogramList(mq));
		lmq.setWeightInLast10minutes(tx10minBuffer.getTotalWeight());
		return lmq;
	}

	private List<CandidateBlockRecap> createCandidateBlockRecapList(MiningQueue mq) {
		List<CandidateBlockRecap> cbrList = new ArrayList<>();
		IntStream.range(0, mq.getNumCandidateBlocks()).mapToObj(mq::getCandidateBlock).map(Optional::get)
				.forEach(cb -> {
					CandidateBlockRecap cbr = new CandidateBlockRecap(cb.getWeight(), cb.getTotalFees(),
							cb.getNumTxs());
					cbrList.add(cbr);
				});
		return cbrList;
	}

	private List<CandidateBlockHistogram> createCandidateBlockHistogramList(MiningQueue mq) {
		List<CandidateBlockHistogram> cbhList = new ArrayList<>();
		IntStream.range(0, mq.getNumCandidateBlocks()).mapToObj(mq::getCandidateBlock).map(Optional::get)
				.forEach(cb -> {
					CandidateBlockHistogram cbh = createHistogramFor(cb);
					cbhList.add(cbh);
				});
		return cbhList;
	}

	private CandidateBlockHistogram createHistogramFor(CandidateBlock cb) {
		CandidateBlockHistogram cbh = new CandidateBlockHistogram();

		cb.getOrderedStream().forEach(txtbm -> {
			int weight = txtbm.getTx().getWeight();
			addTx(txtbm.getTxId(), txtbm.getModifiedSatVByte(), weight, cbh);
		});

		return cbh;
	}

	private void addTx(String txId, double modSatVByte, int weight, CandidateBlockHistogram cbh) {

		SatVByteHistogramElement histogram = cbh.getHistogramMap().get((int) modSatVByte);
		if (histogram == null) {
			addNewPair(txId, modSatVByte, weight, cbh);
		} else {
			// Order matters
			histogram.getTxIdAndWeightList().add(new TxIdAndWeight(txId, weight));
			histogram.getTxIdToListIndex().put(txId, histogram.getNumTxs());
			histogram.setNumTxs(histogram.getNumTxs() + 1);
			histogram.setWeight(histogram.getWeight() + weight);
		}
	}

	private void addNewPair(String txId, double modSatVByte, int weight, CandidateBlockHistogram cbh) {
		List<TxIdAndWeight> txIdAndWeightList = new ArrayList<>();
		txIdAndWeightList.add(new TxIdAndWeight(txId, weight));
		Map<String, Integer> txIdToListIndex = new HashMap<>();
		txIdToListIndex.put(txId, 0);
		SatVByteHistogramElement sVByteHElement = new SatVByteHistogramElement((int) modSatVByte, 1, weight,
				txIdAndWeightList, txIdToListIndex);
		cbh.getHistogramMap().put((int) modSatVByte, sVByteHElement);
		cbh.getHistogramList().add(sVByteHElement);
	}

}
