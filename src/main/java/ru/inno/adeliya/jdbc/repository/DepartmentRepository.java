package ru.inno.adeliya.jdbc.repository;

import ru.inno.adeliya.jdbc.entity.Column;
import ru.inno.adeliya.jdbc.entity.DepartmentEntity;
import ru.inno.adeliya.jdbc.entity.Table;

import java.lang.reflect.Field;
import java.sql.*;

public class DepartmentRepository extends AbstractRepository<DepartmentEntity> {

    public DepartmentRepository(Connection connection) {
        super(connection);
    }

    @Override
    public String getInsertQuery(DepartmentEntity entity) {
        StringBuilder columns = new StringBuilder();
        StringBuilder values = new StringBuilder();
        Field[] fields = DepartmentEntity.class.getDeclaredFields();
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
                DepartmentEntity.class.getAnnotation(Table.class).name(),
                columns,
                values
        );
    }

    @Override
    public String getUpdateQuery(DepartmentEntity entity) {
        StringBuilder newValues = new StringBuilder();
        String condition = null;
        Field[] fields = DepartmentEntity.class.getDeclaredFields();
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
                DepartmentEntity.class.getAnnotation(Table.class).name(),
                newValues,
                condition
        );
    }

    @Override
    public String getSelectQuery(int id) {
        return String.format(
                "SELECT * FROM %s WHERE id = %d;",
                DepartmentEntity.class.getAnnotation(Table.class).name(),
                id
        );
    }

    @Override
    public String getDeleteQuery(int id) {
        return String.format(
                "DELETE FROM %s WHERE id = %d;",
                DepartmentEntity.class.getAnnotation(Table.class).name(),
                id
        );
    }

    @Override
    public DepartmentEntity readResultSet(ResultSet resultSet) throws SQLException {
        return new DepartmentEntity(
                resultSet.getInt("id"),
                resultSet.getInt("organization"),
                resultSet.getString("name")
        );
    }

    @Override
    public boolean isNew(DepartmentEntity entity) {
        return entity.getId() == 0;
    }

    @Override
    public void setId(DepartmentEntity entity, int id) {
        entity.setId(id);
    }
}
