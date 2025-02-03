package ru.inno.adeliya.jdbc;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import ru.inno.adeliya.jdbc.entity.DepartmentEntity;
import ru.inno.adeliya.jdbc.entity.EmployeeEntity;
import ru.inno.adeliya.jdbc.entity.OrganizationEntity;
import ru.inno.adeliya.jdbc.repository.DepartmentRepository;
import ru.inno.adeliya.jdbc.repository.EmployeeRepository;
import ru.inno.adeliya.jdbc.repository.OrganizationRepository;

import java.sql.*;

public class TestcontainersIntegrationTest {

    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>(
            "postgres:16-alpine"
    ).withLogConsumer(outputFrame -> System.out.println(outputFrame.getUtf8String())).waitingFor(Wait.forListeningPort()).withExposedPorts(5432);

    @BeforeAll
    static void beforeAll() {
        postgres.start();
        System.out.println("Mapped port: " + postgres.getMappedPort(5432));
        System.out.println("URL: " + postgres.getJdbcUrl());
        System.out.println("Username: " + postgres.getUsername());
        System.out.println("Password: " + postgres.getPassword());
    }

    @AfterAll
    static void afterAll() {
        System.out.println(postgres.getLogs());
        postgres.stop();
    }

    @BeforeEach
    void setUp() {
        Flyway flyway = Flyway.configure()
                .dataSource(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword())
                .load();
        flyway.migrate();
    }

    @Test
    void insertALotOfData() {
        try (
                Connection connection = DriverManager.getConnection(
                        postgres.getJdbcUrl(),
                        postgres.getUsername(),
                        postgres.getPassword()
                )) {
            System.out.println(1);
            connection.setAutoCommit(false);
            OrganizationRepository organizationRepository = new OrganizationRepository(() -> connection);
            DepartmentRepository departmentRepository = new DepartmentRepository(() -> connection);
            EmployeeRepository employeeRepository = new EmployeeRepository(() -> connection);

            for (int i = 1; i <= 5; i++) {
                OrganizationEntity org = new OrganizationEntity(i + 1, "Организация " + i, 124 + i);
                org = organizationRepository.save(org);
                System.out.println(1);
                for (int j = 1; j <= 10; j++) {
                    DepartmentEntity dept = new DepartmentEntity(j + 1, org.getId(), "Отдел " + j);
                    dept = departmentRepository.save(dept);
                    for (int k = 1; k <= 100; k++) {
                        EmployeeEntity emp = new EmployeeEntity(k + 1, "Сотрудник " + k, 10000 + (k * 10), dept.getId());
                        employeeRepository.save(emp);
                    }
                }
            }
            connection.commit();
            try (Statement statement = connection.createStatement();
                 ResultSet resultSet = statement.executeQuery("SELECT COUNT(*) FROM organization")) {
                if (resultSet.next()) {
                    System.out.println(resultSet.getInt(1));
                }
            }
            try (Statement statement = connection.createStatement();
                 ResultSet resultSet = statement.executeQuery("SELECT COUNT(*) FROM department")) {
                if (resultSet.next()) {
                    System.out.println(resultSet.getInt(1));
                }
            }
            try (Statement statement = connection.createStatement();
                 ResultSet resultSet = statement.executeQuery("SELECT COUNT(*) FROM employee")) {
                if (resultSet.next()) {
                    System.out.println(resultSet.getInt(1));
                }
            }
            try (Statement statement = connection.createStatement();
                 ResultSet resultSet = statement.executeQuery("SELECT * FROM employee")) {
                while (resultSet.next()) {
                    System.out.println(
                            "ID: " + resultSet.getInt("id") +
                                    ", name: " + resultSet.getString("name") +
                                    ", salary: " + resultSet.getInt("salary") +
                                    ", department: " + resultSet.getInt("department")
                    );
                }
            } catch (SQLException e) {
                try {
                    connection.rollback();
                } catch (SQLException rollbackEx) {
                    rollbackEx.printStackTrace();
                }
                e.printStackTrace();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }


    @Test
    void name() {
        System.out.println(1);
    }
}
