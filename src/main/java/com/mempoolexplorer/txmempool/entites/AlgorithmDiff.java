package com.mempoolexplorer.txmempool.entites;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

import com.mempoolexplorer.txmempool.bitcoindadapter.entites.Transaction;
import com.mempoolexplorer.txmempool.bitcoindadapter.entites.blocktemplate.BlockTemplateTx;
import com.mempoolexplorer.txmempool.components.TxMemPool;
import com.mempoolexplorer.txmempool.entites.blocktemplate.BlockTemplate;
import com.mempoolexplorer.txmempool.entites.miningqueue.CandidateBlock;
import com.mempoolexplorer.txmempool.entites.miningqueue.TxToBeMined;

import lombok.Getter;

@Getter
public class AlgorithmDiff {

	private int blockHeight;
	private boolean candidateBlockCorrect;
	private String firstOffendingTx;
	private FeeableData oursData = new FeeableData();
	private FeeableData bitcoindData = new FeeableData();
	private AlgorithmDiffSets algoDiffs = AlgorithmDiffSets.empty();
	private List<Transaction> txOrderedListOurs = new ArrayList<>();
	private List<BlockTemplateTx> txOrderedListBitcoind = new ArrayList<>();

	public static AlgorithmDiff empty() {
		return new AlgorithmDiff();
	}

	private AlgorithmDiff() {

	}

	// Two constructors for the case when we have already calculate if
	// candidateBlock is correct or not
	public AlgorithmDiff(TxMemPool txMemPool, CandidateBlock oursCB, BlockTemplate blockTemplate, int blockHeight,
			boolean candidateBlockCorrect) {
		this.blockHeight = blockHeight;
		this.candidateBlockCorrect = candidateBlockCorrect;
		this.txOrderedListOurs = oursCB.getOrderedStream().map(TxToBeMined::getTx)
				.sorted(Comparator.comparingDouble(Transaction::getSatvByte).reversed()).collect(Collectors.toList());
		order(blockTemplate);

		oursData.checkFees(txOrderedListOurs.stream());
		bitcoindData.checkFees(txOrderedListBitcoind.stream());

		this.algoDiffs = new AlgorithmDiffSets(txMemPool, blockTemplate, oursCB);
		calculateFirstOffending();

	}

	public AlgorithmDiff(TxMemPool txMemPool, CandidateBlock oursCB, BlockTemplate blockTemplate, int blockHeight) {
		this(txMemPool, oursCB, blockTemplate, blockHeight, oursCB.checkIsCorrect().orElse(false));
	}

	private void calculateFirstOffending() {
		Iterator<Transaction> oursIt = txOrderedListOurs.iterator();
		Iterator<BlockTemplateTx> bitcoindIt = txOrderedListBitcoind.iterator();
		while (oursIt.hasNext() && bitcoindIt.hasNext()) {
			Transaction oursTx = oursIt.next();
			BlockTemplateTx bitcoindTx = bitcoindIt.next();
			String oursTxId = oursTx.getTxId();
			String bitcoindTxId = bitcoindTx.getTxId();
			if ((!oursTxId.equals(bitcoindTxId)) && (oursTx.getSatvByte() != bitcoindTx.getSatvByte())) {
				firstOffendingTx = bitcoindTxId;
				break;
			}
		}
	}

	private void order(BlockTemplate blockTemplate) {
		this.txOrderedListBitcoind = blockTemplate.getBlockTemplateTxMap().values().stream()
				.sorted(Comparator.comparingDouble(BlockTemplateTx::getSatvByte).reversed())
				.collect(Collectors.toList());
	}

}
