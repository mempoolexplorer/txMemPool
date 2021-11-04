package com.mempoolexplorer.txmempool.entites;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import com.mempoolexplorer.txmempool.bitcoindadapter.entites.Transaction;
import com.mempoolexplorer.txmempool.components.TxMemPool;
import com.mempoolexplorer.txmempool.entites.blocktemplate.BlockTemplate;
import com.mempoolexplorer.txmempool.entites.miningqueue.CandidateBlock;

public class AlgorithmDiffSets {

	// This three sets are disjoint. inBTNotInCB and inCBNotInBT does NOT contain
	// any elements from notInMemPool
	private Set<String> notInMemPool = new HashSet<>();
	private Set<String> inBTNotInCB = new HashSet<>();
	private Set<String> inCBNotInBT = new HashSet<>();

	public static AlgorithmDiffSets empty() {
		return new AlgorithmDiffSets();
	}

	private AlgorithmDiffSets() {

	}

	public AlgorithmDiffSets(TxMemPool txMemPool, BlockTemplate bt, CandidateBlock candidateBlock) {

		bt.getBlockTemplateTxMap().keySet().forEach(txId -> {
			Optional<Transaction> opTx = txMemPool.getTx(txId);
			if (opTx.isEmpty()) {
				notInMemPool.add(txId);
			} else {
				if (!candidateBlock.containsKey(txId)) {
					inBTNotInCB.add(txId);
				}
			}
		});

		candidateBlock.getEntriesStream().map(entry -> entry.getKey()).forEach(txId -> {
			Optional<Transaction> opTx = txMemPool.getTx(txId);
			if (opTx.isPresent()) {
				if (!bt.getBlockTemplateTxMap().containsKey(txId)) {
					inCBNotInBT.add(txId);
				}
			}
		});
	}

	public Set<String> getNotInMemPool() {
		return notInMemPool;
	}

	public Set<String> getInBTNotInCB() {
		return inBTNotInCB;
	}

	public Set<String> getInCBNotInBT() {
		return inCBNotInBT;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("AlgorithmDifferences [notInMemPool.size=");
		builder.append(notInMemPool.size());
		builder.append(", inBTNotInCB.size=");
		builder.append(inBTNotInCB.size());
		builder.append(", inCBNotInBT.size=");
		builder.append(inCBNotInBT.size());
		builder.append("]");
		return builder.toString();
	}

}
