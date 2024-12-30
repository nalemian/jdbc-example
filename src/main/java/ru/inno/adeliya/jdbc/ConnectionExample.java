package ru.inno.adeliya.jdbc;

import java.sql.DriverManager;
import java.sql.SQLException;

public class ConnectionExample {
    public static void main(String[] args) throws SQLException {
        var connection = DriverManager.getConnection("jdbc:postgresql://localhost:5432/postgres", "user", "password");
        String select = "SELECT * from \"adeliya-learn\".department";
        var result = connection.createStatement().executeQuery(select);
        while (result.next()){
            System.out.println(String.format("%s %s %s",
                    result.getString(1),
                    result.getString(2),
                    result.getString(3)));
        }
        System.out.println(1);
    }
}
