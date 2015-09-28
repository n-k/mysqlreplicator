package com.github.nk.mysqlreplicator.config;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.github.nk.mysqlreplicator.events.BinLogEvent;

@Configuration
public class BinLogEventsQueueConfig {

    @Value("${queue.size:10}")
    private int queueSize;

    @Bean(name = "binlogEventQueue")
    public BlockingQueue<BinLogEvent> getQueue() {
        return new ArrayBlockingQueue<>(queueSize);
    }

}
