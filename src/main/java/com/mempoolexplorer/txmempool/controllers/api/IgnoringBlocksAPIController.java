package com.mempoolexplorer.txmempool.controllers.api;

import com.mempoolexplorer.txmempool.controllers.entities.IgnoringBlockStats;
import com.mempoolexplorer.txmempool.controllers.entities.IgnoringBlockStatsEx;
import com.mempoolexplorer.txmempool.entites.AlgorithmType;
import com.mempoolexplorer.txmempool.entites.IgnoringBlock;
import com.mempoolexplorer.txmempool.repositories.reactive.IgBlockReactiveRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@CrossOrigin
@RestController
@RequestMapping("/ignoringBlocksAPI")
public class IgnoringBlocksAPIController {

        @Autowired
        private IgBlockReactiveRepository igBlockReactiveRepository;

        @GetMapping("/lastIgnoringBlock/{algo}")
        public Mono<IgnoringBlockStatsEx> getIgnoringBlockStatsEx(
                        @PathVariable("algo") AlgorithmType aType) {
                return igBlockReactiveRepository.findByAlgorithmUsedOrderByDbKeyDesc(aType, PageRequest.of(0, 1))
                                .map(IgnoringBlockStatsEx::new).next();
        }

        @GetMapping("/ignoringBlock/{height}/{algo}")
        public Mono<IgnoringBlockStatsEx> getIgnoringBlockStatsEx(@PathVariable("height") Integer height,
                        @PathVariable("algo") AlgorithmType aType) {
                return igBlockReactiveRepository.findById(IgnoringBlock.builDBKey(height, aType))
                                .map(IgnoringBlockStatsEx::new);
        }

        @GetMapping("/ignoringBlocks/{page}/{size}/{algo}")
        public Flux<IgnoringBlockStats> getIgnoringBlocksby(@PathVariable("page") Integer page,
                        @PathVariable("size") Integer size, @PathVariable("algo") AlgorithmType aType) {
                return igBlockReactiveRepository.findByAlgorithmUsedOrderByDbKeyDesc(aType, PageRequest.of(page, size))
                                .map(IgnoringBlockStats::new);
        }
}
