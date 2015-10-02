package com.github.nk.mysqlreplicator.services;

public interface BinlogPositionProvider {
	String getFilename();

	long getPosition();
}
