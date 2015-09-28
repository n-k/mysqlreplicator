package com.github.nk.mysqlreplicator.events;

import java.util.Map;

public class DeleteEvent extends BinLogEvent {

    private final Map<String, Object> data;

    public DeleteEvent(String db, String table, Map<String, Object> data) {
        super(db, table, Type.delete);
        this.data = data;
    }

    public Map<String, Object> getData() {
        return data;
    }

}
