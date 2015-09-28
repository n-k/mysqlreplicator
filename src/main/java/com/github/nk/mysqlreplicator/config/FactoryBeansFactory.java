package com.github.nk.mysqlreplicator.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class FactoryBeansFactory {

    @Bean
    public EventWriterFactoryBean getEventConsumerFactory() {
        return new EventWriterFactoryBean();
    }

    @Bean
    public StateStoreFactoryBean getStateStoreFactory() {
        return new StateStoreFactoryBean();
    }
}
