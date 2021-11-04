package com.mempoolexplorer.txmempool.controllers.entities;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class TxIdTimesIgnored {
    @JsonProperty("i")
    private String txId;
    @JsonProperty("n")
    private Integer nIgnored;
}