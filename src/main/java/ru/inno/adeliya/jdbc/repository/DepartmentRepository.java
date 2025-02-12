package ru.inno.adeliya.jdbc.repository;

import ru.inno.adeliya.jdbc.config.ConnectionProvider;
import ru.inno.adeliya.jdbc.entity.DepartmentEntity;
import ru.inno.adeliya.jdbc.repository.generator.IdGenerator;

public class DepartmentRepository extends AbstractRepository<DepartmentEntity, Integer> {
    private final IdGenerator<Integer> generator;

    public DepartmentRepository(ConnectionProvider connectionProvider, IdGenerator<Integer> generator) {
        super(connectionProvider, generator);
        this.generator = generator;
    }

    @Override
    public boolean isNew(DepartmentEntity entity) {
        return entity.getId() == null;
    }

    @Override
    public void setId(DepartmentEntity entity, Integer id) {
        entity.setId(id);
    }
}
