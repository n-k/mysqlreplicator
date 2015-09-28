package com.github.nk.mysqlreplicator.services;

import java.util.concurrent.BlockingQueue;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.nk.mysqlreplicator.events.BinLogEvent;

public class StdOutWriter extends EventWriter {

    private ObjectMapper om = new ObjectMapper();

    public StdOutWriter(BlockingQueue<BinLogEvent> q) {
        super(q);
    }

    @Override
    public void consume() {
        try {
            BinLogEvent event = q.take();
            System.out.println(om.writeValueAsString(event));
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

}
