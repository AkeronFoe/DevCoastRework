package com.example.devcoast.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.example.devcoast.model.Solicitud;

public interface SolicitudRepository extends MongoRepository<Solicitud, String> {

    List<Solicitud> findByUsuarioIdOrderByFechaSolicitudDesc(String usuarioId);

    Optional<Solicitud> findByIdAndUsuarioId(String id, String usuarioId);

    Optional<Solicitud> findByUsuarioIdAndEmpleoId(String usuarioId, String empleoId);

    List<Solicitud> findByUsuarioIdAndEmpleoIdIn(String usuarioId, List<String> empleoIds);

    List<Solicitud> findByEmpleoIdOrderByFechaSolicitudDesc(String empleoId);

    Optional<Solicitud> findByIdAndEmpleoId(String id, String empleoId);

    long countByUsuarioIdAndEstado(String usuarioId, String estado);

    long countByUsuarioId(String usuarioId);

    long countByEmpleoId(String empleoId);

    List<Solicitud> findByEmpleoIdIn(List<String> empleoIds);
}
