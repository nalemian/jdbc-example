package ru.inno.adeliya.jdbc.repository;

import ru.inno.adeliya.jdbc.entity.EmployeeEntity;

import java.sql.*;

public class EmployeeRepository extends AbstractRepository<EmployeeEntity> {

    public EmployeeRepository(Connection connection) {
        super(connection);
    }

    @Override
    public String getInsertQuery(EmployeeEntity entity) {
        return String.format(
                "INSERT INTO employee (name, salary, department) VALUES ('%s', %d, %d) RETURNING id;",
                entity.getName(), entity.getSalary(), entity.getDepartment()
        );
    }

    @Override
    public String getUpdateQuery(EmployeeEntity entity) {
        return String.format(
                "UPDATE employee SET name = '%s', salary = %d, department = %d WHERE id = %d;",
                entity.getName(), entity.getSalary(), entity.getDepartment(), entity.getId()
        );
    }
    @Override
    public String getSelectQuery(int id) {
        return String.format("SELECT id, name, salary, department FROM employee WHERE id = %d;", id);
    }

    @Override
    public String getDeleteQuery(int id) {
        return String.format("DELETE FROM employee WHERE id = %d;", id);
    }

    @Override
    public EmployeeEntity readResultSet(ResultSet resultSet) throws SQLException {
        return new EmployeeEntity(
                resultSet.getInt("id"),
                resultSet.getString("name"),
                resultSet.getInt("salary"),
                resultSet.getInt("department")
        );
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
