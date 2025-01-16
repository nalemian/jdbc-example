package ru.inno.adeliya.jdbc.repository;

import ru.inno.adeliya.jdbc.entity.EmployeeEntity;

import java.sql.*;

public class Employee implements EntityRepository<EmployeeEntity> {

    private final String url;
    private final String username;
    private final String password;

    public Employee(String url, String username, String password) {
        this.url = url;
        this.username = username;
        this.password = password;
    }

    @Override
    public EmployeeEntity save(EmployeeEntity input) throws SQLException {
        try (Connection connection = DriverManager.getConnection(url, username, password);
             Statement statement = connection.createStatement()) {
            if (input.getId() == 0) {
                String command = String.format(
                        "INSERT INTO employee (name, salary, department) VALUES ('%s', %d, %d) RETURNING id;",
                        input.getName(), input.getSalary(), input.getDepartment());
                try (ResultSet resultSet = statement.executeQuery(command)) {
                    if (resultSet.next()) {
                        input.setId(resultSet.getInt(1));
                    }
                }
            } else {
                String command = String.format(
                        "UPDATE employee SET name = '%s', salary = %d, department = %d WHERE id = %d;",
                        input.getName(), input.getSalary(), input.getDepartment(), input.getId());
                statement.executeUpdate(command);
            }
            return input;
        }
    }

    @Override
    public EmployeeEntity read(int id) throws SQLException {
        String command = String.format("SELECT id, name, salary, department FROM employee WHERE id = %d;", id);
        try (Connection connection = DriverManager.getConnection(url, username, password);
             Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery(command)) {
            if (resultSet.next()) {
                return new EmployeeEntity(
                        resultSet.getInt("id"),
                        resultSet.getString("name"),
                        resultSet.getInt("salary"),
                        resultSet.getInt("department")
                );
            } else {
                return null;
            }
        }
    }

    @Override
    public void delete(int id) throws SQLException {
        String command = String.format("DELETE FROM employee WHERE id = %d;", id);
        try (Connection connection = DriverManager.getConnection(url, username, password);
             Statement statement = connection.createStatement()) {
            statement.executeUpdate(command);
        }
    }
}
