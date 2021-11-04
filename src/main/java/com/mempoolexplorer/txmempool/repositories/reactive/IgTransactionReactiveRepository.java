package com.mempoolexplorer.txmempool.repositories.reactive;

import com.mempoolexplorer.txmempool.entites.IgnoredTransaction;

import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface IgTransactionReactiveRepository extends ReactiveMongoRepository<IgnoredTransaction, String> {
    
}
