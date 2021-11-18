package com.mempoolexplorer.txmempool.controllers.api;

import java.util.List;

import com.mempoolexplorer.txmempool.components.containers.IgTxCacheContainer;
import com.mempoolexplorer.txmempool.controllers.entities.TxIdTimesIgnored;
import com.mempoolexplorer.txmempool.entites.AlgorithmType;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@CrossOrigin
@RestController
@RequestMapping("/ignoredTxAPI")
public class IgnoredTxAPIController {

    @Autowired
    private IgTxCacheContainer igTxCacheContainer;

    //TODO: delete
    @GetMapping("/ignoredTxs")
    public List<TxIdTimesIgnored> getIgnoredTxs() {
        return igTxCacheContainer.getIgTxList(AlgorithmType.OURS);
    }

    @GetMapping("/ignoredTxs/{algo}")
    public List<TxIdTimesIgnored> hasBlock(@PathVariable("algo") AlgorithmType aType) {
        return igTxCacheContainer.getIgTxList(aType);
    }
}