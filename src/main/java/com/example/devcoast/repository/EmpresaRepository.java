package com.example.devcoast.repository;

import java.util.Date;
import java.util.List;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.example.devcoast.model.Empresa;

public interface EmpresaRepository extends MongoRepository<Empresa, String> {

    Empresa findByCorreo(String correo);

    // las empresas con un plan que ya venció (las que hay que bajar a gratis); evita recorrer toda la colección
    List<Empresa> findByPlanAndPlanVenceEnBefore(String plan, Date fecha);
}