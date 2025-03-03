package ru.inno.adeliya.jdbc.repository.generator;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class NextValSequenceGenerator implements IdGenerator<Integer> {
    private final Connection connection;
    private final String sequenceName;

    public NextValSequenceGenerator(Connection connection, String sequenceName) {
        this.connection = connection;
        this.sequenceName = sequenceName;
    }

    public Integer generate() {
        String query = String.format("SELECT nextval('%s')", sequenceName);
        try (Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery(query)) {
            if (resultSet.next()) {
                return resultSet.getInt(1);
            }
            throw new RuntimeException("No result from nextval");
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
}
