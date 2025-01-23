package ru.inno.adeliya.jdbc.entity;

import java.sql.ResultSet;
import java.sql.SQLException;

@Table(name = "employee")
public class EmployeeEntity {
    @Column(name = "id")
    private int id;
    @Column(name = "name")
    private String name;
    @Column(name = "salary")
    private int salary;
    @Column(name = "department")
    private int department;

    public EmployeeEntity(int id, String name, int salary, int department) {
        this.id = id;
        this.name = name;
        this.salary = salary;
        this.department = department;
    }

    public EmployeeEntity(ResultSet resultSet) throws SQLException {
        this.id = resultSet.getInt("id");
        this.name = resultSet.getString("name");
        this.salary = resultSet.getInt("salary");
        this.department = resultSet.getInt("department");
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getSalary() {
        return salary;
    }

    public void setSalary(int salary) {
        this.salary = salary;
    }

    public int getDepartment() {
        return department;
    }

    public void setDepartment(int department) {
        this.department = department;
    }
}
