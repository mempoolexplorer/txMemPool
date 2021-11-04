package com.mempoolexplorer.txmempool.controllers;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.mempoolexplorer.txmempool.components.containers.MinerNamesUnresolvedContainer;
import com.mempoolexplorer.txmempool.entites.MinerNameUnresolved;

@RestController
@RequestMapping("/minerNames")
public class MinerNamesUnresolvedController {

	@Autowired
	private MinerNamesUnresolvedContainer mnuContainer;
	
	@GetMapping("/unresolved")
	public List<MinerNameUnresolved> getSize() {
		return mnuContainer.getMinerNamesUnresolvedList();
	}

}
