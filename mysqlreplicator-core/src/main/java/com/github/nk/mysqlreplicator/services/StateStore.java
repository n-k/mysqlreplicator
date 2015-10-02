package com.github.nk.mysqlreplicator.services;

public interface StateStore {
    void saveState(String binlogFileName, long index) throws Exception;

    BinlogState getState() throws Exception;

    public final class BinlogState {
        private final String filename;
        private final long index;

        public BinlogState(String filename, long index) {
            this.filename = filename;
            this.index = index;
        }

        public String getFilename() {
            return filename;
        }

        public long getIndex() {
            return index;
        }

    }
}
