package com.github.nk.mysqlreplicator.config;

import org.springframework.beans.factory.annotation.Value;

import com.github.nk.mysqlreplicator.services.FileStateStore;
import com.github.nk.mysqlreplicator.services.MemoryStateStore;
import com.github.nk.mysqlreplicator.services.StateStore;

public class StateStoreFactoryBean extends AbstractAutowiringFactoryBean<Object> {

    @Value("${statestore.type}")
    private Type type;

    public enum Type {
        memory, file
    }

    @Override
    public Class<?> getObjectType() {
        return StateStore.class;
    }

    @Override
    protected Object doCreateInstance() {
        switch (type) {
        case memory:
            return new MemoryStateStore();
        case file:
            return new FileStateStore();

        default:
            break;
        }
        return null;
    }

}
