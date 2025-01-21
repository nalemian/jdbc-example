package ru.inno.adeliya.jdbc.entity;

/**
 * Сущность таблицы department
 */

@Table(name = "department")
public class DepartmentEntity {
    @Column(name = "id")
    private int id;
    @Column(name = "organization")
    private int organization;
    @Column(name = "name")
    private String name;

    public DepartmentEntity(int id, int organization, String name) {
        this.id = id;
        this.organization = organization;
        this.name = name;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
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
