package com.example.devcoast.repository;

import java.util.Date;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

import com.example.devcoast.model.Mensaje;

public interface MensajeRepository extends MongoRepository<Mensaje, String> {

        List<Mensaje> findBySolicitudIdOrderByFechaEnvioAsc(String solicitudId);

        long countBySolicitudIdAndDestinatarioIdAndLeidoFalse(String solicitudId, String destinatarioId);

        long countByDestinatarioIdAndLeidoFalse(String destinatarioId);

        Optional<Mensaje> findFirstBySolicitudIdOrderByFechaEnvioDesc(String solicitudId);

        List<Mensaje> findBySolicitudIdInOrderByFechaEnvioDesc(List<String> solicitudIds);

        Page<Mensaje> findBySolicitudIdOrderByFechaEnvioDesc(String solicitudId, Pageable pageable);

        List<Mensaje> findBySolicitudIdAndFechaEnvioBeforeOrderByFechaEnvioDesc(
                        String solicitudId, Date fecha, Pageable pageable);

        List<Mensaje> findBySolicitudIdAndFechaEnvioAfterOrderByFechaEnvioAsc(
                        String solicitudId, Date fecha);
}