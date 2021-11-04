package com.mempoolexplorer.txmempool.controllers.api;

import java.util.Comparator;

import com.mempoolexplorer.txmempool.controllers.entities.TxIdTimesIgnored;
import com.mempoolexplorer.txmempool.entites.AlgorithmType;
import com.mempoolexplorer.txmempool.repositories.reactive.IgTransactionReactiveRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import reactor.core.publisher.Flux;

@CrossOrigin
@RestController
@RequestMapping("/ignoredTxAPI")
public class IgnoredTxAPIController {

    @Autowired
    private IgTransactionReactiveRepository igTxReactiveRepository;

    @GetMapping("/ignoredTxs")
    public Flux<TxIdTimesIgnored> getIgnoredTxs() {

        return igTxReactiveRepository.findAll().filter(igTx -> igTx.getAType() == AlgorithmType.OURS)
                .map(igTx -> new TxIdTimesIgnored(igTx.getTxId(), Integer.valueOf(igTx.getIgnoringBlocks().size())))
                .sort(Comparator.comparingInt(TxIdTimesIgnored::getNIgnored).reversed()
                        .thenComparing(TxIdTimesIgnored::getTxId));
    }
}