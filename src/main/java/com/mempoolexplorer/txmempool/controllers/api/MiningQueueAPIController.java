package com.mempoolexplorer.txmempool.controllers.api;

import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import com.mempoolexplorer.txmempool.bitcoindadapter.entites.Transaction;
import com.mempoolexplorer.txmempool.components.containers.LiveMiningQueueContainer;
import com.mempoolexplorer.txmempool.controllers.entities.CandidateBlockHistogram;
import com.mempoolexplorer.txmempool.controllers.entities.CompleteLiveMiningQueueGraphData;
import com.mempoolexplorer.txmempool.controllers.entities.DirectedEdge;
import com.mempoolexplorer.txmempool.controllers.entities.PrunedLiveMiningQueueGraphData;
import com.mempoolexplorer.txmempool.controllers.entities.PrunedSatVByteHistogramElement;
import com.mempoolexplorer.txmempool.controllers.entities.PrunedTx;
import com.mempoolexplorer.txmempool.controllers.entities.SatVByteHistogramElement;
import com.mempoolexplorer.txmempool.controllers.entities.TxDependenciesInfo;
import com.mempoolexplorer.txmempool.controllers.entities.TxIdAndWeight;
import com.mempoolexplorer.txmempool.controllers.entities.TxIgnoredData;
import com.mempoolexplorer.txmempool.controllers.entities.TxNode;
import com.mempoolexplorer.txmempool.controllers.errors.ErrorDetails;
import com.mempoolexplorer.txmempool.controllers.exceptions.ServiceNotReadyYetException;
import com.mempoolexplorer.txmempool.entites.AlgorithmType;
import com.mempoolexplorer.txmempool.entites.IgnoredTransaction;
import com.mempoolexplorer.txmempool.entites.miningqueue.LiveMiningQueue;
import com.mempoolexplorer.txmempool.entites.miningqueue.MiningQueue;
import com.mempoolexplorer.txmempool.entites.miningqueue.TxToBeMined;
import com.mempoolexplorer.txmempool.repositories.reactive.IgTransactionReactiveRepository;

import org.apache.commons.lang3.tuple.Pair;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author dev7ba
 *
 *         This api returns a PrunedLiveMiningQueueGraphData in all its methods.
 *         Depending on the method called, the last modification that the client
 *         has, and wheather the client already have or not that item, the
 *         returned class will have more or less properties filled. The general
 *         rules are: - Return no data if the client is updated and already has
 *         the data is asking for. - Return all -dependant- data if client is
 *         not updated, that is, if client is searching for mining Queue then
 *         only mining Queue is return. but if a block is queried and client is
 *         not updated then miningQueue data and block data is returned. Also if
 *         client is querying an histogram and it is not updated then mining
 *         queue and block data containing that histogram is returned.
 */
@CrossOrigin
@RestController
@RequestMapping("/miningQueueAPI")
public class MiningQueueAPIController {

	@Autowired
	private LiveMiningQueueContainer liveMiningQueueContainer;

	@Autowired
	private IgTransactionReactiveRepository igTxReactiveRepository;

	@GetMapping("/miningQueue")
	public PrunedLiveMiningQueueGraphData getMiningQueue() throws ServiceNotReadyYetException {

		CompleteLiveMiningQueueGraphData complete = obtainLiveMiningQueue().getLiveMiningQueueGraphData();
		PrunedLiveMiningQueueGraphData pruned = new PrunedLiveMiningQueueGraphData();
		addCommonData(complete, pruned);
		addMiningQueueDataToPruned(pruned, complete);
		return pruned;
	}

	@GetMapping("/block/{blockIndex}")
	public PrunedLiveMiningQueueGraphData getBlockIndex(@PathVariable("blockIndex") Integer blockIndex)
			throws ServiceNotReadyYetException {

		CompleteLiveMiningQueueGraphData complete = obtainLiveMiningQueue().getLiveMiningQueueGraphData();
		PrunedLiveMiningQueueGraphData pruned = new PrunedLiveMiningQueueGraphData();
		addCommonData(complete, pruned);
		addMiningQueueDataToPruned(pruned, complete);
		addBlockDataToPruned(blockIndex, complete, pruned);
		return pruned;
	}

