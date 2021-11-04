package com.mempoolexplorer.txmempool.components;

import com.mempoolexplorer.txmempool.bitcoindadapter.entites.mempool.TxPoolChanges;

public interface Tx10minBuffer {
    void register(TxPoolChanges txPoolChanges);

    int getTotalWeight();
}
