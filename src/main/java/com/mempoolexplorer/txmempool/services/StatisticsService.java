package com.mempoolexplorer.txmempool.services;

import com.mempoolexplorer.txmempool.controllers.entities.RecalculateAllStatsResult;
import com.mempoolexplorer.txmempool.entites.IgnoringBlock;

public interface StatisticsService {

	RecalculateAllStatsResult recalculateAllStats();

	void saveStatisticsToDB(IgnoringBlock iGBlockBitcoind, IgnoringBlock iGBlockOurs);

}