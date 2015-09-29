package com.github.nk.mysqlreplicator.services;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Value;

public class MemoryStateStore implements StateStore {

    @Value("${statestore.writenth}")
    private int writeNth;
    
    @Value("${statestore.memory.binlogfile:null}")
    private String initFileName;
    
    @Value("${statestore.memory.binlogindex:0}")
    private int initIndex;

    private int count;
    private String filename = null;
    private long index = 0;
    
    @PostConstruct
    public void init() {
        if ("null".equals(initFileName)) {
            System.out.println("init filename was null str");
            initFileName = null;
        }
        filename = initFileName;
        index = initIndex;
    }

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
        System.out.println("getting binlog from:" + filename + "/" + index);
        return new BinlogState(filename, index);
    }

}
