package com.example.devcoast.repository;

import java.util.List;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.example.devcoast.model.Empleo;

public interface EmpleoRepository extends MongoRepository<Empleo, String> {
    List<Empleo> findByEmpresaIdOrderByIdDesc(String empresaId);

    List<Empleo> findByEmpresaIdAndEstadoOrderByIdDesc(String empresaId, String estado);

    long countByEmpresaId(String empresaId);

    long countByEmpresaIdAndEstado(String empresaId, String estado);

    long countByEmpresaIdAndDestacarTrueAndEstado(String empresaId, String estado);
}
