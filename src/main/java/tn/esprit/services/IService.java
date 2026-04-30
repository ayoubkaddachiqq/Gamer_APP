package tn.esprit.services;

import java.util.List;

public interface IService<T> {
    // Add a new record
    void add(T t);

    // Delete a record by its Primary Key
    void delete(int id);

    // Update an existing record
    void update(T t);

    // Fetch everything from the table
    List<T> getAll();

    // Find a specific record
    T getById(int id);
}