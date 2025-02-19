package ru.inno.adeliya.jdbc.repository.generator;

public class SynchronizedIntegerGenerator extends SingleThreadIntegerGenerator{
    @Override
    public synchronized Integer generate() {
        counter++;
        return counter;
    }
}
