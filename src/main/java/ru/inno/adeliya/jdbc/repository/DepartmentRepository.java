package ru.inno.adeliya.jdbc.repository;

import ru.inno.adeliya.jdbc.entity.DepartmentEntity;

import java.sql.*;

public class DepartmentRepository extends AbstractRepository<DepartmentEntity> {

    public DepartmentRepository(Connection connection) {
        super(connection);
    }

    @Override
    public String getInsertQuery(DepartmentEntity entity) {
        return String.format(
                "INSERT INTO department (organization, name) VALUES (%d, '%s') RETURNING id;",
                entity.getOrganization(), entity.getName()
        );
    }

    @Override
    public String getUpdateQuery(DepartmentEntity entity) {
        return String.format(
                "UPDATE department SET organization = %d, name = '%s' WHERE id = %d;",
                entity.getOrganization(), entity.getName(), entity.getId()
        );
    }

    @Override
    public String getSelectQuery(int id) {
        return String.format("SELECT id, organization, name FROM department WHERE id = %d;", id);
    }

    @Override
    public String getDeleteQuery(int id) {
        return String.format("DELETE FROM department WHERE id = %d;", id);
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
