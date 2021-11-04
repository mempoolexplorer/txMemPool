package com.mempoolexplorer.txmempool;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.circuitbreaker.EnableCircuitBreaker;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.ConfigurableApplicationContext;

@SpringBootApplication
@RefreshScope
@EnableFeignClients
@EnableCircuitBreaker
public class TxMemPoolApplication {

	private static ConfigurableApplicationContext context;

	public static void main(String[] args) {
		context = SpringApplication.run(TxMemPoolApplication.class, args);
	}

	public static void exit() {
		SpringApplication.exit(context, () -> 1);
	}

}
