package com.mempoolexplorer.txmempool.controllers.entities;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.mempoolexplorer.txmempool.bitcoindadapter.entites.Transaction;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PrunedLiveMiningQueueGraphData {

	private long lastModTime;

	private int weightInLast10minutes;

	private int fblTxSatVByte;// First Block last Tx SatVByte

	@JsonProperty("mempool")
	private List<CandidateBlockRecap> candidateBlockRecapList = new ArrayList<>();

	private int blockSelected = -1;

	@JsonProperty("blockHistogram")
	private List<PrunedSatVByteHistogramElement> prunedCandidateBlockHistogram = new ArrayList<>();

	private int satVByteSelected = -1;

	@JsonProperty("satVByteHistogram")
	private List<PrunedTx> prunedTxs = new ArrayList<>();

	private int txIndexSelected = -1;

	private String txIdSelected = "";

	private TxDependenciesInfo txDependenciesInfo = null;

	private TxIgnoredData txIgnoredData;

	private Transaction tx;
}
