package com.mempoolexplorer.txmempool.entites;

import java.util.Optional;

import com.mempoolexplorer.txmempool.bitcoindadapter.entites.Transaction;

/**
 * Class that represents a transaction that should be mined and it's not.
 * Prefers containment over extends to avoid copy constructor
 */
public class NotMinedTransaction implements Feeable {

	/**
	 * This is the ordinal position of the transaction in the blockQueue.
	 */
	private Transaction tx;
	private Optional<Integer> ordinalpositionInBlock;

	public NotMinedTransaction(Transaction transaction, Optional<Integer> ordinalpositionInBlock) {
		super();
		this.tx = transaction;
		this.ordinalpositionInBlock = ordinalpositionInBlock;
	}

	public Optional<Integer> getOrdinalPositionInBlock() {
		return ordinalpositionInBlock;
	}

	public Transaction getTx() {
		return tx;
	}

	@Override
	public String getTxId() {
		return tx.getTxId();
	}

	@Override
	public double getSatvByteIncludingAncestors() {
		return tx.getSatvByteIncludingAncestors();
	}

	@Override
	public double getSatvByte() {
		return tx.getSatvByte();
	}

	@Override
	public long getBaseFees() {
		return tx.getBaseFees();
	}

	@Override
	public long getAncestorFees() {
		return tx.getAncestorFees();
	}

	@Override
	public int getWeight() {
		return tx.getWeight();
	}

}
