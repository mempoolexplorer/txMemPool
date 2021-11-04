package com.mempoolexplorer.txmempool.feinginterfaces;

import com.mempoolexplorer.txmempool.feinginterfaces.entities.FullStateOnNewBlock;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient("mempoolRecorder")
public interface MempoolRecorder {
    
    @GetMapping(value = "/fullStateOnNewBlock/height/{height}", consumes = "application/json")
	FullStateOnNewBlock getFullStateOnNewBlock(@PathVariable int height);

    @GetMapping(value = "/fullStateOnNewBlock/hasBlock/{height}", consumes = "application/json")
	Boolean getHasBlock(@PathVariable int height);


}
