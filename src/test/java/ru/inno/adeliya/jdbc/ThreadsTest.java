package ru.inno.adeliya.jdbc;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
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
import ru.inno.adeliya.jdbc.repository.generator.SynchronizedIntegerGenerator;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class ThreadsTest {
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>(
            "postgres:16-alpine"
    ).waitingFor(Wait.forListeningPort()).withExposedPorts(5432);

    @BeforeAll
    static void beforeAll() {
        postgres.start();
        Flyway flyway = Flyway.configure()
                .dataSource(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword())
                .load();
        flyway.migrate();
    }

    @AfterAll
    static void afterAll() {
        postgres.stop();
    }

    @Test
    void atomicIdGeneratorTest() throws InterruptedException {
        AtomicInteger organizationIdCounter = new AtomicInteger(1);
        AtomicInteger departmentIdCounter = new AtomicInteger(1);
        AtomicInteger employeeIdCounter = new AtomicInteger(1);
        IdGenerator<Integer> organizationIdGenerator = organizationIdCounter::getAndIncrement;
        IdGenerator<Integer> departmentIdGenerator = departmentIdCounter::getAndIncrement;
        IdGenerator<Integer> employeeIdGenerator = employeeIdCounter::getAndIncrement;
        executeTest(organizationIdGenerator, departmentIdGenerator, employeeIdGenerator, 50);
    }

    /**
     * it demonstrates failure of single thread id generator in multithreading
     */
    @Disabled
    @Test
    void singleThreadIdGeneratorTest() throws InterruptedException {
        IdGenerator<Integer> organizationIdGenerator = new SingleThreadIntegerGenerator();
        IdGenerator<Integer> departmentIdGenerator = new SingleThreadIntegerGenerator();
        IdGenerator<Integer> employeeIdGenerator = new SingleThreadIntegerGenerator();
        executeTest(organizationIdGenerator, departmentIdGenerator, employeeIdGenerator, 50);
    }

    @Test
    void synchronizedGeneratorTest() throws InterruptedException {
        IdGenerator<Integer> organizationIdGenerator = new SynchronizedIntegerGenerator();
        IdGenerator<Integer> departmentIdGenerator = new SynchronizedIntegerGenerator();
        IdGenerator<Integer> employeeIdGenerator = new SynchronizedIntegerGenerator();
        executeTest(organizationIdGenerator, departmentIdGenerator, employeeIdGenerator, 50);
    }

    private void executeTest(
            IdGenerator<Integer> organizationIdGenerator,
            IdGenerator<Integer> departmentIdGenerator,
            IdGenerator<Integer> employeeIdGenerator,
            int numberOfOrgs
    ) throws InterruptedException {
        ExecutorService executorService = Executors.newFixedThreadPool(5);
        for (int i = 1; i <= numberOfOrgs; i++) {
            int finalI = i;
            executorService.submit(() -> {
                try (Connection connection = DriverManager.getConnection(
                        postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword()
                )) {
                    connection.setAutoCommit(false);
                    OrganizationRepository organizationRepository = new OrganizationRepository(() -> connection, organizationIdGenerator);
                    DepartmentRepository departmentRepository = new DepartmentRepository(() -> connection, departmentIdGenerator);
                    EmployeeRepository employeeRepository = new EmployeeRepository(() -> connection, employeeIdGenerator);
                    insertOneOrganization(finalI, organizationRepository, departmentRepository, employeeRepository);
                    connection.commit();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            });
        }
        executorService.shutdown();
        executorService.awaitTermination(1, TimeUnit.MINUTES);
        try (Connection connection = DriverManager.getConnection(
                postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword()
        )) {
            OrganizationRepository organizationRepository = new OrganizationRepository(() -> connection, () -> 0);
            DepartmentRepository departmentRepository = new DepartmentRepository(() -> connection, () -> 0);
            EmployeeRepository employeeRepository = new EmployeeRepository(() -> connection, () -> 0);
            assertEquals(numberOfOrgs, organizationRepository.count());
            assertEquals(numberOfOrgs * 10, departmentRepository.count());
            assertEquals(numberOfOrgs * 1000, employeeRepository.count());
            try (var statement = connection.createStatement()) {
                statement.execute("TRUNCATE TABLE employee, department, organization RESTART IDENTITY CASCADE");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void insertOneOrganization(int orgIndex, OrganizationRepository organizationRepository, DepartmentRepository departmentRepository, EmployeeRepository employeeRepository) throws SQLException {
        OrganizationEntity org = new OrganizationEntity(null, "Организация " + orgIndex, 124 + orgIndex);
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
}
