package ru.inno.adeliya.jdbc;

import org.junit.jupiter.api.Test;
import ru.inno.adeliya.jdbc.entity.DepartmentEntity;
import ru.inno.adeliya.jdbc.entity.EmployeeEntity;
import ru.inno.adeliya.jdbc.entity.OrganizationEntity;
import ru.inno.adeliya.jdbc.repository.DepartmentRepository;
import ru.inno.adeliya.jdbc.repository.EmployeeRepository;
import ru.inno.adeliya.jdbc.repository.OrganizationRepository;

import java.sql.DriverManager;
import java.sql.SQLException;

class ConnectionExampleTest {
    @Test
    void printAllTableValues() throws SQLException {
        var connection = DriverManager.getConnection("jdbc:postgresql://localhost:5432/postgres", "user", "password");
        String select = "SELECT * from \"adeliya-learn\".department";
        var result = connection.createStatement().executeQuery(select);
        while (result.next()) {
            System.out.println(String.format("%s %s %s",
                    result.getString(1),
                    result.getString(2),
                    result.getString(3)));
        }
    }

    @Test
    void insertNewValuesIntoTable() throws SQLException {
        var connection = DriverManager.getConnection("jdbc:postgresql://localhost:5432/postgres", "user", "password");
        String insert = "INSERT INTO \"adeliya-learn\".department (id, organization, name)\n" +
                "VALUES (?, ?, ?)\n" +
                "ON CONFLICT (id) DO UPDATE SET\n" +
                "    organization = EXCLUDED.organization,\n" +
                "    name = EXCLUDED.name\n" +
                "RETURNING id;";
        try (var preparedStatement = connection.prepareStatement(insert)) {
            preparedStatement.setInt(1, 16);
            preparedStatement.setInt(2, 3);
            preparedStatement.setString(3, "Отдел отделов");
            try (var result = preparedStatement.executeQuery()) {
                if (result.next()) {
                    int insertedId = result.getInt("id");
                    System.out.println("inserted id: " + insertedId);
                    String select = "SELECT * FROM \"adeliya-learn\".department WHERE id = ?";
                    try (var selectionStatement = connection.prepareStatement(select)) {
                        selectionStatement.setInt(1, insertedId);
                        try (var selectResult = selectionStatement.executeQuery()) {
                            while (selectResult.next()) {
                                System.out.println(String.format("%s %s %s",
                                        selectResult.getInt("id"),
                                        selectResult.getString("organization"),
                                        selectResult.getString("name")));
                            }
                        }
                    }
                }
            }
        }
    }

    @Test
    void testRepositories() throws SQLException {
        var connection = DriverManager.getConnection("jdbc:postgresql://localhost:5432/postgres", "user", "password");
        DepartmentRepository departmentRepository = new DepartmentRepository(connection);
        EmployeeRepository employeeRepository = new EmployeeRepository(connection);
        OrganizationRepository organizationRepository = new OrganizationRepository(connection);
        OrganizationEntity organization = new OrganizationEntity(0, "ООО ООО", 123);
        organization = organizationRepository.save(organization);
        System.out.println(organization);
        DepartmentEntity department = new DepartmentEntity(0, organization.getId(), "новый отдел");
        department = departmentRepository.save(department);
        System.out.println(department);
        EmployeeEntity employee = new EmployeeEntity(0, "Орландо Блум", 600000, department.getId());
        employee = employeeRepository.save(employee);
        System.out.println(employee);
        System.out.println(organizationRepository.read(organization.getId()));
        System.out.println(departmentRepository.read(department.getId()));
        System.out.println(employeeRepository.read(employee.getId()));
        employee.setSalary(700000);
        employeeRepository.save(employee);
        System.out.println(employee);
        employeeRepository.delete(employee.getId());
        departmentRepository.delete(department.getId());
        organizationRepository.delete(organization.getId());
    }

}