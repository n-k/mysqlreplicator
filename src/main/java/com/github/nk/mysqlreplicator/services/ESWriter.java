package com.github.nk.mysqlreplicator.services;

import org.elasticsearch.client.Client;
import org.springframework.beans.factory.annotation.Value;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.nk.mysqlreplicator.events.BinLogEvent;

public class ESWriter implements EventWriter {

    @Value("${writer.es.hosts}")
    private String hosts;

    @Value("${writer.es.port}")
    private int port;

    @Value("${writer.es.cluster}")
    private String cluster;

    private ObjectMapper om = new ObjectMapper();
    private ThreadLocal<Client> clients = new ThreadLocal<>();

    @Override
    public void consume(BinLogEvent event) {
        try {
            System.out.println(om.writeValueAsString(event));
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
    }

    private Client getClient() {
        return null;
    }
}
