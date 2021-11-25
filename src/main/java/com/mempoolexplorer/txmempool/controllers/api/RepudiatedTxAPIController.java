package com.mempoolexplorer.txmempool.controllers.api;

import com.mempoolexplorer.txmempool.entites.AlgorithmType;
import com.mempoolexplorer.txmempool.entites.RepudiatedTransaction;
import com.mempoolexplorer.txmempool.repositories.reactive.RepudiatedTxReactiveRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import reactor.core.publisher.Flux;

@CrossOrigin
@RestController
@RequestMapping("/repudiatedTxAPI")
public class RepudiatedTxAPIController {

    @Autowired
    private RepudiatedTxReactiveRepository repudiatedTxReactiveRepository;

    @GetMapping("/repudiatedTxs/{page}/{size}/{algo}")
    public Flux<RepudiatedTransaction> getIgnoringBlocksby(@PathVariable("page") Integer page,
            @PathVariable("size") Integer size, @PathVariable("algo") AlgorithmType aType) {
        return repudiatedTxReactiveRepository.findByaTypeOrderByTimeWhenShouldHaveBeenMinedDesc(aType, PageRequest.of(page, size));
    }
}
