package com.mempoolexplorer.txmempool;

import com.mempoolexplorer.txmempool.properties.TxMempoolProperties;
import com.mempoolexplorer.txmempool.repositories.entities.MinerNameToBlockHeight;
import com.mempoolexplorer.txmempool.threads.MempoolEventConsumer;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.cloud.stream.binder.Binding;
import org.springframework.cloud.stream.binder.BindingCreatedEvent;
import org.springframework.context.annotation.Profile;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.index.IndexOperations;
import org.springframework.data.mongodb.core.index.IndexResolver;
import org.springframework.data.mongodb.core.index.MongoPersistentEntityIndexResolver;
import org.springframework.data.mongodb.core.mapping.MongoMappingContext;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

@Component
@Profile(value = { AppProfiles.DEV, AppProfiles.PROD })
@Slf4j
public class AppLifeCycle {

    @Value("${spring.cloud.stream.bindings.txMemPoolEvents.destination}")
    private String topic;

    @Autowired
    private MongoTemplate mongoTemplate;

    @Autowired
    private MongoMappingContext mongoMappingContext;

    @Autowired
    private MempoolEventConsumer mempoolEventConsumer;

    @Autowired
    private TxMempoolProperties properties;

    // It seems that Spring aplicaton events are thrown more than once, so these are
    // the flags to avoid calling clean-up methods more than once.
    private boolean hasInitializated = false;// Avoids intialization more than once
    private boolean isShutingdown = false; // Avoids finalization more than once
    private boolean onApplicationReadyEvent = false;
    private boolean onBindingCreatedEvent = false;

    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReadyEvent(ApplicationReadyEvent event) {
        if (!onApplicationReadyEvent) {// only once.
            initIndicesAfterStartup();
        }
        onApplicationReadyEvent = true;
        checkInitialization();
    }

    public void initIndicesAfterStartup() {
        IndexResolver resolver = new MongoPersistentEntityIndexResolver(mongoMappingContext);
        IndexOperations indexOps = mongoTemplate.indexOps(MinerNameToBlockHeight.class);
        resolver.resolveIndexFor(MinerNameToBlockHeight.class).forEach(indexOps::ensureIndex);
    }

    // Sent when kafka binding is done. Wait for it since we don't want to send
    // things before kafka initialization.
    @EventListener(BindingCreatedEvent.class)
    public void onBindingCreatedEvent(BindingCreatedEvent event) {
        @SuppressWarnings("unchecked") // Since we are receving this event we know it's type
        Binding<Object> binding = (Binding<Object>) event.getSource();
        // Checks that event.source is the same as our kafka topic
        if (binding.getName().compareTo(topic) == 0) {
            onBindingCreatedEvent = true;
            checkInitialization();
        }
    }

    // @PreDestroy <- This is not good, better use this:
    @EventListener(ContextClosedEvent.class)
    public void finalization() {
        if (isShutingdown)
            return;// No more than once
        isShutingdown = true;
        log.info("Shuttingdown MempoolRecorder...");
        mempoolEventConsumer.shutdown();
        log.info("MempoolRecorder shutdown complete.");
    }

    public void checkInitialization() {
        if (onApplicationReadyEvent && onBindingCreatedEvent && !hasInitializated) {
            hasInitializated = true;
            initialization();
        }
    }

    private void initialization() {
        if (properties.isDetached()) {
            log.info("TXMEMPOOL IS RUNNING IN DETACHED MODE.");
        }
        log.info("MempoolEventConsumer is starting...");
        mempoolEventConsumer.start();
        log.info("MempoolEventConsumer started.");
    }

}
