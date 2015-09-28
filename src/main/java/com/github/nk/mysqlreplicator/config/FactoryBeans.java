package com.github.nk.mysqlreplicator.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class FactoryBeans {

    @Bean
    public EventWriterFacoryFactoryBean getEventConsumerFactoryFactory() {
        return new EventWriterFacoryFactoryBean();
    }

    @Bean
    public StateStoreFactoryBean getStateStoreFactory() {
        return new StateStoreFactoryBean();
    }
}
