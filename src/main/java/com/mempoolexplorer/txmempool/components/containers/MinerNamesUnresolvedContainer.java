package com.mempoolexplorer.txmempool.components.containers;

import java.util.List;

import com.mempoolexplorer.txmempool.entites.MinerNameUnresolved;

public interface MinerNamesUnresolvedContainer {

	List<MinerNameUnresolved> getMinerNamesUnresolvedList();

	void addCoinBaseField(String coinBaseField, int blockHeight);

}