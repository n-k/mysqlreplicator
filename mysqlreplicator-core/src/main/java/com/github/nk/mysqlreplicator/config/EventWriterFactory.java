package com.github.nk.mysqlreplicator.config;

import com.github.nk.mysqlreplicator.services.EventWriter;

public interface EventWriterFactory {
    EventWriter getEventWriter();
}
