package com.mempoolexplorer.txmempool.repositories.reactive.custom;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;

import com.mempoolexplorer.txmempool.repositories.entities.MinerNameToBlockHeight;

public class CustomMinerNameToBlockHeightReactiveRepositoryImpl
		implements CustomMinerNameToBlockHeightReactiveRepository {

	@Autowired
	MongoTemplate mt;

	@Override
	public List<String> findDistinctMinerNames() {
		return mt.query(MinerNameToBlockHeight.class).distinct("minerToBlock.minerName").as(String.class).all();
	}

}
