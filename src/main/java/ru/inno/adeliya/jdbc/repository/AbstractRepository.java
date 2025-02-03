package ru.inno.adeliya.jdbc.repository;

import ru.inno.adeliya.jdbc.config.ConnectionProvider;
import ru.inno.adeliya.jdbc.entity.Column;
import ru.inno.adeliya.jdbc.entity.Table;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public abstract class AbstractRepository<T> implements EntityRepository<T> {

    private final ConnectionProvider connectionProvider;
    private final String tableName;
    private final Class<T> entityClass;

    public AbstractRepository(ConnectionProvider connectionProvider) {
        this.connectionProvider = connectionProvider;
        this.entityClass = (Class<T>) ((ParameterizedType) getClass()
                .getGenericSuperclass()).getActualTypeArguments()[0];
        this.tableName = entityClass.getAnnotation(Table.class).name();
    }

    abstract boolean isNew(T entity);

    abstract void setId(T entity, int id);

    public String getSelectQuery(int id) {
        return String.format(
                "SELECT * FROM %s WHERE id = %d;",
                tableName,
                id
        );
    }

    public String getDeleteQuery(int id) {
        return String.format(
                "DELETE FROM %s WHERE id = %d;",
                tableName,
                id
        );
    }

    public T readResultSet(ResultSet resultSet) throws SQLException {
        try {
            Constructor<T> constructor = entityClass.getConstructor(ResultSet.class);
            return constructor.newInstance(resultSet);
        } catch (Exception e) {
            throw new SQLException("Problem with entity and resultSet");
        }
    }

    public String getInsertQuery(T entity) {
        StringBuilder columns = new StringBuilder();
        StringBuilder values = new StringBuilder();
        Field[] fields = entityClass.getDeclaredFields();
        for (Field field : fields) {
            if (field.isAnnotationPresent(Column.class)) {
                field.setAccessible(true);
                try {
                    String columnName = field.getAnnotation(Column.class).name();
                    columns.append(columnName).append(", ");
                    values.append("'").append(field.get(entity)).append("', ");
                } catch (IllegalAccessException e) {
                    System.out.println(e.getMessage());
                }
            }
        }
        columns.setLength(columns.length() - 2);
        values.setLength(values.length() - 2);
        return String.format(
                "INSERT INTO %s (%s) VALUES (%s) RETURNING id;",
                tableName,
                columns,
                values
        );
    }

    public String getUpdateQuery(T entity) {
        StringBuilder newValues = new StringBuilder();
        String condition = null;
        Field[] fields = entityClass.getDeclaredFields();
        for (Field field : fields) {
            if (field.isAnnotationPresent(Column.class)) {
                field.setAccessible(true);
                try {
                    String columnName = field.getAnnotation(Column.class).name();
                    Object fieldValue = field.get(entity);
                    if (columnName.equals("id")) {
                        condition = "id = " + fieldValue;
                    } else {
                        newValues.append(columnName).append(" = '").append(fieldValue).append("', ");
                    }
                } catch (IllegalAccessException e) {
                    System.out.println(e.getMessage());
                }
            }
        }
        newValues.setLength(newValues.length() - 2);
        return String.format(
                "UPDATE %s SET %s WHERE %s;",
                tableName,
                newValues,
                condition
        );
    }

    @Override
    public T save(T entity) throws SQLException {
        try (Statement statement = connectionProvider.getConnection().createStatement()) {
            if (isNew(entity)) {
                String command = getInsertQuery(entity);
                System.out.println("creating new %s : %s".formatted(entity, command));
                try (ResultSet resultSet = statement.executeQuery(command)) {
                    if (resultSet.next()) {
                        setId(entity, resultSet.getInt(1));
                    }

                }
            } else {
                String command = getUpdateQuery(entity);
                System.out.println("updating %s : %s".formatted(entity, command));
                statement.executeUpdate(command);

            }
            return entity;
        }
    }

    @Override
    public T read(int id) throws SQLException {
        String command = getSelectQuery(id);
        try (Statement statement = connectionProvider.getConnection().createStatement();
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
        try (Statement statement = connectionProvider.getConnection().createStatement()) {
            statement.executeUpdate(command);

        }
    }
}