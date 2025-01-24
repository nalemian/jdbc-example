package ru.inno.adeliya.jdbc.config;

import java.sql.Connection;

public interface ConnectionProvider {

    Connection getConnection();
}
