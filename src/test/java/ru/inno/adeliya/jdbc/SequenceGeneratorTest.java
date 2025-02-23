package ru.inno.adeliya.jdbc;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import ru.inno.adeliya.jdbc.repository.generator.IdGenerator;
import ru.inno.adeliya.jdbc.repository.generator.SequenceWithBatchesGenerator;
import ru.inno.adeliya.jdbc.repository.generator.NextValSequenceGenerator;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.Executors;

import static org.junit.jupiter.api.Assertions.assertEquals;

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
            Set<Long> idSet = generateIdsInParallel(generator, threads, threadIds);
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
            Set<Long> idSet = generateIdsInParallel(generator, threads, threadIds);
            assertEquals(threads * threadIds, idSet.size());
        } catch (SQLException | InterruptedException e) {
            e.printStackTrace();
        }
    }
    private Set<Long> generateIdsInParallel(IdGenerator<Long> generator, int threads, int threadIds) throws InterruptedException {
        ExecutorService executor = Executors.newFixedThreadPool(threads);
        Set<Long> idSet = ConcurrentHashMap.newKeySet();
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
}
