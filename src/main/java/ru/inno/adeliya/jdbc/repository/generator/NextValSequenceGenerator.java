package ru.inno.adeliya.jdbc.repository.generator;

import ru.inno.adeliya.jdbc.config.ConnectionProvider;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class NextValSequenceGenerator implements IdGenerator<Long> {
    private final Connection connection;

    public NextValSequenceGenerator(Connection connection) {
        this.connection = connection;
    }

    @Override
    public Long generate() {
        try (Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery("SELECT nextval('mysequence')")) {
            if (resultSet.next()) {
                return resultSet.getLong(1);
            }
            throw new RuntimeException();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
}
