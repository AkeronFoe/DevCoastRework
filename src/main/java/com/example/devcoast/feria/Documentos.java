package com.example.devcoast.feria;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;

import com.example.devcoast.model.Empleo;
import com.example.devcoast.model.Empresa;
import com.example.devcoast.model.EventoEstado;
import com.example.devcoast.model.Invitacion;
import com.example.devcoast.model.Notificacion;
import com.example.devcoast.model.Proyecto;
import com.example.devcoast.model.Solicitud;
import com.example.devcoast.model.Usuario;

// armado de postulaciones, invitaciones y avisos con la misma forma que crea la app; lo usan las historias demo y el simulador
final class Documentos {

    private Documentos() {
    }

    // ids de los dos primeros proyectos publicados de la persona (se calculan una vez y se reutilizan)
    static List<String> proyectosPublicados(MongoTemplate mongo, Usuario u) {
        List<String> ids = new ArrayList<>();
        for (Proyecto p : mongo.find(Query.query(Criteria.where("usuarioId").is(u.getId()).and("publicado").is(true)), Proyecto.class))
            if (ids.size() < 2)
                ids.add(p.getId());
        return ids;
    }

    // id = el que se le va a poner al documento (con la fecha de creación dentro)
    static Solicitud solicitud(Usuario u, Empleo e, List<String> proyectosIds, String estado, Date fecha, Date decision, String id) {
        Solicitud s = new Solicitud();
        s.setUsuarioId(u.getId());
        s.setUsuarioCorreo(u.getCorreo());
        s.setUsuarioNombre(u.getNombre());
        s.setUsuarioRolTitulo(u.getRolTitulo());
        s.setEmpleoId(e.getId());
        s.setEmpleoTitulo(e.getTitulo());
        s.setEmpleoCategoria(e.getCategoria());
        s.setEmpleoSeniority(e.getSeniority());
        s.setEmpleoModalidad(e.getModalidad());
        s.setEmpleoContrato(e.getContrato());
        s.setEmpleoEmpresaNombre(e.getEmpresaNombre());
        s.setEmpleoEmpresaSector(e.getEmpresaSector());
        s.setEmpleoEmpresaTamano(e.getEmpresaTamano());
        s.setEmpleoEmpresaWeb(e.getEmpresaWeb());
        s.setEmpleoEmpresaDescripcion(e.getEmpresaDescripcion());
        s.setEmpleoIcono(e.getIcono());
        s.setEmpleoColorIcono(e.getColorIcono());
        s.setEmpleoStack(e.getStack());
        s.setEmpleoSoftSkills(e.getSoftSkills());
        s.setEmpleoDescripcionRol(e.getDescripcionRol());
        s.setEmpleoRequisitosDeseables(e.getRequisitosDeseables());
        s.setEmpleoMoneda(e.getMoneda());
        s.setEmpleoSalarioMin(e.getSalarioMin());
        s.setEmpleoSalarioMax(e.getSalarioMax());
        s.setEmpleoSalarioPeriodo(e.getSalarioPeriodo());
        s.setEmpleoTimezone(e.getTimezone());
        s.setEmpleoIngles(e.getIngles());
        s.setEmpleoVacantes(e.getVacantes());
        s.setEmpleoMostrarSalario(e.isMostrarSalario());
        s.setEstado(estado);
        s.setFechaSolicitud(fecha);
        String stack = e.getStack() == null || e.getStack().isBlank() ? "mi stack" : e.getStack().split(",")[0].trim();
        s.setMensajeMotivacion("Me interesa mucho el rol de " + e.getTitulo() + " en " + e.getEmpresaNombre()
                + ". He trabajado con " + stack + " y creo que puedo aportar desde el primer mes.");
        s.setProyectosIds(new ArrayList<>(proyectosIds));
        List<EventoEstado> historial = new ArrayList<>();
        historial.add(new EventoEstado("revision", fecha, "Postulación enviada"));
        if ("aprobada".equals(estado))
            historial.add(new EventoEstado("aprobada", decision, "Aceptada por la empresa"));
        else if ("rechazada".equals(estado))
            historial.add(new EventoEstado("rechazada", decision, "Rechazada por la empresa"));
        s.setHistorial(historial);
        s.setId(id);
        return s;
    }

    static Invitacion invitacion(Empresa em, Usuario t, Empleo emp, String estado, Date fecha, String id) {
        Invitacion i = new Invitacion();
        i.setEmpresaId(em.getId());
        i.setEmpresaNombre(em.getNombreEmpresa());
        i.setEmpresaSector(em.getSector());
        i.setEmpresaWeb(em.getSitioWeb());
        i.setEmpresaDescripcion(em.getDescripcion());
        i.setEmpresaTamano(em.getTamano());
        i.setTalentoId(t.getId());
        i.setTalentoNombre(t.getNombre());
        i.setTalentoCorreo(t.getCorreo());
        String pila = t.getNombre().split(" ")[0];
        if (emp != null) {
            i.setEmpleoId(emp.getId());
            i.setEmpleoTitulo(emp.getTitulo());
            i.setEmpleoModalidad(emp.getModalidad());
            i.setEmpleoContrato(emp.getContrato());
            i.setEmpleoCategoria(emp.getCategoria());
            i.setEmpleoSalarioMin(emp.getSalarioMin());
            i.setEmpleoSalarioMax(emp.getSalarioMax());
            i.setEmpleoMoneda(emp.getMoneda());
            i.setEmpleoMostrarSalario(emp.isMostrarSalario());
            i.setMensaje("Hola " + pila + ", revisamos tu perfil y creemos que encajas con el rol de " + emp.getTitulo() + " en "
                    + em.getNombreEmpresa() + ". ¿Tienes disponibilidad esta semana para conversar?");
        } else {
            i.setMensaje("Hola " + pila + ", nos gustó tu portafolio y queremos tenerte en cuenta para próximas vacantes en "
                    + em.getNombreEmpresa() + ". ¿Te interesa que hablemos?");
        }
        i.setEstado(estado);
        i.setFechaEnvio(fecha);
        i.setId(id);
        return i;
    }

    static Notificacion notificacion(String destinatario, String tipo, String titulo, String mensaje, Date fecha, String url, String id) {
        Notificacion n = new Notificacion();
        n.setDestinatarioId(destinatario);
        n.setTipo(tipo);
        n.setTitulo(titulo);
        n.setMensaje(mensaje);
        n.setFechaCreacion(fecha);
        n.setUrl(url);
        n.setId(id);
        return n;
    }
}
