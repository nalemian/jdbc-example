package ru.inno.adeliya.jdbc.repository;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public abstract class AbstractRepository<T> implements EntityRepository<T> {

    private final Connection connection;

    public AbstractRepository(Connection connection) {
        this.connection = connection;
    }

    abstract String getInsertQuery(T entity);

    abstract String getUpdateQuery(T entity);

    abstract String getSelectQuery(int id);

    abstract String getDeleteQuery(int id);

    abstract T readResultSet(ResultSet resultSet) throws SQLException;

    abstract boolean isNew(T entity);

    abstract void setId(T entity, int id);

    @Override
    public T save(T entity) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            if (isNew(entity)) {
                String command = getInsertQuery(entity);
                try (ResultSet resultSet = statement.executeQuery(command)) {
                    if (resultSet.next()) {
                        setId(entity, resultSet.getInt(1));
                    }
                }
            } else {
                String command = getUpdateQuery(entity);
                statement.executeUpdate(command);
            }
            return entity;
        }
    }

    @Override
    public T read(int id) throws SQLException {
        String command = getSelectQuery(id);
        try (Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery(command)) {
            if (resultSet.next()) {
                return readResultSet(resultSet);
            }
            return null;
        }
    }

    @Override
    public void delete(int id) throws SQLException {
        String command = getDeleteQuery(id);
        try (Statement statement = connection.createStatement()) {
            statement.executeUpdate(command);
        }
    }
}