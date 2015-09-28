package com.github.nk.mysqlreplicator.services;

import com.github.nk.mysqlreplicator.events.BinLogEvent;

/**
 * Interface of event writers. These are passed com.github.nk.mysqlreplicator.events.*
 * objects
 * 
 * Note that the same instance will be used by all writer threads.
 */
public interface EventWriter {
    public void consume(BinLogEvent event);
}
