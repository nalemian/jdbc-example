package ru.inno.adeliya.jdbc;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ru.inno.adeliya.jdbc.config.ConnectionProvider;
import ru.inno.adeliya.jdbc.config.DirectConnectionProvider;
import ru.inno.adeliya.jdbc.entity.DepartmentEntity;
import ru.inno.adeliya.jdbc.entity.EmployeeEntity;
import ru.inno.adeliya.jdbc.entity.OrganizationEntity;
import ru.inno.adeliya.jdbc.repository.DepartmentRepository;
import ru.inno.adeliya.jdbc.repository.EmployeeRepository;
import ru.inno.adeliya.jdbc.repository.OrganizationRepository;
import ru.inno.adeliya.jdbc.repository.generator.IdGenerator;
import ru.inno.adeliya.jdbc.repository.generator.SingleThreadIntegerGenerator;

import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class ConnectionExampleTest {

    private ConnectionProvider connectionProvider;
    private IdGenerator<Integer> departmentIdGenerator;
    private IdGenerator<Integer> employeeIdGenerator;
    private IdGenerator<Integer> organizationIdGenerator;

    @BeforeEach
    void setUp() {
        this.connectionProvider = new DirectConnectionProvider(
                "jdbc:postgresql://localhost:5432/postgres", "user", "password");
    }

    @Test
    void printAllTableValues() throws SQLException {
        var connection = connectionProvider.getConnection();
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
        var connection = connectionProvider.getConnection();
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
        DepartmentRepository departmentRepository = new DepartmentRepository(connectionProvider, departmentIdGenerator);
        EmployeeRepository employeeRepository = new EmployeeRepository(connectionProvider, employeeIdGenerator);
        OrganizationRepository organizationRepository = new OrganizationRepository(connectionProvider, organizationIdGenerator);
        OrganizationEntity organization = new OrganizationEntity(0, "ООО ООО", 123);
        organization = organizationRepository.save(organization);
        assertNotNull(organization);
        DepartmentEntity department = new DepartmentEntity(0, organization.getId(), "новый отдел");
        department = departmentRepository.save(department);
        assertNotNull(department);
        EmployeeEntity employee = new EmployeeEntity(0, "Орландо Блум", 600000, department.getId());
        employee = employeeRepository.save(employee);
        assertNotNull(employee);
        assertEquals(employee.getSalary(), 600000);
        employee.setSalary(700000);
        employeeRepository.save(employee);
        assertEquals(employee.getSalary(), 700000);
    }
}