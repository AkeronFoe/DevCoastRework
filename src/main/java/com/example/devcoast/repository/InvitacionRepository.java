package com.example.devcoast.repository;

import java.util.List;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.example.devcoast.model.Invitacion;

public interface InvitacionRepository extends MongoRepository<Invitacion, String> {

    List<Invitacion> findByTalentoIdOrderByFechaEnvioDesc(String talentoId);

    List<Invitacion> findByEmpresaIdOrderByFechaEnvioDesc(String empresaId);

    boolean existsByEmpresaIdAndTalentoIdAndEmpleoId(String empresaId, String talentoId, String empleoId);

    boolean existsByEmpresaIdAndTalentoIdAndEmpleoIdIsNull(String empresaId, String talentoId);

    long countByTalentoIdAndEstado(String talentoId, String estado);
}
