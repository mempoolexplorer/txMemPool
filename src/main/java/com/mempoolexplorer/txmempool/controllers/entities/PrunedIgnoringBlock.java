package com.mempoolexplorer.txmempool.controllers.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.mempoolexplorer.txmempool.entites.CoinBaseData;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PrunedIgnoringBlock {
	private int height;
	@JsonIgnore
	private String hash;// This is needed to uniquely identify block.
	private int txsInMinedBlock;
	private int txsInCandidateBlock;
	private int posInCandidateBlock;
	private long time;
	CoinBaseData coinBaseData;
}
