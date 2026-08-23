package com.example.devcoast.service;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;

import com.example.devcoast.model.Empleo;
import com.example.devcoast.model.Invitacion;
import com.example.devcoast.model.Mensaje;
import com.example.devcoast.model.Notificacion;
import com.example.devcoast.model.Proyecto;
import com.example.devcoast.model.Solicitud;
import com.example.devcoast.model.Usuario;

// centraliza el borrado en cascada, sin esto quedan huérfanos por todos lados
// (mensajes que nadie puede abrir pero siguen sumando en "no leídos", notificaciones
// a páginas muertas, favoritos apuntando a empleos que ya no existen, etc.)
@Service
public class CleanupService {

    private final MongoTemplate mongoTemplate;

    public CleanupService(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    public void limpiarDatosDeSolicitud(String solicitudId) {
        if (solicitudId == null)
            return;
        mongoTemplate.remove(Query.query(Criteria.where("solicitudId").is(solicitudId)), Mensaje.class);
        mongoTemplate.remove(
                Query.query(Criteria.where("url").regex(".*" + java.util.regex.Pattern.quote(solicitudId) + ".*")),
                Notificacion.class);
    }

    public void limpiarDatosDeEmpleo(String empleoId) {
        if (empleoId == null)
            return;

        List<Solicitud> solicitudes = mongoTemplate.find(
                Query.query(Criteria.where("empleoId").is(empleoId)), Solicitud.class);
        for (Solicitud s : solicitudes) {
            limpiarDatosDeSolicitud(s.getId());
        }
        List<String> solicitudIds = solicitudes.stream().map(Solicitud::getId).collect(Collectors.toList());
        if (!solicitudIds.isEmpty()) {
            mongoTemplate.remove(Query.query(Criteria.where("id").in(solicitudIds)), Solicitud.class);
        }

        mongoTemplate.remove(Query.query(Criteria.where("empleoId").is(empleoId)), Invitacion.class);

        mongoTemplate.remove(
                Query.query(Criteria.where("url").regex(".*" + java.util.regex.Pattern.quote(empleoId) + ".*")),
                Notificacion.class);

        mongoTemplate.updateMulti(
                Query.query(Criteria.where("empleosFavoritos").is(empleoId)),
                new Update().pull("empleosFavoritos", empleoId),
                Usuario.class);
    }

    public void limpiarDatosDeTalento(String usuarioId) {
        if (usuarioId == null)
            return;

        List<Solicitud> solicitudes = mongoTemplate.find(
                Query.query(Criteria.where("usuarioId").is(usuarioId)), Solicitud.class);
        for (Solicitud s : solicitudes) {
            limpiarDatosDeSolicitud(s.getId());
        }
        mongoTemplate.remove(Query.query(Criteria.where("usuarioId").is(usuarioId)), Solicitud.class);

        mongoTemplate.remove(Query.query(Criteria.where("usuarioId").is(usuarioId)), Proyecto.class);
        mongoTemplate.remove(Query.query(Criteria.where("talentoId").is(usuarioId)), Invitacion.class);
        mongoTemplate.remove(Query.query(Criteria.where("destinatarioId").is(usuarioId)), Notificacion.class);
        mongoTemplate.remove(
                new Query(new Criteria().orOperator(
                        Criteria.where("remitenteId").is(usuarioId),
                        Criteria.where("destinatarioId").is(usuarioId))),
                Mensaje.class);
    }

    public void limpiarDatosDeEmpresa(String empresaId) {
        if (empresaId == null)
            return;

        List<Empleo> empleos = mongoTemplate.find(
                Query.query(Criteria.where("empresaId").is(empresaId)), Empleo.class);
        for (Empleo e : empleos) {
            limpiarDatosDeEmpleo(e.getId());
        }
        List<String> empleoIds = empleos.stream().map(Empleo::getId).collect(Collectors.toList());
        if (!empleoIds.isEmpty()) {
            mongoTemplate.remove(Query.query(Criteria.where("id").in(empleoIds)), Empleo.class);
        }

        mongoTemplate.remove(Query.query(Criteria.where("empresaId").is(empresaId)), Invitacion.class);
        mongoTemplate.remove(Query.query(Criteria.where("destinatarioId").is(empresaId)), Notificacion.class);
    }
}
