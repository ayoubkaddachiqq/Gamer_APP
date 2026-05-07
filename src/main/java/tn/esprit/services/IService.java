package tn.esprit.services;

import java.util.List;

public interface IService<T> {
    int add(T t);
    void delete(int id);
    void update(T t);
    List<T> getAll();
    T getById(int id);
}