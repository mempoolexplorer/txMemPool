package com.mempoolexplorer.txmempool.feinginterfaces;

import java.util.Map;

import com.mempoolexplorer.txmempool.bitcoindadapter.entites.AppStateEnum;
import com.mempoolexplorer.txmempool.bitcoindadapter.entites.Transaction;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;

@FeignClient("bitcoindAdapter")
public interface BitcoindAdapter {

	@GetMapping(value = "/memPool/state", consumes = "application/json")
	AppStateEnum getState();

	@GetMapping(value = "/memPool/full", consumes = "application/json")
	Map<String, Transaction> getFullMemPool();

	@GetMapping(value = "/memPool/size", consumes = "application/json")
	Integer getMemPoolSize();


}
