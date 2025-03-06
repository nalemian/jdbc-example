package ru.inno.adeliya.jdbc;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
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
import ru.inno.adeliya.jdbc.repository.generator.NextValSequenceGenerator;
import ru.inno.adeliya.jdbc.repository.generator.SequenceWithBatchesGenerator;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class SequenceGeneratorTest {
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .waitingFor(Wait.forListeningPort())
            .withExposedPorts(5432);

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
    void nextValSequenceGeneratorTest() {
        try (Connection connection = DriverManager.getConnection(
                postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword()
        )) {
            NextValSequenceGenerator generator = new NextValSequenceGenerator(connection, "mysequence");
            int threads = 10;
            int threadIds = 50;
            Set<Integer> idSet = generateIdsInParallel(generator, threads, threadIds);
            assertEquals(threads * threadIds, idSet.size());
        } catch (SQLException | InterruptedException e) {
            e.printStackTrace();
        }
    }

    @Test
    void sequenceWithBatchesGeneratorTest() {
        try (Connection connection = DriverManager.getConnection(
                postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword()
        )) {
            SequenceWithBatchesGenerator generator = new SequenceWithBatchesGenerator(connection, 20, "mysequence");
            int threads = 10;
            int threadIds = 50;
            Set<Integer> idSet = generateIdsInParallel(generator, threads, threadIds);
            assertEquals(threads * threadIds, idSet.size());
        } catch (SQLException | InterruptedException e) {
            e.printStackTrace();
        }
    }

    @Test
    void sequenceWithBatchesGeneratorTestWithSaveAll() {
        try (Connection connection = DriverManager.getConnection(
                postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword()
        )) {
            connection.setAutoCommit(false);
            SequenceWithBatchesGenerator organizationIdGenerator = new SequenceWithBatchesGenerator(connection, 20, "mysequence");
            SequenceWithBatchesGenerator departmentIdGenerator = new SequenceWithBatchesGenerator(connection, 20, "mysequence");
            SequenceWithBatchesGenerator employeeIdGenerator = new SequenceWithBatchesGenerator(connection, 20, "mysequence");
            OrganizationRepository organizationRepository = new OrganizationRepository(() -> connection, organizationIdGenerator);
            DepartmentRepository departmentRepository = new DepartmentRepository(() -> connection, departmentIdGenerator);
            EmployeeRepository employeeRepository = new EmployeeRepository(() -> connection, employeeIdGenerator);
            int numberOfOrgs = 1;
            insertOneOrganizationBatch(numberOfOrgs, organizationRepository, departmentRepository, employeeRepository);
            connection.commit();
            assertEquals(numberOfOrgs, organizationRepository.count());
            assertEquals(numberOfOrgs * 10, departmentRepository.count());
            assertEquals(numberOfOrgs * 1000, employeeRepository.count());
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Test
    void saveAllWithExistingEntity() {
        try (Connection connection = DriverManager.getConnection(
                postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword()
        )) {
            connection.setAutoCommit(false);
            SequenceWithBatchesGenerator organizationIdGenerator = new SequenceWithBatchesGenerator(connection, 20, "mysequence");
            SequenceWithBatchesGenerator departmentIdGenerator = new SequenceWithBatchesGenerator(connection, 20, "mysequence");
            SequenceWithBatchesGenerator employeeIdGenerator = new SequenceWithBatchesGenerator(connection, 20, "mysequence");
            OrganizationRepository organizationRepository = new OrganizationRepository(() -> connection, organizationIdGenerator);
            DepartmentRepository departmentRepository = new DepartmentRepository(() -> connection, departmentIdGenerator);
            EmployeeRepository employeeRepository = new EmployeeRepository(() -> connection, employeeIdGenerator);
            int numberOfOrgs = 1;
            insertOneOrganizationBatch(numberOfOrgs, organizationRepository, departmentRepository, employeeRepository);
            OrganizationEntity existingOrg = organizationRepository.read(numberOfOrgs);
            assertNotNull(existingOrg);
            OrganizationEntity updatedOrg = new OrganizationEntity(existingOrg.getId(), "новая организация", existingOrg.getTax_number());
            OrganizationEntity newOrg = new OrganizationEntity(null, "совсем новая организация", 111);
            organizationRepository.saveAll(Arrays.asList(updatedOrg, newOrg));
            connection.commit();
            assertEquals(2, organizationRepository.count());
            assertEquals("новая организация", organizationRepository.read(existingOrg.getId()).getName());
            OrganizationEntity insertedNewOrg = organizationRepository.read(newOrg.getId());
            assertEquals("совсем новая организация", insertedNewOrg.getName());
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private Set<Integer> generateIdsInParallel(IdGenerator<Integer> generator, int threads, int threadIds) throws InterruptedException {
        ExecutorService executor = Executors.newFixedThreadPool(threads);
        Set<Integer> idSet = ConcurrentHashMap.newKeySet();
        for (int i = 0; i < threads; i++) {
            executor.submit(() -> {
                for (int j = 0; j < threadIds; j++) {
                    idSet.add(generator.generate());
                }
            });
        }
        executor.shutdown();
        executor.awaitTermination(1, TimeUnit.MINUTES);
        return idSet;
    }

    private void insertOneOrganizationBatch(int orgIndex, OrganizationRepository organizationRepository, DepartmentRepository departmentRepository, EmployeeRepository employeeRepository) throws SQLException {
        OrganizationEntity org = new OrganizationEntity(null, "Организация " + orgIndex, 124 + orgIndex);
        organizationRepository.save(org);
        List<DepartmentEntity> departments = new ArrayList<>();
        List<EmployeeEntity> employees = new ArrayList<>();
        for (int j = 1; j <= 10; j++) {
            DepartmentEntity dept = new DepartmentEntity(null, org.getId(), "Отдел " + j);
            departments.add(dept);
        }
        departmentRepository.saveAll(departments);
        for (DepartmentEntity dept : departments) {
            for (int k = 1; k <= 100; k++) {
                employees.add(new EmployeeEntity(null, "Сотрудник " + k, 10000 + (k * 10), dept.getId()));
            }
        }
        employeeRepository.saveAll(employees);
    }
}
