package com.mempoolexplorer.txmempool.components.containers;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.springframework.stereotype.Component;

import com.mempoolexplorer.txmempool.entites.MinerNameUnresolved;

@Component
public class MinerNamesUnresolvedContainerImpl implements MinerNamesUnresolvedContainer {

	private List<MinerNameUnresolved> minerNamesUnresolvedList = Collections.synchronizedList(new ArrayList<>());

	@Override
	public void addCoinBaseField(String coinBaseField, int blockHeight) {
		minerNamesUnresolvedList.add(new MinerNameUnresolved(coinBaseField, blockHeight));
	}

	@Override
	public List<MinerNameUnresolved> getMinerNamesUnresolvedList() {
		return minerNamesUnresolvedList;
	}

}