package com.mempoolexplorer.txmempool.feinginterfaces.entities;

import java.util.ArrayList;
import java.util.List;

import com.mempoolexplorer.txmempool.bitcoindadapter.entites.Transaction;
import com.mempoolexplorer.txmempool.bitcoindadapter.entites.blockchain.Block;
import com.mempoolexplorer.txmempool.entites.blocktemplate.BlockTemplate;

import lombok.Getter;
import lombok.Setter;


@Getter
@Setter
public class FullStateOnNewBlock {
    private Integer height;
    private Block block;
    private List<String> blockTxIds; // Block TxIds in connected or disconnected Blocks
    private List<Transaction> memPool = new ArrayList<>(); // Mempool as-is before block is received
    private BlockTemplate blockTemplate;// BlockTemplate as-is before block is received
}
