package com.mempoolexplorer.txmempool.entites.miningqueue;

import com.mempoolexplorer.txmempool.controllers.entities.CompleteLiveMiningQueueGraphData;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class LiveMiningQueue {

	private CompleteLiveMiningQueueGraphData liveMiningQueueGraphData = CompleteLiveMiningQueueGraphData.empty();
	private MiningQueue miningQueue = MiningQueue.empty();

	public static LiveMiningQueue empty() {
		return new LiveMiningQueue(CompleteLiveMiningQueueGraphData.empty(), MiningQueue.empty());
	}
}
