package com.mempoolexplorer.txmempool.components.containers;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import com.mempoolexplorer.txmempool.events.MempoolEvent;

import org.springframework.stereotype.Component;

@Component
public class MempoolEventQueueContainerImpl implements MempoolEventQueueContainer {

    BlockingQueue<MempoolEvent> blockingQueue = new LinkedBlockingQueue<>();

    @Override
    public BlockingQueue<MempoolEvent> getBlockingQueue() {
        return this.blockingQueue;
    }

}
