package ru.inno.adeliya.jdbc.repository;

import ru.inno.adeliya.jdbc.config.ConnectionProvider;
import ru.inno.adeliya.jdbc.entity.OrganizationEntity;

public class OrganizationRepository extends AbstractRepository<OrganizationEntity> {

    public OrganizationRepository(ConnectionProvider connectionProvider) {
        super(connectionProvider);
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
