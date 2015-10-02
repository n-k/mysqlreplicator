package com.github.nk.mysqlreplicator.events;

import java.util.Map;

import com.github.nk.mysqlreplicator.services.TableInfo;

public class UpdateEvent extends BinLogEvent {

    private final Map<String, Object> before;
    private final Map<String, Object> after;

    public UpdateEvent(TableInfo tInfo, Map<String, Object> before, Map<String, Object> after) {
        super(tInfo, Type.update);
        this.before = before;
        this.after = after;
    }

    public Map<String, Object> getBefore() {
        return before;
    }

    public Map<String, Object> getAfter() {
        return after;
    }

}
