package com.github.nk.mysqlreplicator.events;

import java.util.Map;

import com.github.nk.mysqlreplicator.services.TableInfo;

public class DeleteEvent extends BinLogEvent {

    private final Map<String, Object> data;

    public DeleteEvent(TableInfo tInfo, Map<String, Object> data) {
        super(tInfo, Type.delete);
        this.data = data;
    }

    public Map<String, Object> getData() {
        return data;
    }

}
