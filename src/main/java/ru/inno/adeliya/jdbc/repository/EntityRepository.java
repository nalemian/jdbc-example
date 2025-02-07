package ru.inno.adeliya.jdbc.repository;

import java.sql.SQLException;

/**
 * класс, инкапсулирующий работу с sql-логикой таблицы department. Умеет только CRUD операции
 */
public interface EntityRepository<T, ID> {
    /**
     * создание (если id пустой) или обновление (id заполнен)
     * @param input
     * @return
     */
    T save(T input) throws SQLException;
    T read(ID id) throws SQLException;
    void delete(ID id) throws SQLException;
}
