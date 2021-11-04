package com.mempoolexplorer.txmempool.repositories.reactive;

import com.mempoolexplorer.txmempool.entites.RepudiatedTransaction;

import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RepudiatedTxReactiveRepository extends ReactiveMongoRepository<RepudiatedTransaction, String> {

}
