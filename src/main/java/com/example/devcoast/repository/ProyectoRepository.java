package com.example.devcoast.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.example.devcoast.model.Proyecto;

public interface ProyectoRepository extends MongoRepository<Proyecto, String> {

    List<Proyecto> findByUsuarioIdOrderByCreadoEnDesc(String usuarioId);

    Optional<Proyecto> findByIdAndUsuarioId(String id, String usuarioId);

    List<Proyecto> findByUsuarioIdInAndPublicadoTrueOrderByCreadoEnDesc(List<String> usuarioIds);
}
