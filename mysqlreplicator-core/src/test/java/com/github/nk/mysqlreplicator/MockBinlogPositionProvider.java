package com.github.nk.mysqlreplicator;

import com.github.nk.mysqlreplicator.services.BinlogPositionProvider;

public class MockBinlogPositionProvider implements BinlogPositionProvider {
	private String filename;
	private long position;

	public String getFilename() {
		return filename;
	}

	public void setFilename(String filename) {
		this.filename = filename;
	}

	public long getPosition() {
		return position;
	}

	public void setPosition(long position) {
		this.position = position;
	}

}
