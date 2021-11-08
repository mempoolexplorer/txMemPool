package com.mempoolexplorer.txmempool.entites.miningqueue;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import com.mempoolexplorer.txmempool.bitcoindadapter.entites.Transaction;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TxGraphList {
    private List<TxGraph> txGraphList = new LinkedList<>();

    public void add(Transaction tx) {
        TxGraph newTxGraph = new TxGraph(tx);

        Iterator<TxGraph> it = txGraphList.iterator();
        while (it.hasNext()) {
            TxGraph next = it.next();
            if (newTxGraph.containsAnyOf(next)) {
                newTxGraph.merge(next);
                it.remove();
            }
        }
        txGraphList.add(newTxGraph);
    }

    public void sort(){
        txGraphList.sort(Comparator.comparing(TxGraph::isNonLinear).thenComparing(txG->txG.getTxSet().size()).reversed());
    }

}
