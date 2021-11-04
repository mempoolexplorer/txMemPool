package com.mempoolexplorer.txmempool.controllers.entities;

import java.util.ArrayList;
import java.util.List;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class TxDependenciesInfo {

	// First node (nodes.get(0) is currentTx
	List<TxNode> nodes = new ArrayList<>();
	List<DirectedEdge> edges = new ArrayList<>();
}
