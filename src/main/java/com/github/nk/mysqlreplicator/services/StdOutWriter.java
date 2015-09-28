package com.github.nk.mysqlreplicator.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.nk.mysqlreplicator.events.BinLogEvent;

public class StdOutWriter implements EventWriter {

    private ObjectMapper om = new ObjectMapper();

    @Override
    public void consume(BinLogEvent event) {
        try {
            System.out.println(om.writeValueAsString(event));
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
    }

}
