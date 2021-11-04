package com.mempoolexplorer.txmempool.entites.blocktemplate;

import java.util.HashMap;
import java.util.Map;

import com.mempoolexplorer.txmempool.bitcoindadapter.entites.blocktemplate.BlockTemplateTx;
import com.mempoolexplorer.txmempool.entites.miningqueue.TxContainer;
import com.mempoolexplorer.txmempool.utils.SysProps;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class BlockTemplate implements TxContainer {

	// Height of the bock to be mined with this template.
	private int height;
	private Map<String, BlockTemplateTx> blockTemplateTxMap = new HashMap<>(SysProps.HM_INITIAL_CAPACITY_FOR_BLOCK);

	public static BlockTemplate empty() {
		return new BlockTemplate();
	}

	@Override
	public boolean containsKey(String txId) {
		return blockTemplateTxMap.containsKey(txId);
	}

}
