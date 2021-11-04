package com.mempoolexplorer.txmempool.services;

import java.util.Collection;
import java.util.List;

import com.mempoolexplorer.txmempool.components.TxMemPool;
import com.mempoolexplorer.txmempool.entites.IgnoringBlock;
import com.mempoolexplorer.txmempool.entites.NotMinedTransaction;

public interface IgnoredEntitiesService {

        public void onDeleteTx(String txId);

        public void onNewBlockConnected(IgnoringBlock igBlock, List<String> blockTxIds,
                        Collection<NotMinedTransaction> ignoredTxs);

        void onNewBlockDisconnected(IgnoringBlock igBlock, List<String> blockTxIds,
                        Collection<NotMinedTransaction> ignoredTxs);

        public void cleanIgTxNotInMempool(TxMemPool txMemPool);

        public void onRecalculateBlockFromRecorder(IgnoringBlock igBlock);

}
