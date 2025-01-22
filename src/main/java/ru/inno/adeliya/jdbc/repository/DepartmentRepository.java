package ru.inno.adeliya.jdbc.repository;

import ru.inno.adeliya.jdbc.entity.DepartmentEntity;

import java.sql.Connection;

public class DepartmentRepository extends AbstractRepository<DepartmentEntity> {

    public DepartmentRepository(Connection connection) {
        super(connection);
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
