package custom;

import org.w3c.dom.Node;

import java.util.Arrays;

public class JoinTable {
    String tableName;
    String[] columns;

    public JoinTable(String tableName, String[] columns) {
        this.tableName=tableName;
        this.columns=columns;
    }


    @Override
    public String toString() {
        return "JoinTable{" +
                "tableName='" + tableName + '\'' +
                ", columns=" + Arrays.toString(columns) +
                '}';
    }

    public String[] getColumns() {
        return columns;
    }

    public String getTableName() {
        return tableName;
    }
}
