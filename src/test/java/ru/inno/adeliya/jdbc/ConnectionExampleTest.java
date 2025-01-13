package ru.inno.adeliya.jdbc;

import org.junit.jupiter.api.Test;

import java.sql.DriverManager;
import java.sql.SQLException;

class ConnectionExampleTest {
    @Test
    void printAllTableValues() throws SQLException {
        var connection = DriverManager.getConnection("jdbc:postgresql://localhost:5432/postgres", "user", "password");
        String select = "SELECT * from \"adeliya-learn\".department";
        var result = connection.createStatement().executeQuery(select);
        while (result.next()) {
            System.out.println(String.format("%s %s %s",
                    result.getString(1),
                    result.getString(2),
                    result.getString(3)));
        }
    }

    @Test
    void insertNewValuesIntoTable() throws SQLException {
        var connection = DriverManager.getConnection("jdbc:postgresql://localhost:5432/postgres", "user", "password");
        String insert = "INSERT INTO \"adeliya-learn\".department (id, organization, name)\n" +
                "VALUES (?, ?, ?)\n" +
                "ON CONFLICT (id) DO UPDATE SET\n" +
                "    organization = EXCLUDED.organization,\n" +
                "    name = EXCLUDED.name\n" +
                "RETURNING id;";
        try (var preparedStatement = connection.prepareStatement(insert)) {
            preparedStatement.setInt(1, 16);
            preparedStatement.setInt(2, 3);
            preparedStatement.setString(3, "Отдел отделов");
            try (var result = preparedStatement.executeQuery()) {
                if (result.next()) {
                    int insertedId = result.getInt("id");
                    System.out.println("inserted id: " + insertedId);
                    String select = "SELECT * FROM \"adeliya-learn\".department WHERE id = ?";
                    try (var selectionStatement = connection.prepareStatement(select)) {
                        selectionStatement.setInt(1, insertedId);
                        try (var selectResult = selectionStatement.executeQuery()) {
                            while (selectResult.next()) {
                                System.out.println(String.format("%s %s %s",
                                        selectResult.getInt("id"),
                                        selectResult.getString("organization"),
                                        selectResult.getString("name")));
                            }
                        }
                    }
                }
            }
        }
    }

}