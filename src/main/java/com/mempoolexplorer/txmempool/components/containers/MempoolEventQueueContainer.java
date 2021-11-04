package com.mempoolexplorer.txmempool.components.containers;

import java.util.concurrent.BlockingQueue;

import com.mempoolexplorer.txmempool.events.MempoolEvent;

public interface MempoolEventQueueContainer {
    BlockingQueue<MempoolEvent> getBlockingQueue();
}
