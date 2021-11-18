package com.mempoolexplorer.txmempool.components.containers;

import java.util.List;

import com.mempoolexplorer.txmempool.controllers.entities.TxIdTimesIgnored;
import com.mempoolexplorer.txmempool.entites.AlgorithmType;

public interface IgTxCacheContainer {

    public List<TxIdTimesIgnored> getIgTxList(AlgorithmType aType);

    public void calculate();
}
