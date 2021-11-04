package com.mempoolexplorer.txmempool.bitcoindadapter.entites;

import java.util.ArrayList;
import java.util.List;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class TxInput {
	private List<String> addressIds = new ArrayList<>();// Several addresses if P2PSH, none if unrecognized script. or
	// coinBase transaction
	private long amount;// In Satoshis.
	private String txId;// Transaction where output is being spent by this input
	private int vOutIndex;// The output index number (vout) of the outpoint being spent. The first output
	// in a transaction has an index of 0. Not present if this is a coinbase
	// transaction
	private String coinbase;// Coinbase, normally null.

	public TxInput deepCopy() {
		TxInput txi = new TxInput();

		List<String> newAddressIds = new ArrayList<>();
		if (this.addressIds != null) {
			newAddressIds.addAll(this.addressIds);
		}
		txi.setAddressIds(newAddressIds);
		txi.setAmount(this.amount);
		txi.setTxId(this.txId);
		txi.setVOutIndex(this.vOutIndex);
		txi.setCoinbase(this.coinbase);
		return txi;
	}

}
