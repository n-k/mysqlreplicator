package com.github.nk.mysqlreplicator.services;

import java.util.Map;
import java.util.concurrent.BlockingQueue;

import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.update.UpdateRequest;
import org.elasticsearch.client.Client;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.settings.ImmutableSettings;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.transport.InetSocketTransportAddress;
import org.springframework.beans.factory.annotation.Value;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.nk.mysqlreplicator.events.BinLogEvent;
import com.github.nk.mysqlreplicator.events.BinLogEvent.Type;
import com.github.nk.mysqlreplicator.events.DeleteEvent;
import com.github.nk.mysqlreplicator.events.InsertEvent;
import com.github.nk.mysqlreplicator.events.UpdateEvent;

public class ESWriter extends EventWriter {

    @Value("${writer.es.hosts}")
    private String hosts;

    @Value("${writer.es.port}")
    private int port;

    @Value("${writer.es.cluster}")
    private String cluster;

    private ObjectMapper om = new ObjectMapper();
    private Client client;

    public ESWriter(BlockingQueue<BinLogEvent> q) {
        super(q);
    }

    @Override
    public void consume() {
        try {
            BinLogEvent event = q.take();
            if (event == null) {
                return;
            }
            System.out.println(om.writeValueAsString(event));
            String db = event.getDb();
            String table = event.getTable();
            String primaryKey = event.getPrimaryKey();
            Map<String, Object> data = null;
            switch (event.getEvent()) {
            case insert:
                data = ((InsertEvent) event).getData();
                break;
            case update:
                data = ((UpdateEvent) event).getAfter();
                break;
            case delete:
                data = ((DeleteEvent) event).getData();
                break;
            default:
                // log
                return;
            }
            if (data == null) {
                return;
            }
            Object pkObj = data.get(primaryKey);
            if (pkObj == null) {
                return;
            }
            String pk = String.valueOf(pkObj);
            String id = db + "_" + table + "_" + pk;
            if (event.getEvent() == Type.delete) {
                delete(db, table, id);
            } else {
                upsert(db, table, id, data);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void delete(String index, String doc, String id) {
        getClient().prepareDelete(index, doc, id)
                .execute()
                .actionGet();
    }

    private void upsert(String index, String doc, String id, Map<String, Object> data) throws Exception {
        IndexRequest indexRequest = new IndexRequest(index, doc, id).source(data);
        UpdateRequest updateRequest = new UpdateRequest(index, doc, id).doc(data).upsert(indexRequest);
        getClient().update(updateRequest).get();
    }

    private Client getClient() {
        if (this.client != null) {
            return this.client;
        }
        Settings settings = ImmutableSettings.settingsBuilder().put("cluster.name", cluster).build();
        TransportClient client = new TransportClient(settings);
        for (String h : hosts.split(",")) {
            client.addTransportAddress(new InetSocketTransportAddress(h, 9300));
        }
        this.client = client;
        return client;
    }
}
