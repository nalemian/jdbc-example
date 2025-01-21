package ru.inno.adeliya.jdbc.entity;

@Table(name = "organization")
public class OrganizationEntity {
    @Column(name = "id")
    private int id;
    @Column(name = "name")
    private String name;
    @Column(name = "tax_number")
    private int tax_number;

    public OrganizationEntity(int id, String name, int tax_number) {
        this.id = id;
        this.name = name;
        this.tax_number = tax_number;
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

    public int getTax_number() {
        return tax_number;
    }

    public void setTax_number(int tax_number) {
        this.tax_number = tax_number;
    }
}
