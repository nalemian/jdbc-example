package ru.inno.adeliya.jdbc.repository;

import java.sql.SQLException;

/**
 * класс, инкапсулирующий работу с sql-логикой таблицы department. Умеет только CRUD операции
 */
public interface EntityRepository<T> {
    /**
     * создание (если id пустой) или обновление (id заполнен)
     * @param input
     * @return
     */
    T save(T input) throws SQLException;
    T read(int id) throws SQLException;
    void delete(int id) throws SQLException;
}
