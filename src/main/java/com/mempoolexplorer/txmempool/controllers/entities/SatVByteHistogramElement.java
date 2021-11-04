package com.mempoolexplorer.txmempool.controllers.entities;

import java.util.List;
import java.util.Map;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class SatVByteHistogramElement {

	private int modSatVByte;
	private int numTxs;
	private int weight;
	private List<TxIdAndWeight> txIdAndWeightList;
	//Map that gives index of a txId in the list above
	private Map<String,Integer> txIdToListIndex;

}
