package com.example.devcoast.repository;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.example.devcoast.model.Empresa;

public interface EmpresaRepository extends MongoRepository<Empresa, String> {

    Empresa findByCorreo(String correo);
}