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
import ru.inno.adeliya.jdbc.repository.generator.IdGenerator;
import ru.inno.adeliya.jdbc.repository.generator.SingleThreadIntegerGenerator;

import java.sql.*;

import static org.junit.jupiter.api.Assertions.assertEquals;

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
                )
        ) {
            connection.setAutoCommit(false);
            IdGenerator<Integer> organizationIdGenerator = new SingleThreadIntegerGenerator();
            IdGenerator<Integer> departmentIdGenerator = new SingleThreadIntegerGenerator();
            IdGenerator<Integer> employeeIdGenerator = new SingleThreadIntegerGenerator();
            OrganizationRepository organizationRepository = new OrganizationRepository(() -> connection, organizationIdGenerator);
            DepartmentRepository departmentRepository = new DepartmentRepository(() -> connection, departmentIdGenerator);
            EmployeeRepository employeeRepository = new EmployeeRepository(() -> connection, employeeIdGenerator);

            for (int i = 1; i <= 5; i++) {
                OrganizationEntity org = new OrganizationEntity(null, "Организация " + i, 124 + i);
                org = organizationRepository.save(org);
                for (int j = 1; j <= 10; j++) {
                    DepartmentEntity dept = new DepartmentEntity(null, org.getId(), "Отдел " + j);
                    dept = departmentRepository.save(dept);
                    for (int k = 1; k <= 100; k++) {
                        EmployeeEntity emp = new EmployeeEntity(null, "Сотрудник " + k, 10000 + (k * 10), dept.getId());
                        employeeRepository.save(emp);
                    }
                }
            }
            connection.commit();
            assertEquals(5, organizationRepository.count());
            assertEquals(50, departmentRepository.count());
            assertEquals(5000, employeeRepository.count());
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
