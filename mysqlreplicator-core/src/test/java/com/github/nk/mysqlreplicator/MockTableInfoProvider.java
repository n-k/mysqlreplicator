package com.github.nk.mysqlreplicator;

import com.github.nk.mysqlreplicator.services.TableInfo;
import com.github.nk.mysqlreplicator.services.TableInfoProvider;
import com.github.shyiko.mysql.binlog.event.TableMapEventData;

public class MockTableInfoProvider implements TableInfoProvider {

	private TableInfo tableInfo;

	@Override
	public TableInfo getTableInfo(TableMapEventData tmed) throws Exception {
		return tableInfo;
	}

	public void setTableInfo(TableInfo tableInfo) {
		this.tableInfo = tableInfo;
	}

}
