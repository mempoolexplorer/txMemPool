package com.mempoolexplorer.txmempool.components;

import java.time.Instant;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import com.mempoolexplorer.txmempool.bitcoindadapter.entites.Transaction;
import com.mempoolexplorer.txmempool.bitcoindadapter.entites.mempool.TxPoolChanges;
import com.mempoolexplorer.txmempool.entites.TxMempoolStats;

public interface TxMemPool {

	void refresh(TxPoolChanges txPoolChanges);

	Integer getTxNumber();

	TxMempoolStats getTxMempoolStats();

	Stream<Transaction> getDescendingTxStream();

	Set<String> getAllParentsOf(Transaction tx);

	Set<String> getAllChildrenOf(Transaction tx);

	boolean containsTxId(String txId);

	boolean containsAddrId(String addrId);

	Optional<Transaction> getTx(String txId);

	Set<String> getTxIdsOfAddress(String addrId);
	
	Stream<Transaction> getTxsAfter(Instant instant);

	void drop();


}