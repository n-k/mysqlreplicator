package com.github.nk.mysqlreplicator.services;

import java.util.List;

public class ColumnInfo {

    private final String name;
    private final String typeLowerCase;
    private final List<String> enumValues;

    public ColumnInfo(String name, String typeLowerCase, List<String> enumValues) {
        this.name = name;
        this.typeLowerCase = typeLowerCase;
        this.enumValues = enumValues;
    }

    public String getName() {
        return name;
    }

    public String getTypeLowerCase() {
        return typeLowerCase;
    }

    public List<String> getEnumValues() {
        return enumValues;
    }

}
