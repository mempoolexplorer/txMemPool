package com.mempoolexplorer.txmempool.controllers;

import com.mempoolexplorer.txmempool.components.containers.MempoolEventQueueContainer;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/pendingEvents")
public class EventQueueController {
    @Autowired
    private MempoolEventQueueContainer queueContainer;

    @GetMapping("/size")
    public int getSize() {
        return queueContainer.getBlockingQueue().size();
    }

}
