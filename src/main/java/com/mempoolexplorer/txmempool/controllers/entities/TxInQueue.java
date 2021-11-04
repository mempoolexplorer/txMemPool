package com.mempoolexplorer.txmempool.controllers.entities;

import com.mempoolexplorer.txmempool.bitcoindadapter.entites.Transaction;

public class TxInQueue {
	public static final int UNKNOWN_POSITION = -1;
	private Transaction tx;
	private int positionInQueue = UNKNOWN_POSITION;

	public TxInQueue(Transaction tx, int positionInQueue) {
		super();
		this.tx = tx;
		this.positionInQueue = positionInQueue;
	}

	public Transaction getTx() {
		return tx;
	}

	public void setTx(Transaction tx) {
		this.tx = tx;
	}

	public int getPositionInQueue() {
		return positionInQueue;
	}

	public void setPositionInQueue(int positionInQueue) {
		this.positionInQueue = positionInQueue;
	}

}