	@GetMapping("/histogram/{blockIndex}/{satVByte}")
	public PrunedLiveMiningQueueGraphData getHistogram(@PathVariable("blockIndex") Integer blockIndex,
			@PathVariable("satVByte") Integer satVByte) throws ServiceNotReadyYetException {

		CompleteLiveMiningQueueGraphData complete = obtainLiveMiningQueue().getLiveMiningQueueGraphData();
		PrunedLiveMiningQueueGraphData pruned = new PrunedLiveMiningQueueGraphData();
		addCommonData(complete, pruned);
		addMiningQueueDataToPruned(pruned, complete);
		addBlockDataToPruned(blockIndex, complete, pruned);
		addHistogramDataToPruned(blockIndex, satVByte, complete, pruned);
		return pruned;
	}

	@GetMapping("/txIndex/{blockIndex}/{satVByte}/{txIndex}")
	public PrunedLiveMiningQueueGraphData getTxByIndex(@PathVariable("blockIndex") Integer blockIndex,
			@PathVariable("satVByte") Integer satVByte, @PathVariable("txIndex") int txIndex)
			throws ServiceNotReadyYetException {

		LiveMiningQueue liveMiningQueue = obtainLiveMiningQueue();

		CompleteLiveMiningQueueGraphData complete = liveMiningQueue.getLiveMiningQueueGraphData();
		return getTxByIndex(blockIndex, satVByte, txIndex, liveMiningQueue, complete);

	}

	// Returns first tx found in first longest dependency graph
	@GetMapping("/txIFLDG")
	public PrunedLiveMiningQueueGraphData getTxIFLDG() throws ServiceNotReadyYetException {
		Transaction txIFLDG = obtainLiveMiningQueue().getMiningQueue().getTxIFLDG();
		if (txIFLDG == null)
			throw new ServiceNotReadyYetException("txIFLDG is null. No txs on MiningQueue");
		return getTxById(txIFLDG.getTxId());
	}

	@GetMapping("/tx/{txId}")
	public PrunedLiveMiningQueueGraphData getTxById(@PathVariable("txId") String txId)
			throws ServiceNotReadyYetException {

		LiveMiningQueue liveMiningQueue = obtainLiveMiningQueue();
		MiningQueue miningQueue = liveMiningQueue.getMiningQueue();

		Optional<TxToBeMined> opTxToBeMined = miningQueue.getTxToBeMined(txId);

		if (opTxToBeMined.isEmpty())
			return getMiningQueue();

		TxToBeMined txToBeMined = opTxToBeMined.get();// Safe
		int blockIndex = txToBeMined.getContainingBlock().getIndex();
		int satVByte = (int) txToBeMined.getModifiedSatVByte();

		CompleteLiveMiningQueueGraphData complete = liveMiningQueue.getLiveMiningQueueGraphData();

		int txIndex = complete.getCandidateBlockHistogramList().get(blockIndex).getHistogramMap().get(satVByte)
				.getTxIdToListIndex().get(txId);

		return getTxByIndex(blockIndex, satVByte, txIndex, liveMiningQueue, complete);

	}

	private LiveMiningQueue obtainLiveMiningQueue() throws ServiceNotReadyYetException {
		LiveMiningQueue liveMiningQueue = liveMiningQueueContainer.atomicGet();

		if (liveMiningQueue == null)
			throw new ServiceNotReadyYetException();

		return liveMiningQueue;
	}

	private PrunedLiveMiningQueueGraphData getTxByIndex(Integer blockIndex, Integer satVByte, int txIndex,
			LiveMiningQueue liveMiningQueue, CompleteLiveMiningQueueGraphData complete) {
		PrunedLiveMiningQueueGraphData pruned = new PrunedLiveMiningQueueGraphData();
		addCommonData(complete, pruned);
		addMiningQueueDataToPruned(pruned, complete);
		addBlockDataToPruned(blockIndex, complete, pruned);
		addHistogramDataToPruned(blockIndex, satVByte, complete, pruned);
		addTxByIndexToPruned(blockIndex, satVByte, txIndex, liveMiningQueue, pruned);
		return pruned;
	}

