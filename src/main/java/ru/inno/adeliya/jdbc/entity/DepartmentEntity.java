package ru.inno.adeliya.jdbc.entity;

import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Сущность таблицы department
 */

@Table(name = "department")
public class DepartmentEntity {
    @Id
    @Column(name = "id")
    private Integer id;
    @Column(name = "organization")
    private int organization;
    @Column(name = "name")
    private String name;

    public DepartmentEntity(Integer id, int organization, String name) {
        this.id = id;
        this.organization = organization;
        this.name = name;
    }

    public DepartmentEntity(ResultSet resultSet) throws SQLException {
        this.id = resultSet.getInt("id");
        if (resultSet.wasNull()) {
            this.id = null;
        }
        this.organization = resultSet.getInt("organization");
        this.name = resultSet.getString("name");
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public int getOrganization() {
        return organization;
    }

    public void setOrganization(int organization) {
        this.organization = organization;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
