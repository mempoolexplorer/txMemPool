package com.mempoolexplorer.txmempool.controllers.entities;

import java.util.ArrayList;
import java.util.List;

import com.mempoolexplorer.txmempool.bitcoindadapter.entites.Transaction;
import com.mempoolexplorer.txmempool.bitcoindadapter.entites.TxInput;
import com.mempoolexplorer.txmempool.bitcoindadapter.entites.TxOutput;

import lombok.Getter;

//Parts of Transaction that are invariant, so can be catched.
@Getter
public class InvariantTxParts {

    private InvariantTxParts() {
    }

    public InvariantTxParts(Transaction tx) {
        txId = tx.getTxId();
        txInputs = tx.getTxInputs();
        txOutputs = tx.getTxOutputs();
    }

    private String txId;
    private List<TxInput> txInputs = new ArrayList<>();
    private List<TxOutput> txOutputs = new ArrayList<>();
}
