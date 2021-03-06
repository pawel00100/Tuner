package com.tuner.persistence.db;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

// I believe this class could be used via composition instead of inheritance, but I failed to fix failing IOC for repository field

//TODO: add logger

@Component
public abstract class GenericDao<T> {

    @Autowired
    protected JpaRepository<T, Integer> repository;



    public boolean add(T object) {
        try {
            repository.save(object);
            return true;
        } catch (IllegalArgumentException e) {
            System.out.println(e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    public boolean addAll(Iterable<T> objects) { //for some reason List here breaks app launch, worth investigating
        try {
            repository.saveAll(objects);
            return true;
        } catch (IllegalArgumentException e) {
            System.out.println(e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    public boolean delete(T object) {
        try {
            repository.delete(object);
            return true;
        } catch (IllegalArgumentException e) {
            System.out.println(e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    public boolean delete(Integer id) {
        try {
            repository.deleteById(id);
            return true;
        } catch (IllegalArgumentException e) {
            System.out.println(e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    public boolean deleteAll() {
        try {
            repository.deleteAll();
            return true;
        } catch (IllegalArgumentException e) {
            System.out.println(e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    public Optional<T> getById(Integer id) {
        return repository.findById(id);
    }

    public List<T> getAll() {
        return repository.findAll();
    }

    public List<T> getFiltered(Predicate<T> predicate) {
        return getAll().stream().filter(predicate).collect(Collectors.toList());
    }

    protected Stream<T> getStream(Iterable<T> objects) {
        return StreamSupport.stream(objects.spliterator(), false);
    }

}
