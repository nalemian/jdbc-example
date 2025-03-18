package ru.inno.adeliya.jdbc.repository;

import ru.inno.adeliya.jdbc.config.ConnectionProvider;
import ru.inno.adeliya.jdbc.entity.Column;
import ru.inno.adeliya.jdbc.entity.Table;
import ru.inno.adeliya.jdbc.repository.generator.IdGenerator;

import java.lang.reflect.*;
import java.sql.*;
import java.util.*;

public abstract class AbstractRepository<T, ID> implements EntityRepository<T, ID> {

    private final ConnectionProvider connectionProvider;
    private final String tableName;
    private final IdGenerator<ID> generator;
    private final Map<Field, Method> getters;
    private final List<Field> allColumnFields;
    private final List<Field> updateFields;
    private final List<String> allColumnNames;
    private final Field idField;
    private final PreparedStatement insertPreparedStatement;
    private final PreparedStatement updatePreparedStatement;
    private final Constructor<T> resultSetConstructor;


    public AbstractRepository(ConnectionProvider connectionProvider, IdGenerator<ID> generator) {
        this.connectionProvider = connectionProvider;
        this.generator = generator;
        Class<T> entityClass = (Class<T>) ((ParameterizedType) getClass()
                .getGenericSuperclass()).getActualTypeArguments()[0];
        this.tableName = entityClass.getAnnotation(Table.class).name();
        Map<Field, Method> tempGetters = new HashMap<>();
        List<Field> tempAllFields = new ArrayList<>();
        List<String> tempAllColumnNames = new ArrayList<>();
        List<Field> tempUpdateFields = new ArrayList<>();
        List<String> updateColumnNames = new ArrayList<>();
        Field tempIdField = null;
        for (Field field : entityClass.getDeclaredFields()) {
            if (field.isAnnotationPresent(Column.class)) {
                String fieldName = field.getName();
                String getterName = "get" + Character.toUpperCase(fieldName.charAt(0)) + fieldName.substring(1);
                try {
                    Method getter = entityClass.getMethod(getterName);
                    tempGetters.put(field, getter);
                } catch (NoSuchMethodException e) {
                    throw new RuntimeException("Getter not found for field: " + fieldName);
                }
                tempAllFields.add(field);
                String columnName = field.getAnnotation(Column.class).name();
                tempAllColumnNames.add(columnName);
                if ("id".equals(columnName)) {
                    tempIdField = field;
                } else {
                    tempUpdateFields.add(field);
                    updateColumnNames.add(columnName);
                }
            }
        }
        if (tempIdField == null) {
            throw new RuntimeException("No field with column name 'id' found in " + entityClass.getName());
        }
        this.getters = tempGetters;
        this.allColumnFields = tempAllFields;
        this.allColumnNames = tempAllColumnNames;
        this.updateFields = tempUpdateFields;
        this.idField = tempIdField;
        try {
            Connection connection = connectionProvider.getConnection();
            String insertColumns = String.join(", ", allColumnNames);
            String insertPlaceholders = String.join(", ", Collections.nCopies(allColumnNames.size(), "?"));
            String insertQuery = String.format("INSERT INTO %s (%s) VALUES (%s) RETURNING id;", tableName, insertColumns, insertPlaceholders);
            this.insertPreparedStatement = connection.prepareStatement(insertQuery, Statement.RETURN_GENERATED_KEYS);
            List<String> columnsForUpdate = new ArrayList<>();
            for (String col : updateColumnNames) {
                columnsForUpdate.add(col + " = ?");
            }
            String updateQuery = String.format("UPDATE %s SET %s WHERE id = ?;", tableName, String.join(", ", columnsForUpdate));
            this.updatePreparedStatement = connection.prepareStatement(updateQuery, Statement.RETURN_GENERATED_KEYS);
        } catch (SQLException e) {
            throw new RuntimeException("Error with creating prepared statements");
        }
        try {
            this.resultSetConstructor = entityClass.getConstructor(ResultSet.class);
        } catch (NoSuchMethodException e) {
            throw new RuntimeException("No constructor with ResultSet parameter in " + entityClass.getName());
        }

    }

    public int count() throws SQLException {
        String query = "SELECT COUNT(*) FROM " + tableName;
        try (Statement statement = connectionProvider.getConnection().createStatement();
             ResultSet resultSet = statement.executeQuery(query)) {
            if (resultSet.next()) {
                return resultSet.getInt(1);
            }
            return 0;
        }
    }

    abstract boolean isNew(T entity);

    abstract void setId(T entity, ID id);

    public String getSelectQuery(ID id) {
        return String.format(
                "SELECT * FROM %s WHERE id = %s;",
                tableName,
                id
        );
    }

