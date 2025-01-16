package ru.inno.adeliya.jdbc.repository;

import ru.inno.adeliya.jdbc.entity.DepartmentEntity;

import java.sql.*;

public class Department implements DepartmentRepository {
    private final String url;
    private final String username;
    private final String password;

    public Department(String url, String username, String password) {
        this.url = url;
        this.username = username;
        this.password = password;
    }

    @Override
    public DepartmentEntity save(DepartmentEntity input) throws SQLException {
        try (Connection connection = DriverManager.getConnection(url, username, password);
             Statement statement = connection.createStatement()) {
            if (input.getId() == 0) {
                String command = String.format(
                        "INSERT INTO department (organization, name) VALUES (%d, '%s') RETURNING id;",
                        input.getOrganization(), input.getName());
                try (ResultSet resultSet = statement.executeQuery(command)) {
                    if (resultSet.next()) {
                        input.setId(resultSet.getInt(1));
                    }
                }
            } else {
                String command = String.format(
                        "UPDATE department SET organization = %d, name = '%s' WHERE id = %d;",
                        input.getOrganization(), input.getName(), input.getId());
                statement.executeUpdate(command);
            }
            return input;
        }
    }

    @Override
    public DepartmentEntity read(int id) throws SQLException {
        String command = String.format("SELECT id, organization, name FROM department WHERE id = %d;", id);
        try (Connection connection = DriverManager.getConnection(url, username, password);
             Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery(command)) {
            if (resultSet.next()) {
                return new DepartmentEntity(
                        resultSet.getInt("id"),
                        resultSet.getInt("organization"),
                        resultSet.getString("name")
                );
            } else {
                return null;
            }
        }
    }

    @Override
    public void delete(int id) throws SQLException {
        String command = String.format("DELETE FROM department WHERE id = %d;", id);
        try (Connection connection = DriverManager.getConnection(url, username, password);
             Statement statement = connection.createStatement()) {
            statement.executeUpdate(command);
        }
    }
}
