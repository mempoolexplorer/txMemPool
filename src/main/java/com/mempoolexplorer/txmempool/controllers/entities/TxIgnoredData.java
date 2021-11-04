package com.mempoolexplorer.txmempool.controllers.entities;

import java.util.ArrayList;
import java.util.List;

import com.mempoolexplorer.txmempool.entites.IgnoredTransaction;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TxIgnoredData {

	private List<PrunedIgnoringBlock> ignoringBlocks = new ArrayList<>();
	private double totalSVByteLost;
	private long totalFeesLost;

	public static TxIgnoredData from(IgnoredTransaction ignoredTransaction) {
		TxIgnoredData txIgData = new TxIgnoredData();
		txIgData.setTotalFeesLost(ignoredTransaction.getTotalFeesLost());
		txIgData.setTotalSVByteLost(ignoredTransaction.getTotalSatvBytesLost());
		txIgData.setIgnoringBlocks(ignoredTransaction.getIgnoringBlocks());
		return txIgData;
	}

}
