package ru.inno.adeliya.jdbc.repository;

import ru.inno.adeliya.jdbc.entity.DepartmentEntity;

import java.sql.SQLException;

/**
 * класс, инкапсулирующий работу с sql-логикой таблицы department. Умеет только CRUD операции
 */
public interface DepartmentRepository {
    /**
     * создание (если id пустой) или обновление (id заполнен)
     * @param input
     * @return
     */
    DepartmentEntity save(DepartmentEntity input) throws SQLException;
    DepartmentEntity read(int id) throws SQLException;
    void delete(int id) throws SQLException;
}
