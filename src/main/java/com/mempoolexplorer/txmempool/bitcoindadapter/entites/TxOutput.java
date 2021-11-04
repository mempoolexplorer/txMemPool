package com.mempoolexplorer.txmempool.bitcoindadapter.entites;

import java.util.ArrayList;
import java.util.List;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class TxOutput {
	private List<String> addressIds = new ArrayList<>();// Several addresses if P2PSH, none if unrecognized script.
	private long amount;// In Satoshis.
	private int index;// Begins in 0

	public TxOutput deepCopy() {
		TxOutput txo = new TxOutput();
		List<String> newAddressIds = new ArrayList<>();
		if (this.addressIds != null) {
			newAddressIds.addAll(this.addressIds);
		}
		txo.setAddressIds(newAddressIds);
		txo.setAmount(this.amount);
		txo.setIndex(this.index);
		return txo;
	}
}
