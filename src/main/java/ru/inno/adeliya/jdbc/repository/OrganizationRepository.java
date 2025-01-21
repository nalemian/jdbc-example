package ru.inno.adeliya.jdbc.repository;

import ru.inno.adeliya.jdbc.entity.Column;
import ru.inno.adeliya.jdbc.entity.DepartmentEntity;
import ru.inno.adeliya.jdbc.entity.OrganizationEntity;
import ru.inno.adeliya.jdbc.entity.Table;

import java.lang.reflect.Field;
import java.sql.*;

public class OrganizationRepository extends AbstractRepository<OrganizationEntity> {
    public OrganizationRepository(Connection connection) {
        super(connection);
    }

    @Override
    public String getInsertQuery(OrganizationEntity entity) {
        StringBuilder columns = new StringBuilder();
        StringBuilder values = new StringBuilder();
        Field[] fields = OrganizationEntity.class.getDeclaredFields();
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
                OrganizationEntity.class.getAnnotation(Table.class).name(),
                columns,
                values
        );
    }

    @Override
    public String getUpdateQuery(OrganizationEntity entity) {
        StringBuilder newValues = new StringBuilder();
        String condition = null;
        Field[] fields = OrganizationEntity.class.getDeclaredFields();
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
                OrganizationEntity.class.getAnnotation(Table.class).name(),
                newValues,
                condition
        );
    }

    @Override
    public String getSelectQuery(int id) {
        return String.format(
                "SELECT * FROM %s WHERE id = %d;",
                OrganizationEntity.class.getAnnotation(Table.class).name(),
                id
        );
    }

    @Override
    public String getDeleteQuery(int id) {
        return String.format(
                "DELETE FROM %s WHERE id = %d;",
                OrganizationEntity.class.getAnnotation(Table.class).name(),
                id
        );
    }
    @Override
    public OrganizationEntity readResultSet(ResultSet resultSet) throws SQLException {
        return new OrganizationEntity(
                resultSet.getInt("id"),
                resultSet.getString("name"),
                resultSet.getInt("tax_number")
        );
    }

    @Override
    public boolean isNew(OrganizationEntity entity) {
        return entity.getId() == 0;
    }

    @Override
    public void setId(OrganizationEntity entity, int id) {
        entity.setId(id);
    }
}
