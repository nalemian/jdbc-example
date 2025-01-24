package ru.inno.adeliya.jdbc.config;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DirectConnectionProvider implements ConnectionProvider {

    private final String url;
    private final String user;
    private final String password;

    public DirectConnectionProvider(String url, String user, String password) {
        this.url = url;
        this.user = user;
        this.password = password;
    }

    @Override
    public Connection getConnection() {
        try {
            return DriverManager.getConnection(url, user, password);
        } catch (SQLException throwables) {
            throw new RuntimeException(throwables);
        }
    }
}
