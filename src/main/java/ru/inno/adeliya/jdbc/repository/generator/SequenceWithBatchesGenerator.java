package ru.inno.adeliya.jdbc.repository.generator;

import ru.inno.adeliya.jdbc.config.ConnectionProvider;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.LinkedList;
import java.util.Queue;

public class SequenceWithBatchesGenerator implements IdGenerator<Long> {
    private final Connection connection;
    private final int batchSize;
    private final Queue<Long> idQueue = new LinkedList<>();

    public SequenceWithBatchesGenerator(Connection connection, int batchSize) {
        this.connection = connection;
        this.batchSize = batchSize;
    }

    @Override
    public synchronized Long generate() {
        if (idQueue.isEmpty()) {
            String query = String.format("SELECT nextval('mysequence') FROM generate_series(1, %d)", batchSize);
            try (Statement statement = connection.createStatement();
                 ResultSet resultSet = statement.executeQuery(query)) {
                while (resultSet.next()) {
                    idQueue.offer(resultSet.getLong(1));
                }
            } catch (SQLException e) {
                throw new RuntimeException();
            }
        }
        return idQueue.poll();
    }
}
