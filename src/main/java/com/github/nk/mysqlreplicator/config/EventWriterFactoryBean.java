package com.github.nk.mysqlreplicator.config;

import org.springframework.beans.factory.annotation.Value;

import com.github.nk.mysqlreplicator.services.ESWriter;
import com.github.nk.mysqlreplicator.services.EventWriter;
import com.github.nk.mysqlreplicator.services.StdOutWriter;

public class EventWriterFactoryBean extends AbstractAutowiringFactoryBean<Object> {

    @Value("${writer.type}")
    private Type type;

    public enum Type {
        sysout, es
    }

    @Override
    public Class<?> getObjectType() {
        return EventWriter.class;
    }

    @Override
    protected Object doCreateInstance() {
        switch (type) {
        case sysout:
            return new StdOutWriter();
        case es:
            return new ESWriter();

        default:
            break;
        }
        return null;
    }

}
