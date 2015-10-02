package com.github.nk.mysqlreplicator.services;

import com.github.shyiko.mysql.binlog.event.TableMapEventData;

public interface TableInfoProvider {
	TableInfo getTableInfo(TableMapEventData tmed) throws Exception;
}
