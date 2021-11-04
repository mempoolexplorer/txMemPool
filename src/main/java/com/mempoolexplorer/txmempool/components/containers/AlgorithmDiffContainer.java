package com.mempoolexplorer.txmempool.components.containers;

import java.util.Map;
import java.util.Optional;

import com.mempoolexplorer.txmempool.entites.AlgorithmDiff;

public interface AlgorithmDiffContainer {

	void put(AlgorithmDiff ad);

	Map<Integer, AlgorithmDiff> getHeightToAlgoDiffMap();

	Optional<AlgorithmDiff> getLast();

	void drop();
}