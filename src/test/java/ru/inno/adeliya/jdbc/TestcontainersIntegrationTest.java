package ru.inno.adeliya.jdbc;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.PostgreSQLContainer;
import ru.inno.adeliya.jdbc.config.ConnectionProvider;
import ru.inno.adeliya.jdbc.config.DirectConnectionProvider;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class TestcontainersIntegrationTest {

    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>(
            "postgres:16-alpine"
    );

    @BeforeAll
    static void beforeAll() {
        postgres.start();
    }

    @AfterAll
    static void afterAll() {
        postgres.stop();
    }

    private ConnectionProvider connectionProvider;

    @BeforeEach
    void setUp() {
        connectionProvider = new DirectConnectionProvider(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword());
        createCustomersTableIfNotExists();
        createEmployeeTableIfNotExists();
        createOrganizationTableIfNotExists();
        createDepartmentTableIfNotExists();
    }

    @Test
    void name() {

        System.out.println(1);
    }

    private void createCustomersTableIfNotExists() {
        try (Connection conn = this.connectionProvider.getConnection()) {
            PreparedStatement pstmt = conn.prepareStatement(
                    """
                            create table if not exists customers (
                                id bigint not null,
                                name varchar not null,
                                primary key (id)
                            )
                            """
            );
            pstmt.execute();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private void createEmployeeTableIfNotExists() {
        try (Connection conn = this.connectionProvider.getConnection()) {
            PreparedStatement pstmt = conn.prepareStatement(
                    """
                            create table if not exists employee (
                                id bigint not null,
                                name varchar,
                                salary bigint,
                                department bigint,
                                primary key (id)
                            )
                            """
            );
            pstmt.execute();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private void createOrganizationTableIfNotExists() {
        try (Connection conn = this.connectionProvider.getConnection()) {
            PreparedStatement pstmt = conn.prepareStatement(
                    """
                            create table if not exists organization (
                                id bigint not null,
                                name varchar,
                                tax_number integer not null,
                                primary key (id)
                            )
                            """
            );
            pstmt.execute();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private void createDepartmentTableIfNotExists() {
        try (Connection conn = this.connectionProvider.getConnection()) {
            PreparedStatement pstmt = conn.prepareStatement(
                    """
                            create table if not exists department (
                                id bigint not null,
                                organization bigint
                                    constraint organization_fk
                                            references organization
                                            on delete restrict,
                                name varchar,
                                primary key (id)
                            )
                            """
            );
            pstmt.execute();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
}
