package com.mempoolexplorer.txmempool.repositories.reactive;

import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import org.springframework.stereotype.Repository;

import com.mempoolexplorer.txmempool.repositories.entities.MinerNameToBlockHeight;
import com.mempoolexplorer.txmempool.repositories.reactive.custom.CustomMinerNameToBlockHeightReactiveRepository;

import reactor.core.publisher.Flux;

@Repository
public interface MinerNameToBlockHeightReactiveRepository extends
		ReactiveMongoRepository<MinerNameToBlockHeight, String>, CustomMinerNameToBlockHeightReactiveRepository {

	Flux<MinerNameToBlockHeight> findTop20ByMinerToBlockMinerNameOrderByMedianMinedTimeDesc(String minerName);

}
