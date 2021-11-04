package com.mempoolexplorer.txmempool.components.health;

import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Component
public class MempoolSyncedHealthIndicator implements HealthIndicator {

    private boolean mempoolSynced = false;

    @Override
    public Health health() {
        if (mempoolSynced) {
            return Health.up().build();
        } else {
            return Health.down().build();
        }
    }

}
