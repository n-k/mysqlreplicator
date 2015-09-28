package com.github.nk.mysqlreplicator.services;

import org.springframework.beans.factory.annotation.Value;

public class MemoryStateStore implements StateStore {

    @Value("${statestore.writenth}")
    private int writeNth;

    private int count;
    private String filename = null;
    private long index = 0;

    @Override
    public void saveState(String binlogFileName, long index) {
        if (count++ % writeNth == 0) {
            count = 0;
            filename = binlogFileName;
            this.index = index;
            System.out.println("Saved position:" + binlogFileName + "/" + index);
        }
    }

    @Override
    public BinlogState getState() {
        return new BinlogState(filename, index);
    }

}
