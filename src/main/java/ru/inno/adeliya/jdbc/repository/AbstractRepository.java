package ru.inno.adeliya.jdbc.repository;

import ru.inno.adeliya.jdbc.config.ConnectionProvider;
import ru.inno.adeliya.jdbc.entity.Column;
import ru.inno.adeliya.jdbc.entity.Id;
import ru.inno.adeliya.jdbc.entity.Table;
import ru.inno.adeliya.jdbc.repository.generator.IdGenerator;

import java.lang.reflect.*;
import java.sql.*;
import java.util.*;

public abstract class AbstractRepository<T, ID> implements EntityRepository<T, ID> {

    private final ConnectionProvider connectionProvider;
    private final String tableName;
    private final IdGenerator<ID> generator;
    private final PreparedStatement insertPreparedStatement;
    private final PreparedStatement updatePreparedStatement;
    private final Constructor<T> resultSetConstructor;
    private List<String> updateColumnNames;
    private Map<Field, Method> getters;
    private List<Field> allColumnFields;
    private List<Field> updateFields;
    private List<String> allColumnNames;
    private Field idField;

    public AbstractRepository(ConnectionProvider connectionProvider, IdGenerator<ID> generator) {
        this.connectionProvider = connectionProvider;
        this.generator = generator;
        Class<T> entityClass = (Class<T>) ((ParameterizedType) getClass()
                .getGenericSuperclass()).getActualTypeArguments()[0];
        this.tableName = entityClass.getAnnotation(Table.class).name();
        initializeReflectionData(entityClass);
        this.insertPreparedStatement = initializeInsertPreparedStatement();
        this.updatePreparedStatement = initializeUpdatePreparedStatement();
        try {
            this.resultSetConstructor = entityClass.getConstructor(ResultSet.class);
        } catch (NoSuchMethodException e) {
            throw new RuntimeException("No constructor with ResultSet parameter in " + entityClass.getName());
        }

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

    @Override
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

    @Override
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

    private String getSelectQuery(ID id) {
        return String.format(
                "SELECT * FROM %s WHERE id = %d;",
                tableName,
                id
        );
    }

    private String getDeleteQuery(ID id) {
        return String.format(
                "DELETE FROM %s WHERE id = %d;",
                tableName,
                id
        );
    }

    private String getInsertQuery(T entity) {
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

    private String getUpdateQuery(T entity) {
        StringBuilder newValues = new StringBuilder();
        String condition = null;
        for (int i = 0; i < allColumnFields.size(); i++) {
            Field field = allColumnFields.get(i);
            String columnName = allColumnNames.get(i);
            try {
                Object value = getters.get(field).invoke(entity);
                if (field.equals(idField)) {
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

    private void initializeReflectionData(Class<T> entityClass) {
        Map<Field, Method> tempGetters = new HashMap<>();
        List<Field> tempAllFields = new ArrayList<>();
        List<String> tempAllColumnNames = new ArrayList<>();
        List<Field> tempUpdateFields = new ArrayList<>();
        List<String> tempUpdateColumnNames = new ArrayList<>();
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
                if (field.isAnnotationPresent(Id.class)) {
                    tempIdField = field;
                } else {
                    tempUpdateFields.add(field);
                    tempUpdateColumnNames.add(columnName);
                }
            }
        }
        if (tempIdField == null) {
            throw new RuntimeException("No field annotated with @Id found in " + entityClass.getName());
        }
        this.getters = tempGetters;
        this.allColumnFields = tempAllFields;
        this.allColumnNames = tempAllColumnNames;
        this.updateFields = tempUpdateFields;
        this.updateColumnNames = tempUpdateColumnNames;
        this.idField = tempIdField;
    }

    private PreparedStatement initializeInsertPreparedStatement() {
        try {
            Connection connection = connectionProvider.getConnection();
            String insertColumns = String.join(", ", allColumnNames);
            String insertPlaceholders = String.join(", ", Collections.nCopies(allColumnNames.size(), "?"));
            String insertQuery = String.format("INSERT INTO %s (%s) VALUES (%s) RETURNING id;", tableName, insertColumns, insertPlaceholders);
            return connection.prepareStatement(insertQuery, Statement.RETURN_GENERATED_KEYS);
        } catch (SQLException e) {
            throw new RuntimeException("Error with creating insert prepared statement");
        }
    }

    private PreparedStatement initializeUpdatePreparedStatement() {
        try {
            Connection connection = connectionProvider.getConnection();
            List<String> columnsForUpdate = new ArrayList<>();
            for (String column : updateColumnNames) {
                columnsForUpdate.add(column + " = ?");
            }
            String updateQuery = String.format("UPDATE %s SET %s WHERE id = ?;", tableName, String.join(", ", columnsForUpdate));
            return connection.prepareStatement(updateQuery, Statement.RETURN_GENERATED_KEYS);
        } catch (SQLException e) {
            throw new RuntimeException("Error with creating update prepared statement");
        }
    }

    abstract boolean isNew(T entity);

    abstract void setId(T entity, ID id);
}