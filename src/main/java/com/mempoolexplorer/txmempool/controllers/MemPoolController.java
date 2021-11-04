package com.mempoolexplorer.txmempool.controllers;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import com.mempoolexplorer.txmempool.bitcoindadapter.entites.Transaction;
import com.mempoolexplorer.txmempool.components.TxMemPool;
import com.mempoolexplorer.txmempool.controllers.errors.ErrorDetails;
import com.mempoolexplorer.txmempool.controllers.exceptions.AddressNotFoundInMemPoolException;
import com.mempoolexplorer.txmempool.controllers.exceptions.TransactionNotFoundException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/memPool")
public class MemPoolController {

	private static final String NF = " not found.";

	@Autowired
	private TxMemPool txMemPool;

	@GetMapping("/size")
	public Integer getSize() {
		return txMemPool.getTxNumber();
	}

	@GetMapping("exist/{txId}")
	public Boolean existTxId(@PathVariable("txId") String txId) {
		return txMemPool.containsTxId(txId);
	}

	@GetMapping("existAddr/{addrId}")
	public Boolean existAddrId(@PathVariable("addrId") String addrId) {
		return txMemPool.containsAddrId(addrId);
	}

	@GetMapping("/{txId}")
	public Transaction getTxId(@PathVariable("txId") String txId) throws TransactionNotFoundException {
		Optional<Transaction> tx = txMemPool.getTx(txId);
		if (tx.isPresent()) {
			return tx.get();
		}
		throw new TransactionNotFoundException("txId: " + txId + NF);
	}

	@GetMapping("/fullRaw")
	public List<Transaction> getRawTxList() {
		return txMemPool.getDescendingTxStream().collect(Collectors.toList());
	}

	@GetMapping("/fullTxIds")
	public List<String> getTxIdsList() {
		return txMemPool.getDescendingTxStream().map(Transaction::getTxId).collect(Collectors.toList());
	}

	@GetMapping("/parentsOf/{txId}")
	public Set<String> getParentsOfTxId(@PathVariable("txId") String txId) throws TransactionNotFoundException {
		Optional<Transaction> tx = txMemPool.getTx(txId);
		if (tx.isPresent()) {
			return txMemPool.getAllParentsOf(tx.get());
		}
		throw new TransactionNotFoundException("txId: " + txId + NF);
	}

	@GetMapping("/address/{addrId}")
	public Set<String> getTxIdsHavingAddrId(@PathVariable("addrId") String addrId)
			throws AddressNotFoundInMemPoolException {
		Set<String> txIdsOfAddress = txMemPool.getTxIdsOfAddress(addrId);
		if (txIdsOfAddress.isEmpty()) {
			throw new AddressNotFoundInMemPoolException("addrId: " + addrId + NF);
		}
		return txIdsOfAddress;
	}

	@GetMapping("/nonStandardTxs")
	public List<String> getNonStandardTxs() {
		List<String> retList = new ArrayList<>();
		txMemPool.getDescendingTxStream().forEach(tx -> {
			if (tx.isNonStandard()) {
				retList.add(tx.getTxId());
			}
		});
		return retList;
	}

	@ExceptionHandler(TransactionNotFoundException.class)
	public ResponseEntity<ErrorDetails> onTransactionNotFound(TransactionNotFoundException e) {
		ErrorDetails errorDetails = new ErrorDetails();
		errorDetails.setErrorMessage(e.getMessage());
		errorDetails.setErrorCode(HttpStatus.NOT_FOUND.toString());
		return new ResponseEntity<>(errorDetails, HttpStatus.NOT_FOUND);
	}

	@ExceptionHandler(AddressNotFoundInMemPoolException.class)
	public ResponseEntity<ErrorDetails> onAddressNotFound(AddressNotFoundInMemPoolException e) {
		ErrorDetails errorDetails = new ErrorDetails();
		errorDetails.setErrorMessage(e.getMessage());
		errorDetails.setErrorCode(HttpStatus.NOT_FOUND.toString());
		return new ResponseEntity<>(errorDetails, HttpStatus.NOT_FOUND);
	}

}
