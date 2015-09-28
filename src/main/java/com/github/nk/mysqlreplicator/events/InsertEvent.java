package com.github.nk.mysqlreplicator.events;

import java.util.Map;

public class InsertEvent extends BinLogEvent {

    private final Map<String, Object> data;

    public InsertEvent(String db, String table, Map<String, Object> data) {
        super(db, table, Type.insert);
        this.data = data;
    }

    public Map<String, Object> getData() {
        return data;
    }

}
