package com.example.devcoast.config;

import java.util.List;
import java.util.Set;

import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

import com.example.devcoast.model.Empleo;
import com.example.devcoast.model.Notificacion;
import com.example.devcoast.model.Usuario;
import com.example.devcoast.repository.InvitacionRepository;
import com.example.devcoast.repository.MensajeRepository;
import com.example.devcoast.repository.NotificacionRepository;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

// esto de acá le pone a cada página los numeritos de la campana, los mensajes sin leer y demás avisos
@Component
public class NotificacionInterceptor implements HandlerInterceptor {

    private final InvitacionRepository invitacionRepository;

    private final NotificacionRepository notificacionRepository;

    private final MensajeRepository mensajeRepository;

    private final MongoTemplate mongoTemplate;

    private static final Set<String> VISTAS_SHELL_NAUTICO = Set.of("empleos", "solicitudes", "proyectos", "dashboard", "dashboard-empresa",
            "talentos", "talento-perfil", "perfil", "perfil-empresa", "misEmpleos", "candidatos", "ajustes", "empleos-guardados", "detalles-empleo", "curriculum", "facturacion", "solicitud-detalle", "postulaciones-empleo", "crearEmpleo", "subirProyecto", "cambiarPassword", "detalle-proyecto", "analytics", "empresa-perfil", "mensajes", "chat");

    public NotificacionInterceptor(InvitacionRepository invitacionRepository, NotificacionRepository notificacionRepository,
            MensajeRepository mensajeRepository, MongoTemplate mongoTemplate) {
        this.invitacionRepository = invitacionRepository;
        this.notificacionRepository = notificacionRepository;
        this.mensajeRepository = mensajeRepository;
        this.mongoTemplate = mongoTemplate;
    }

    @Override
    public void postHandle(HttpServletRequest request, HttpServletResponse response,
            Object handler, ModelAndView modelAndView) {

        if (modelAndView == null || modelAndView.getViewName() == null) return;
        String view = modelAndView.getViewName();
        if (view.startsWith("redirect:") || view.startsWith("forward:")) return;

        HttpSession session = request.getSession(false);
        if (session == null) return;

        Object usuarioId = session.getAttribute("usuarioActualId");
        if (usuarioId == null) return;

        String uid = usuarioId.toString();
        boolean esTalento = "talento".equals(session.getAttribute("usuarioActualTipo"));

        if (esTalento) {
            try {
                long pendientes = invitacionRepository.countByTalentoIdAndEstado(uid, "pendiente");
                modelAndView.addObject("invPendienteCount", pendientes);
            } catch (Exception ignored) {
                modelAndView.addObject("invPendienteCount", 0L);
            }
        } else {
            modelAndView.addObject("invPendienteCount", 0L);
        }

        try {
            long noLeidas = notificacionRepository.countByDestinatarioIdAndLeidaFalse(uid);
            modelAndView.addObject("notifNoLeidas", noLeidas);

            List<Notificacion> recientes = notificacionRepository
                    .findTop5ByDestinatarioIdOrderByFechaCreacionDesc(uid);
            modelAndView.addObject("notifRecientes", recientes);
        } catch (Exception ignored) {
            modelAndView.addObject("notifNoLeidas", 0L);
            modelAndView.addObject("notifRecientes", List.of());
        }

        try {
            long mensajesNoLeidos = mensajeRepository.countByDestinatarioIdAndLeidoFalse(uid);
            modelAndView.addObject("mensajesNoLeidos", mensajesNoLeidos);
        } catch (Exception ignored) {
            modelAndView.addObject("mensajesNoLeidos", 0L);
        }

        if (VISTAS_SHELL_NAUTICO.contains(view)) {
            cargarDatosShell(modelAndView, uid, esTalento);
        }
    }

    private void cargarDatosShell(ModelAndView mav, String uid, boolean esTalento) {
        try {
            Query activos = new Query(new Criteria().orOperator(
                    Criteria.where("estado").is("ACTIVO"), Criteria.where("estado").exists(false)));
            mav.addObject("shellTotalOfertas", mongoTemplate.count(activos, Empleo.class));
        } catch (Exception ignored) {
        }

        if (!esTalento) {
            cargarPendientesEmpresa(mav, uid);
            return;
        }
        try {
            Object ya = mav.getModel().get("usuario");
            Usuario u = (ya instanceof Usuario uu && uid.equals(uu.getId())) ? uu : null;
            if (u == null) {
                Query q = new Query(Criteria.where("_id").is(uid));
                q.fields().include("nombre", "rol_titulo", "bio", "stack", "email_contacto", "ubicacion", "modalidad",
                        "portfolio_url", "linkedin_url", "github_url", "cv_experiencias", "cv_educaciones");
                u = mongoTemplate.findOne(q, Usuario.class);
            }
            if (u != null) mav.addObject("shellCompletitud", u.getCompletitudPerfil());
        } catch (Exception ignored) {
        }
    }

    private void cargarPendientesEmpresa(ModelAndView mav, String uid) {
        if (mav.getModel().containsKey("solicitudesPendientesEmpresa")) return;
        try {
            Query ofertas = new Query(Criteria.where("empresaId").is(uid).and("estado").is("ACTIVO"));
            ofertas.fields().include("_id");
            List<String> ids = mongoTemplate.find(ofertas, Empleo.class).stream().map(Empleo::getId).toList();
            long pendientes = ids.isEmpty() ? 0
                    : mongoTemplate.count(new Query(Criteria.where("empleoId").in(ids).and("estado").is("revision")),
                            com.example.devcoast.model.Solicitud.class);
            mav.addObject("solicitudesPendientesEmpresa", pendientes);
        } catch (Exception ignored) {
        }
    }
}