    public String getDeleteQuery(ID id) {
        return String.format(
                "DELETE FROM %s WHERE id = %s;",
                tableName,
                id
        );
    }

    public String getInsertQuery(T entity) {
        StringBuilder columns = new StringBuilder();
        StringBuilder values = new StringBuilder();
        for (int i = 0; i < allColumnFields.size(); i++) {
            Field field = allColumnFields.get(i);
            String columnName = allColumnNames.get(i);
            columns.append(columnName).append(", ");
            try {
                Object value = getters.get(field).invoke(entity);
                values.append("'").append(value).append("', ");
            } catch (Exception e) {
                throw new RuntimeException("Error with getting value for field " + field.getName());
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
        for (int i = 0; i < allColumnFields.size(); i++) {
            Field field = allColumnFields.get(i);
            String columnName = allColumnNames.get(i);
            try {
                Object value = getters.get(field).invoke(entity);
                if ("id".equals(columnName)) {
                    condition = "id = " + value;
                } else {
                    newValues.append(columnName).append(" = '").append(value).append("', ");
                }
            } catch (Exception e) {
                throw new RuntimeException("Error updating a field " + field.getName());
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
        String command;
        if (isNew(entity)) {
            setId(entity, generator.generate());
            command = getInsertQuery(entity);
        } else {
            command = getUpdateQuery(entity);
        }
        try (Statement statement = connectionProvider.getConnection().createStatement();
             ResultSet resultSet = statement.executeQuery(command)) {
            if (resultSet.next()) {
                System.out.println(resultSet.getInt(1));
            }
        }
        return entity;
    }

    @Override
    public T read(ID id) throws SQLException {
        String command = getSelectQuery(id);
        try (Statement statement = connectionProvider.getConnection().createStatement();
             ResultSet resultSet = statement.executeQuery(command)) {
            if (resultSet.next()) {
                try {
                    return resultSetConstructor.newInstance(resultSet);
                } catch (Exception e) {
                    throw new SQLException("Problem with entity and resultSet");
                }
            }
            return null;
        }
    }

    @Override
    public void delete(ID id) throws SQLException {
        String command = getDeleteQuery(id);
        try (Statement statement = connectionProvider.getConnection().createStatement()) {
            statement.executeUpdate(command);

        }
    }

    public Collection<T> saveAll(Collection<T> entities) {
        if (entities.isEmpty()) {
            return Collections.emptyList();
        }
        List<T> newEntities = new ArrayList<>();
        List<T> updateEntities = new ArrayList<>();
        for (T entity : entities) {
            if (isNew(entity)) {
                newEntities.add(entity);
            } else {
                updateEntities.add(entity);
            }
        }
        if (!newEntities.isEmpty()) {
            try {
                processBatch(newEntities, insertPreparedStatement, true);
                try (ResultSet generatedKeys = insertPreparedStatement.getGeneratedKeys()) {
                    for (T entity : newEntities) {
                        if (generatedKeys.next()) {
                            ID id = (ID) generatedKeys.getObject(1);
                            setId(entity, id);
                            System.out.println(id);
                        }
                    }
                }
            } catch (SQLException e) {
                throw new RuntimeException("Error during batch insert");
            }
        }
        if (!updateEntities.isEmpty()) {
            try {
                processBatch(updateEntities, updatePreparedStatement, false);
            } catch (SQLException e) {
                throw new RuntimeException("Error during batch update");
            }
        }
        return entities;
    }

    private void processBatch(Collection<T> entities, PreparedStatement preparedStatement, boolean isInsert) throws SQLException {
        for (T entity : entities) {
            if (isInsert && isNew(entity)) {
                setId(entity, generator.generate());
            }
            int index = setEntityParameters(preparedStatement, entity, !isInsert);
            if (!isInsert) {
                try {
                    preparedStatement.setObject(index, getters.get(idField).invoke(entity));
                } catch (IllegalAccessException | InvocationTargetException e) {
                    throw new RuntimeException("Error with getter for field "
                            + idField.getName() + " in getId");
                }
            }
            preparedStatement.addBatch();
        }
        preparedStatement.executeBatch();
    }

    private int setEntityParameters(PreparedStatement preparedStatement, T entity, boolean skipId) {
        int index = 1;
        List<Field> fieldsToUse = skipId ? updateFields : allColumnFields;
        for (Field field : fieldsToUse) {
            try {
                Object value = getters.get(field).invoke(entity);
                preparedStatement.setObject(index++, value);
            } catch (IllegalAccessException | InvocationTargetException | SQLException e) {
                throw new RuntimeException("Error with setting entity parameter for field "
                        + field.getName() + " in setEntityParameters");
            }
        }
        return index;
    }
}