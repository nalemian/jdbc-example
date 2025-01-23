package ru.inno.adeliya.jdbc.repository;

import ru.inno.adeliya.jdbc.config.ConnectionProvider;
import ru.inno.adeliya.jdbc.entity.DepartmentEntity;

public class DepartmentRepository extends AbstractRepository<DepartmentEntity> {

    public DepartmentRepository(ConnectionProvider connectionProvider) {
        super(connectionProvider);
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
