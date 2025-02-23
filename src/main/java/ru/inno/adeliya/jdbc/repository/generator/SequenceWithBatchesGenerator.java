package ru.inno.adeliya.jdbc.repository.generator;

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
    private final String sequenceName;

    public SequenceWithBatchesGenerator(Connection connection, int batchSize, String sequenceName) {
        this.connection = connection;
        this.batchSize = batchSize;
        this.sequenceName = sequenceName;
    }

    @Override
    public synchronized Long generate() {
        if (idQueue.isEmpty()) {
            String query = String.format("SELECT nextval('%s') FROM generate_series(1, %d)", sequenceName, batchSize);
            try (Statement statement = connection.createStatement();
                 ResultSet resultSet = statement.executeQuery(query)) {
                while (resultSet.next()) {
                    idQueue.offer(resultSet.getLong(1));
                }
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        }
        return idQueue.poll();
    }
}
