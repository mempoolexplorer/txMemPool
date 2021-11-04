package com.mempoolexplorer.txmempool.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import lombok.Getter;
import lombok.Setter;

@Component
@ConfigurationProperties(prefix = "txmempool")
@Getter
@Setter
public class TxMempoolProperties {

	private int liveMiningQueueRefreshEachMillis = 5000;
	private int miningQueueNumTxs = 100000;
	private int miningQueueMaxNumBlocks = 30;
	private int liveMiningQueueMaxTxs = 100000;
	private int liveMiningQueueGraphSize = 500;
	private int maxLiveDataBufferSize = 100;
	private int numTimesTxIgnoredToRaiseAlarm = 3;
	private int totalSatVBLostToRaiseAlarm = 3;
	private int numTxMinedButNotInMemPoolToRaiseAlarm = 10;
	private boolean detached = false;
}
