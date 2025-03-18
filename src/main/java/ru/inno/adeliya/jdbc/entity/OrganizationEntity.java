package ru.inno.adeliya.jdbc.entity;

import java.sql.ResultSet;
import java.sql.SQLException;

@Table(name = "organization")
public class OrganizationEntity {
    @Id
    @Column(name = "id")
    private Integer id;
    @Column(name = "name")
    private String name;
    @Column(name = "tax_number")
    private int tax_number;

    public OrganizationEntity(Integer id, String name, int tax_number) {
        this.id = id;
        this.name = name;
        this.tax_number = tax_number;
    }

    public OrganizationEntity(ResultSet resultSet) throws SQLException {
        this.id = resultSet.getInt("id");
        if (resultSet.wasNull()) {
            this.id = null;
        }
        this.name = resultSet.getString("name");
        this.tax_number = resultSet.getInt("tax_number");
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getTax_number() {
        return tax_number;
    }

    public void setTax_number(int tax_number) {
        this.tax_number = tax_number;
    }
}
