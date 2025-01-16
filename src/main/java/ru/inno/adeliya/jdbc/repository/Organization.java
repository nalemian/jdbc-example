package ru.inno.adeliya.jdbc.repository;

import ru.inno.adeliya.jdbc.entity.OrganizationEntity;

import java.sql.*;

public class Organization implements EntityRepository<OrganizationEntity> {
    private final String url;
    private final String username;
    private final String password;

    public Organization(String url, String username, String password) {
        this.url = url;
        this.username = username;
        this.password = password;
    }

    @Override
    public OrganizationEntity save(OrganizationEntity input) throws SQLException {
        try (Connection connection = DriverManager.getConnection(url, username, password);
             Statement statement = connection.createStatement()) {
            if (input.getId() == 0) {
                String command = String.format(
                        "INSERT INTO organization (name, tax_number) VALUES ('%s', %d) RETURNING id;",
                        input.getName(), input.getTax_number());
                try (ResultSet resultSet = statement.executeQuery(command)) {
                    if (resultSet.next()) {
                        input.setId(resultSet.getInt(1));
                    }
                }
            } else {
                String command = String.format(
                        "UPDATE organization SET name = '%s', tax_number = %d WHERE id = %d;",
                        input.getName(), input.getTax_number(), input.getId());
                statement.executeUpdate(command);
            }
            return input;
        }
    }

    @Override
    public OrganizationEntity read(int id) throws SQLException {
        String command = String.format("SELECT id, name, tax_number FROM organization WHERE id = %d;", id);
        try (Connection connection = DriverManager.getConnection(url, username, password);
             Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery(command)) {
            if (resultSet.next()) {
                return new OrganizationEntity(
                        resultSet.getInt("id"),
                        resultSet.getString("name"),
                        resultSet.getInt("tax_number")
                );
            } else {
                return null;
            }
        }
    }

    @Override
    public void delete(int id) throws SQLException {
        String command = String.format("DELETE FROM organization WHERE id = %d;", id);
        try (Connection connection = DriverManager.getConnection(url, username, password);
             Statement statement = connection.createStatement()) {
            statement.executeUpdate(command);
        }
    }
}
