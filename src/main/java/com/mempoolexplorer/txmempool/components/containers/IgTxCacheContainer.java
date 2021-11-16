package com.mempoolexplorer.txmempool.components.containers;

import java.util.List;

import com.mempoolexplorer.txmempool.controllers.entities.TxIdTimesIgnored;

public interface IgTxCacheContainer {

    public List<TxIdTimesIgnored> getIgTxList();

    public void calculate();
}
