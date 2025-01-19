package ru.inno.adeliya.jdbc.repository;

import ru.inno.adeliya.jdbc.entity.OrganizationEntity;

import java.sql.*;

public class OrganizationRepository extends AbstractRepository<OrganizationEntity> {
    public OrganizationRepository(Connection connection) {
        super(connection);
    }

    @Override
    public String getInsertQuery(OrganizationEntity entity) {
        return String.format(
                "INSERT INTO organization (name, tax_number) VALUES ('%s', %d) RETURNING id;",
                entity.getName(), entity.getTax_number()
        );
    }

    @Override
    public String getUpdateQuery(OrganizationEntity entity) {
        return String.format(
                "UPDATE organization SET name = '%s', tax_number = %d WHERE id = %d;",
                entity.getName(), entity.getTax_number(), entity.getId()
        );
    }

    @Override
    public String getSelectQuery(int id) {
        return String.format("SELECT id, name, tax_number FROM organization WHERE id = %d;", id);
    }

    @Override
    public String getDeleteQuery(int id) {
        return String.format("DELETE FROM organization WHERE id = %d;", id);
    }
    @Override
    public OrganizationEntity readResultSet(ResultSet resultSet) throws SQLException {
        return new OrganizationEntity(
                resultSet.getInt("id"),
                resultSet.getString("name"),
                resultSet.getInt("tax_number")
        );
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
