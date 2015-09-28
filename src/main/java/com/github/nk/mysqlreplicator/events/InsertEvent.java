package com.github.nk.mysqlreplicator.events;

import java.util.Map;

import com.github.nk.mysqlreplicator.services.TableInfo;

public class InsertEvent extends BinLogEvent {

    private final Map<String, Object> data;

    public InsertEvent(TableInfo tInfo, Map<String, Object> data) {
        super(tInfo, Type.insert);
        this.data = data;
    }

    public Map<String, Object> getData() {
        return data;
    }

}
