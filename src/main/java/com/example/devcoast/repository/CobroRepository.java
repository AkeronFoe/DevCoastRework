package com.example.devcoast.repository;

import java.util.List;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.example.devcoast.model.Cobro;

public interface CobroRepository extends MongoRepository<Cobro, String> {
    List<Cobro> findByEmpresaIdOrderByFechaDesc(String empresaId);
}
