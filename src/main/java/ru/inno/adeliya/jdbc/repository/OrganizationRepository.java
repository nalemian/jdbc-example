package ru.inno.adeliya.jdbc.repository;

import ru.inno.adeliya.jdbc.entity.OrganizationEntity;

import java.sql.Connection;

public class OrganizationRepository extends AbstractRepository<OrganizationEntity> {
    public OrganizationRepository(Connection connection) {
        super(connection);
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
