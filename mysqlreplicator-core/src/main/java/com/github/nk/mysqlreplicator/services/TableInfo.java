package com.github.nk.mysqlreplicator.services;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import com.github.shyiko.mysql.binlog.event.TableMapEventData;

public class TableInfo {

    private final TableMapEventData data;
    private final List<ColumnInfo> colInfos;
    private final String primaryKey;

    public TableInfo(TableMapEventData data, List<ColumnInfo> colInfos, String primaryKey) {
        this.data = data;
        this.colInfos = colInfos;
        this.primaryKey = primaryKey;
    }

    public TableMapEventData getData() {
        return data;
    }

    public List<ColumnInfo> getColInfos() {
        return colInfos;
    }

    public String getPrimaryKey() {
        return primaryKey;
    }

    public static TableInfo getInfo(String host, int port, String user, String pass, TableMapEventData data)
            throws Exception {
        String url = "jdbc:mysql://" + host + ":" + port + "/" + data.getDatabase();
        Connection con = DriverManager.getConnection(url, user, pass);
        DatabaseMetaData meta = con.getMetaData();
        ResultSet result = meta.getPrimaryKeys(null, data.getDatabase(), data.getTable());
        String primaryKey = null;
        if (result.next()) {
            primaryKey = result.getString(4);
        }
        result.close();
        ResultSet colsRS = meta.getColumns(null, null, data.getTable(), null);
        List<ColumnInfo> columns = new ArrayList<ColumnInfo>();

        while (colsRS.next()) {
            String name = colsRS.getString("COLUMN_NAME");
            String typeLowerCase = colsRS.getString("TYPE_NAME").toLowerCase();
            if (typeLowerCase == "enum") {
                List<String> enumValues = getEnumValues(con, data.getTable(), name);
                columns.add(new ColumnInfo(name, typeLowerCase, enumValues));
            } else {
                columns.add(new ColumnInfo(name, typeLowerCase, new ArrayList<>()));
            }
        }
        colsRS.close();
        con.close();
        return new TableInfo(data, columns, primaryKey);
    }

    private static List<String> getEnumValues(Connection con, String table, String name) throws Exception {
        Statement stmt = con.createStatement();
        String sql = "SHOW COLUMNS FROM " + table + " LIKE '" + name + "'";
        ResultSet rs = stmt.executeQuery(sql);
        if (!rs.next())
            throw new Exception(sql + " returns empty result");

        String enm = rs.getString("Type");
        rs.close();
        stmt.close();
        if (!enm.startsWith("enum("))
            throw new Exception(table + "." + name + " is not an enum");

        String valueString = enm.substring("enum(".length(), enm.length() - 1);
        String[] quotedValues = valueString.split(",");
        // val ret = new Array[String](quotedValues.size)
        List<String> values = new ArrayList<>();
        for (String quotedValue : quotedValues) {
            String trimedQuotedValue = quotedValue.trim();
            String value = trimedQuotedValue.substring(1, trimedQuotedValue.length() - 1);
            values.add(value);
        }
        return values;
    }
}
