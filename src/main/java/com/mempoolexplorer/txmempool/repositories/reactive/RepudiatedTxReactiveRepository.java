package com.mempoolexplorer.txmempool.repositories.reactive;

import com.mempoolexplorer.txmempool.entites.AlgorithmType;
import com.mempoolexplorer.txmempool.entites.IgnoredTxState;
import com.mempoolexplorer.txmempool.entites.RepudiatedTransaction;

import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import org.springframework.stereotype.Repository;

import reactor.core.publisher.Flux;

@Repository
public interface RepudiatedTxReactiveRepository extends ReactiveMongoRepository<RepudiatedTransaction, String> {

    Flux<RepudiatedTransaction> findByaTypeOrderByTimeWhenShouldHaveBeenMinedDesc(AlgorithmType aType,
            Pageable pageable);

    Flux<RepudiatedTransaction> findByState(IgnoredTxState state);

}