	private void addCommonData(CompleteLiveMiningQueueGraphData complete, PrunedLiveMiningQueueGraphData pruned) {
		pruned.setLastModTime(complete.getLastModTime());
		pruned.setWeightInLast10minutes(complete.getWeightInLast10minutes());
		pruned.setFblTxSatVByte(1);
		if (!complete.getCandidateBlockHistogramList().isEmpty()) {
			CandidateBlockHistogram fbh = complete.getCandidateBlockHistogramList().get(0);// First Block Histogram
			if (!fbh.getHistogramList().isEmpty()) {
				SatVByteHistogramElement lastHistogram = fbh.getHistogramList().get(fbh.getHistogramList().size() - 1);
				pruned.setFblTxSatVByte(lastHistogram.getModSatVByte());
			}
		}
	}

	private void addMiningQueueDataToPruned(PrunedLiveMiningQueueGraphData pruned,
			CompleteLiveMiningQueueGraphData complete) {
		pruned.setCandidateBlockRecapList(complete.getCandidateBlockRecapList());
	}

	private void addBlockDataToPruned(Integer blockIndex, CompleteLiveMiningQueueGraphData complete,
			PrunedLiveMiningQueueGraphData pruned) {
		if (blockIndex >= 0 && complete.getCandidateBlockHistogramList().size() > blockIndex) {
			pruned.setPrunedCandidateBlockHistogram(
					from(complete.getCandidateBlockHistogramList().get(blockIndex).getHistogramList()));
			pruned.setBlockSelected(blockIndex);
		}
	}

	private void addHistogramDataToPruned(Integer blockIndex, Integer satVByte,
			CompleteLiveMiningQueueGraphData complete, PrunedLiveMiningQueueGraphData pruned) {
		if (blockIndex >= 0 && complete.getCandidateBlockHistogramList().size() > blockIndex) {
			CandidateBlockHistogram candidateBlockHistogram = complete.getCandidateBlockHistogramList().get(blockIndex);
			SatVByteHistogramElement satVByteHistogramElement = candidateBlockHistogram.getHistogramMap().get(satVByte);
			if (satVByteHistogramElement != null) {
				pruned.setSatVByteSelected(satVByte);
				pruned.setPrunedTxs(prunedFrom(satVByteHistogramElement.getTxIdAndWeightList()));
			}
		}
	}

	private void addTxByIndexToPruned(Integer blockIndex, Integer satVByte, int txIndex,
			LiveMiningQueue liveMiningQueue, PrunedLiveMiningQueueGraphData pruned) {
		CompleteLiveMiningQueueGraphData complete = liveMiningQueue.getLiveMiningQueueGraphData();
		if (blockIndex >= 0 && complete.getCandidateBlockHistogramList().size() > blockIndex) {
			CandidateBlockHistogram candidateBlockHistogram = complete.getCandidateBlockHistogramList().get(blockIndex);
			SatVByteHistogramElement satVByteHistogramElement = candidateBlockHistogram.getHistogramMap().get(satVByte);
			if (satVByteHistogramElement != null) {
				List<TxIdAndWeight> txIdAndWeightList = satVByteHistogramElement.getTxIdAndWeightList();
				if (txIdAndWeightList.size() > txIndex) {
					String txId = txIdAndWeightList.get(txIndex).getTxId();
					pruned.setTxIdSelected(txId);
					pruned.setTxIndexSelected(txIndex);
					pruned.setTxDependenciesInfo(buildDependenciesInfo(txId, liveMiningQueue.getMiningQueue()));
					pruned.setTxIgnoredData(buildTxIgnoredData(txId));
					pruned.setTx(buildTx(txId, liveMiningQueue.getMiningQueue()));
				}
			}
		}
	}

	private Transaction buildTx(String txId, MiningQueue miningQueue) {
		Optional<TxToBeMined> txToBeMined = miningQueue.getTxToBeMined(txId);
		if (txToBeMined.isEmpty())
			return null;
		return txToBeMined.get().getTx();
	}

	private TxIgnoredData buildTxIgnoredData(String txId) {
		IgnoredTransaction igTx = igTxReactiveRepository
				.findById(IgnoredTransaction.buildDBKey(txId, AlgorithmType.OURS)).block();
		if (igTx == null) {
			return new TxIgnoredData();
		}
		return TxIgnoredData.from(igTx);
	}

