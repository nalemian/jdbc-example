package ru.inno.adeliya.jdbc.repository;

import ru.inno.adeliya.jdbc.config.ConnectionProvider;
import ru.inno.adeliya.jdbc.entity.Column;
import ru.inno.adeliya.jdbc.entity.Table;
import ru.inno.adeliya.jdbc.repository.generator.IdGenerator;

import java.lang.reflect.*;
import java.sql.*;
import java.util.*;
import java.util.stream.Collectors;

public abstract class AbstractRepository<T, ID> implements EntityRepository<T, ID> {

    private final ConnectionProvider connectionProvider;
    private final String tableName;
    private final Class<T> entityClass;
    private final IdGenerator<ID> generator;
    private final Map<Field, Method> getters;

    public AbstractRepository(ConnectionProvider connectionProvider, IdGenerator<ID> generator) {
        this.connectionProvider = connectionProvider;
        this.generator = generator;
        this.entityClass = (Class<T>) ((ParameterizedType) getClass()
                .getGenericSuperclass()).getActualTypeArguments()[0];
        this.tableName = entityClass.getAnnotation(Table.class).name();
        this.getters = new HashMap<>();
        initializeGetters();
    }

    private void initializeGetters() {
        for (Field field : entityClass.getDeclaredFields()) {
            if (field.isAnnotationPresent(Column.class)) {
                String fieldName = field.getName();
                String getterName = "get" + Character.toUpperCase(fieldName.charAt(0)) + fieldName.substring(1);
                try {
                    Method getter = entityClass.getMethod(getterName);
                    getters.put(field, getter);
                } catch (NoSuchMethodException e) {
                    e.printStackTrace();
                }
            }
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
                "SELECT * FROM %s WHERE id = %d;",
                tableName,
                id
        );
    }

    public String getDeleteQuery(ID id) {
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
                return readResultSet(resultSet);
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

    public Collection<T> saveAll(Collection<T> entities) throws SQLException {
        if (entities.isEmpty()) {
            return Collections.emptyList();
        }
        Field[] fields = entityClass.getDeclaredFields();
        List<String> columnNames = new ArrayList<>();
        for (Field field : fields) {
            if (field.isAnnotationPresent(Column.class)) {
                columnNames.add(field.getAnnotation(Column.class).name());
            }
        }
        String columnNamesStr = String.join(", ", columnNames);
        String placeholders = String.join(", ", Collections.nCopies(columnNames.size(), "?"));
        String sql = String.format("INSERT INTO %s (%s) VALUES (%s) RETURNING id;", tableName, columnNamesStr, placeholders);
        Connection connection = connectionProvider.getConnection();
        PreparedStatement preparedStatement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
        try {
            for (T entity : entities) {
                int index = 1;
                if (isNew(entity)) {
                    setId(entity, generator.generate());
                }
                for (Field field : fields) {
                    if (field.isAnnotationPresent(Column.class)) {
                        Method getter = getters.get(field);
                        try {
                            Object value = getter.invoke(entity);
                            preparedStatement.setObject(index++, value);
                        } catch (IllegalAccessException | InvocationTargetException e) {
                            e.printStackTrace();
                        }
                    }
                }
                preparedStatement.addBatch();
            }
            preparedStatement.executeBatch();
            try (ResultSet generatedKeys = preparedStatement.getGeneratedKeys()) {
                for (T entity : entities) {
                    if (generatedKeys.next()) {
                        int id = generatedKeys.getInt(1);
                        setId(entity, (ID) Integer.valueOf(id));
                        System.out.println(id);
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return entities;
    }
}