package com.mempoolexplorer.txmempool.controllers;

import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.mempoolexplorer.txmempool.components.containers.AlgorithmDiffContainer;
import com.mempoolexplorer.txmempool.controllers.errors.ErrorDetails;
import com.mempoolexplorer.txmempool.controllers.exceptions.BlockNotFoundException;
import com.mempoolexplorer.txmempool.controllers.exceptions.ServiceNotReadyYetException;
import com.mempoolexplorer.txmempool.entites.AlgorithmDiff;

@RestController
@RequestMapping("/algo")
public class AlgoDiffController {

	@Autowired
	private AlgorithmDiffContainer algoDiffContainer;

	@GetMapping("/diffs/{height}")
	public AlgorithmDiff getAlgorithmDifferences(@PathVariable("height") Integer height) throws BlockNotFoundException {
		AlgorithmDiff algorithmDifferences = algoDiffContainer.getHeightToAlgoDiffMap().get(height);
		if (algorithmDifferences == null) {
			throw new BlockNotFoundException();
		}
		return algorithmDifferences;
	}

	@GetMapping("/diffs/last")
	public AlgorithmDiff getLastAlgorithmDifferences() throws BlockNotFoundException {
		Optional<AlgorithmDiff> opAlgorithmDifferences = algoDiffContainer.getLast();
		if (opAlgorithmDifferences.isEmpty()) {
			throw new BlockNotFoundException();
		}
		return opAlgorithmDifferences.get();
	}

	@ExceptionHandler(ServiceNotReadyYetException.class)
	public ResponseEntity<ErrorDetails> onServiceNotReadyYetException(ServiceNotReadyYetException e) {
		ErrorDetails errorDetails = new ErrorDetails();
		errorDetails.setErrorMessage(e.getMessage());
		errorDetails.setErrorCode(HttpStatus.NOT_FOUND.toString());
		return new ResponseEntity<>(errorDetails, HttpStatus.NOT_FOUND);
	}

	@ExceptionHandler(BlockNotFoundException.class)
	public ResponseEntity<ErrorDetails> onIgnoringBlockNotFound(BlockNotFoundException e) {
		ErrorDetails errorDetails = new ErrorDetails();
		errorDetails.setErrorMessage(e.getMessage());
		errorDetails.setErrorCode(HttpStatus.NOT_FOUND.toString());
		return new ResponseEntity<>(errorDetails, HttpStatus.NOT_FOUND);
	}

}
