package com.github.nk.mysqlreplicator.services;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;

import org.springframework.beans.factory.annotation.Value;

public class FileStateStore implements StateStore {

    private static final String SEP = "\\|\\|\\|\\|";

    @Value("${statestore.file.location}")
    private String location;
    
    @Value("${statestore.writenth}")
    private int writeNth;
    
    private volatile int count = 0;

    @Override
    public void saveState(String binlogFileName, long index) throws Exception {
        count++;
        if (count % writeNth == 0) {
            count = 0;
            writeFileContents(location, binlogFileName + "||||" + index);
        }
    }

    @Override
    public BinlogState getState() {
        String contents = null;
        try {
            contents = getFileContents(location);
        } catch (Exception e) {
            e.printStackTrace();
            return new BinlogState(null, 0);
        }
        String[] parts = contents.split(SEP);
        return new BinlogState(parts[0], Long.parseLong(parts[1]));
    }

    private static String getFileContents(String location) throws Exception {
        String str = "";
        FileInputStream fis = new FileInputStream(location);
        InputStreamReader isr = new InputStreamReader(fis);
        BufferedReader br = new BufferedReader(isr);
        str = br.readLine();
        br.close();
        isr.close();
        fis.close();
        return str;
    }

    private static void writeFileContents(String location, String contents) throws Exception {
        FileOutputStream fos = new FileOutputStream(location);
        OutputStreamWriter osw = new OutputStreamWriter(fos);
        BufferedWriter bw = new BufferedWriter(osw);
        bw.write(contents);
        bw.flush();
        osw.flush();
        fos.flush();
        bw.close();
        osw.close();
        fos.close();
    }
}
