package com.mempoolexplorer.txmempool.components;

import com.mempoolexplorer.txmempool.entites.CoinBaseData;

public interface MinerNameResolver {

	CoinBaseData resolveFrom(String coinBaseField);

}