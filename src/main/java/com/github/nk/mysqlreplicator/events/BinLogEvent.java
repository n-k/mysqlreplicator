package com.github.nk.mysqlreplicator.events;

import com.github.nk.mysqlreplicator.services.TableInfo;

public abstract class BinLogEvent {

    public enum Type {
        insert, update, delete
    }

    private final Type event;
    private final TableInfo tInfo;

    public BinLogEvent(TableInfo tInfo, Type event) {
        this.tInfo = tInfo;
        this.event = event;
    }

    public String getDb() {
        return tInfo.getData().getDatabase();
    }

    public String getTable() {
        return tInfo.getData().getTable();
    }

    public String getPrimaryKey() {
        return tInfo.getPrimaryKey();
    }

    public Type getEvent() {
        return event;
    }

}
