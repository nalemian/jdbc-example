package ru.inno.adeliya.jdbc.repository;

import ru.inno.adeliya.jdbc.config.ConnectionProvider;
import ru.inno.adeliya.jdbc.entity.EmployeeEntity;

public class EmployeeRepository extends AbstractRepository<EmployeeEntity> {

    public EmployeeRepository(ConnectionProvider connectionProvider) {
        super(connectionProvider);
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
