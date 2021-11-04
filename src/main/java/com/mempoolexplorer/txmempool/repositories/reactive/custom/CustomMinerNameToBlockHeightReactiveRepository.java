package com.mempoolexplorer.txmempool.repositories.reactive.custom;

import java.util.List;

public interface CustomMinerNameToBlockHeightReactiveRepository {

	List <String> findDistinctMinerNames();
}
