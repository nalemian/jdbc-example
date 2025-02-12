package ru.inno.adeliya.jdbc.repository;

import ru.inno.adeliya.jdbc.config.ConnectionProvider;
import ru.inno.adeliya.jdbc.entity.EmployeeEntity;
import ru.inno.adeliya.jdbc.repository.generator.IdGenerator;

public class EmployeeRepository extends AbstractRepository<EmployeeEntity, Integer> {
    private final IdGenerator<Integer> generator;

    public EmployeeRepository(ConnectionProvider connectionProvider, IdGenerator<Integer> generator) {
        super(connectionProvider, generator);
        this.generator = generator;
    }

    @Override
    public boolean isNew(EmployeeEntity entity) {
        return entity.getId() == null;
    }

    @Override
    public void setId(EmployeeEntity entity, Integer id) {
        entity.setId(id);
    }
}
