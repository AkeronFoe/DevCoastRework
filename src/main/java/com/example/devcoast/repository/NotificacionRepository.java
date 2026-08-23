package com.example.devcoast.repository;

import java.util.List;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.example.devcoast.model.Notificacion;

public interface NotificacionRepository extends MongoRepository<Notificacion, String> {

    List<Notificacion> findByDestinatarioIdOrderByFechaCreacionDesc(String destinatarioId);

    List<Notificacion> findTop5ByDestinatarioIdOrderByFechaCreacionDesc(String destinatarioId);

    long countByDestinatarioIdAndLeidaFalse(String destinatarioId);
}
