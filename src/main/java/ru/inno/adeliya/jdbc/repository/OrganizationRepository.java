package ru.inno.adeliya.jdbc.repository;

import ru.inno.adeliya.jdbc.config.ConnectionProvider;
import ru.inno.adeliya.jdbc.entity.OrganizationEntity;
import ru.inno.adeliya.jdbc.repository.generator.IdGenerator;

public class OrganizationRepository extends AbstractRepository<OrganizationEntity, Integer> {
    private final IdGenerator<Integer> generator;


    public OrganizationRepository(ConnectionProvider connectionProvider, IdGenerator<Integer> generator) {
        super(connectionProvider, generator);
        this.generator = generator;
    }

    @Override
    public boolean isNew(OrganizationEntity entity) {
        return entity.getId() == null;
    }

    @Override
    public void setId(OrganizationEntity entity, Integer id) {
        entity.setId(id);
    }
}
