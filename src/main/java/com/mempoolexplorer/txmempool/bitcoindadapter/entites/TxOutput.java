package com.mempoolexplorer.txmempool.bitcoindadapter.entites;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class TxOutput {
	private String address;// Can be null
	private long amount;// In Satoshis.
	private int index;// Begins in 0

	public TxOutput deepCopy() {
		TxOutput txo = new TxOutput();
		txo.setAddress(this.address);
		txo.setAmount(this.amount);
		txo.setIndex(this.index);
		return txo;
	}
}
