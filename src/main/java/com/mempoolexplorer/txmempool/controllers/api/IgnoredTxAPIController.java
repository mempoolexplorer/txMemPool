package com.mempoolexplorer.txmempool.controllers.api;

import java.util.List;

import com.mempoolexplorer.txmempool.components.containers.IgTxCacheContainer;
import com.mempoolexplorer.txmempool.controllers.entities.TxIdTimesIgnored;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@CrossOrigin
@RestController
@RequestMapping("/ignoredTxAPI")
public class IgnoredTxAPIController {

    @Autowired
    private IgTxCacheContainer igTxCacheContainer;

    @GetMapping("/ignoredTxs")
    public List<TxIdTimesIgnored> getIgnoredTxs() {
        return igTxCacheContainer.getIgTxList();
    }

}