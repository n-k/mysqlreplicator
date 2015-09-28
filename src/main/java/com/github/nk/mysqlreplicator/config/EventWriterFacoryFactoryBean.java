package com.github.nk.mysqlreplicator.config;

import java.util.concurrent.BlockingQueue;

import javax.annotation.Resource;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import com.github.nk.mysqlreplicator.events.BinLogEvent;
import com.github.nk.mysqlreplicator.services.ESWriter;
import com.github.nk.mysqlreplicator.services.EventWriter;
import com.github.nk.mysqlreplicator.services.StdOutWriter;

public class EventWriterFacoryFactoryBean extends AbstractAutowiringFactoryBean<Object> {

    @Value("${writer.type}")
    private Type type;

    @Autowired
    @Resource(name = "binlogEventQueue")
    private BlockingQueue<BinLogEvent> eventQueue;

    public enum Type {
        sysout, es
    }

    @Override
    public Class<?> getObjectType() {
        return EventWriterFactory.class;
    }

    @Override
    protected Object doCreateInstance() {
        return new EventWriterFactoryImpl();
    }

    private class EventWriterFactoryImpl implements EventWriterFactory {

        @Override
        public EventWriter getEventWriter() {
            EventWriter instance = null;
            switch (type) {
            case sysout:
                instance = new StdOutWriter(eventQueue);
            case es:
                instance = new ESWriter(eventQueue);

            default:
                break;
            }
            if (instance != null) {
                getApplicationContext().getAutowireCapableBeanFactory().autowireBean(instance);
            }
            return instance;
        }

    }

}
