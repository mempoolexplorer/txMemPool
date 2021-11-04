package com.mempoolexplorer.txmempool.controllers.entities;

import java.util.ArrayList;
import java.util.List;

import lombok.Data;

@Data
public class RecalculateAllStatsResult {

	private List<String> executionInfoList = new ArrayList<>();
	
}