	private TxDependenciesInfo buildDependenciesInfo(String initTxId, MiningQueue miningQueue) {

		TxDependenciesInfo info = new TxDependenciesInfo();

		// Helping HashMap: txid->Pair<TxToBeMined,txIndex in info.getNodes()>
		Map<String, Pair<TxToBeMined, Integer>> txIdToPairMap = new HashMap<>();

		addNodes(info.getNodes(), miningQueue, txIdToPairMap, initTxId);
		addEdges(info.getEdges(), txIdToPairMap);

		return info;
	}

	private void addNodes(List<TxNode> nodeList, MiningQueue miningQueue,
			Map<String, Pair<TxToBeMined, Integer>> txIdToPairMap, String initTxId) {

		Deque<String> txIdStack = new LinkedList<>();// Stack containing txIds to visit

		// Adds initial tx to the stack and map.
		txIdStack.push(initTxId);

		int txIndexCount = 0;
		while (!txIdStack.isEmpty()) {
			String txId = txIdStack.pop();
			if (!txIdToPairMap.containsKey(txId)) {
				Optional<TxToBeMined> optTxToBeMined = miningQueue.getTxToBeMined(txId);

				if (optTxToBeMined.isEmpty())
					continue;

				TxToBeMined txToBeMined = optTxToBeMined.get();// Safe
				Transaction tx = txToBeMined.getTx();

				TxNode txNode = buildFrom(txToBeMined);

				txIdToPairMap.put(txId, Pair.of(txToBeMined, txIndexCount));
				txIndexCount++;
				nodeList.add(txNode);

				List<String> depends = tx.getTxAncestry().getDepends();
				List<String> spentBy = tx.getTxAncestry().getSpentby();
				depends.stream().filter(parent -> !txIdToPairMap.containsKey(parent)).forEach(txIdStack::add);
				spentBy.stream().filter(child -> !txIdToPairMap.containsKey(child)).forEach(txIdStack::add);
			}
		}
	}

	private void addEdges(List<DirectedEdge> directedEdgeList, Map<String, Pair<TxToBeMined, Integer>> txIdToPairMap) {

		txIdToPairMap.values().forEach(pair -> {
			TxToBeMined child = pair.getLeft();
			int childIndex = pair.getRight();

			child.getTx().getTxAncestry().getDepends().forEach(parentId -> {
				int parentIndex = txIdToPairMap.get(parentId).getRight();
				DirectedEdge de = new DirectedEdge(childIndex, parentIndex);
				directedEdgeList.add(de);
			});

		});
	}

	private TxNode buildFrom(TxToBeMined txToBeMined) {
		Transaction tx = txToBeMined.getTx();
		TxNode txNode = new TxNode();
		txNode.setBaseFee(tx.getBaseFees());
		txNode.setContainingBlockIndex(txToBeMined.getContainingBlock().getIndex());
		txNode.setModifiedSatVByte(txToBeMined.getModifiedSatVByte());
		txNode.setBip125Replaceable(tx.getBip125Replaceable());
		txNode.setTimeInMillis(tx.getTimeInSecs() * 1000);
		txNode.setTxId(tx.getTxId());
		txNode.setWeight(tx.getWeight());
		return txNode;
	}

	private List<PrunedTx> prunedFrom(List<TxIdAndWeight> list) {
		return list.stream().map(e -> new PrunedTx(e.getWeight())).collect(Collectors.toList());
	}

	private List<PrunedSatVByteHistogramElement> from(List<SatVByteHistogramElement> list) {
		return list.stream()
				.map(e -> new PrunedSatVByteHistogramElement(e.getModSatVByte(), e.getNumTxs(), e.getWeight()))
				.collect(Collectors.toList());
	}

	@ExceptionHandler(ServiceNotReadyYetException.class)
	public ResponseEntity<ErrorDetails> onServiceNotReadyYet(ServiceNotReadyYetException e) {
		ErrorDetails errorDetails = new ErrorDetails();
		errorDetails.setErrorMessage(e.getMessage());
		errorDetails.setErrorCode(HttpStatus.SERVICE_UNAVAILABLE.toString());
		return new ResponseEntity<>(errorDetails, HttpStatus.SERVICE_UNAVAILABLE);
	}

}
