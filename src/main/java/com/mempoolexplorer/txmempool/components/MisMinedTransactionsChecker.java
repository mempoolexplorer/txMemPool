package com.mempoolexplorer.txmempool.components;

import com.mempoolexplorer.txmempool.entites.MisMinedTransactions;

public interface MisMinedTransactionsChecker {

	void check(MisMinedTransactions mmt);

}