package ru.inno.adeliya.jdbc.repository;

import ru.inno.adeliya.jdbc.entity.EmployeeEntity;

import java.sql.Connection;

public class EmployeeRepository extends AbstractRepository<EmployeeEntity> {

    public EmployeeRepository(Connection connection) {
        super(connection);
    }

    @Override
    public boolean isNew(EmployeeEntity entity) {
        return entity.getId() == 0;
    }

    @Override
    public void setId(EmployeeEntity entity, int id) {
        entity.setId(id);
    }
}
