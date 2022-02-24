# This is an abandoned project. 
# See https://github.com/mempoolexplorer/mempool-explorer-back for a self-hosted version.
# TxMempool

## .properties configuration

.properties file are loaded by configurationServer service. These properties are almost auto-explicative:

## REST API

### Alarms api

`/alarms/list` List of alarms generated by this service. Errors or unexpected events on mempool.

### Algorithm differences API

`/algo/diffs/{height}` List algorithm differences for a block height (Algorithms includes ours or bitcoind's getblocktemplate.
`/algo/diffs/last` Same as above but for last block
`/algo/liveDiffs` Compares candidate blocks of one algoritm with the other, using the tx that are now in mempool

### Ignored Transactions

`/liveIgnored/{algo}/txs` List the ignored txs depending on the algorithm ('ours' or 'bitcoind')
`/liveIgnored/{algo}/fullTxs` Same as above but returns all tx's data including tx ins and outs.
`/liveIgnored/{algo}/txs/{txId}` List the ignored tx with txId depending on the algorithm ('ours' or 'bitcoind')
`/liveIgnored/{algo}/fullTxs/{txId}` Same as above but returns all tx's data including tx ins and outs.
`/liveIgnored/{algo}/txsNTimes/{nTimes}` List the ignored txs depending on the algorithm ('ours' or 'bitcoind') and having been ignored nTimes at least.
`/liveIgnored/{algo}/blocks` List statistics of all ignoring blocks in cache for an algorithm ('ours' or 'bitcoind')
`/liveIgnored/{algo}/blocks/{height}` List statistics of block with 'height' in cache for an algorithm ('ours' or 'bitcoind').
`/liveIgnored/{algo}/blocks/last` List statistics of last block in cache for an algorithm ('ours' or 'bitcoind').

### Live Mining Queue data

`/liveMiningQueue/graphicData` Returns a histogram of the miningQueue for graphical representation.

## Compilation with https:

You have to follow the guidelines in https://www.thomasvitale.com/https-spring-boot-ssl-certificate/ but here is a recap:
1. Add spring-boot-starter-security as a dependency in build.gradle
1. Uncomment lines in ServerConfig.java and WebSecurityConfig.java
1. Uncomment lines starting with "server.ssl." in txMempoolX.yml and add "- ~/.rsassl:/ssl" to the volume section.
1. You can generate a certificate derived from a self-signed certificate using info in https://deliciousbrains.com/ssl-certificate-authority-for-local-https-development/ also you can execute certGenerator.sh to generate a derived certificate automatically.
1. You have to store your password as a enviroment variable using .env file, config server or .yml file.
