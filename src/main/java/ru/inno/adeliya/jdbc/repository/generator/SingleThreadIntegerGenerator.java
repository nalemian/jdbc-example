package ru.inno.adeliya.jdbc.repository.generator;

public class SingleThreadIntegerGenerator implements IdGenerator<Integer>{

    protected Integer counter;
    public SingleThreadIntegerGenerator() {
        this.counter = 0;
    }

    @Override
    public Integer generate() {
        counter++;
        return counter;
    }
}
