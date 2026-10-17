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

@Component
public class NotificacionInterceptor implements HandlerInterceptor {

    private final InvitacionRepository invitacionRepository;

    private final NotificacionRepository notificacionRepository;

    private final MensajeRepository mensajeRepository;

    private final MongoTemplate mongoTemplate;

    // vistas que usan el shell náutico (fragments/nautico :: sidebar). Solo ellas piden los datos
    // del menú lateral, para no gastar consultas en las páginas que todavía tienen el shell viejo.
    // Al migrar una página al shell nuevo, agregar aquí el nombre de su vista.
    private static final Set<String> VISTAS_SHELL_NAUTICO = Set.of("empleos", "solicitudes", "proyectos", "dashboard-carta");

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

        // el badge de invitaciones solo aplica a talento
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

    // datos del menú lateral: el total de ofertas activas (solo un conteo) y, para talento, el % de perfil completo
    private void cargarDatosShell(ModelAndView mav, String uid, boolean esTalento) {
        try {
            // mismo criterio que el listado de /empleos: ACTIVO o sin estado (documentos viejos)
            Query activos = new Query(new Criteria().orOperator(
                    Criteria.where("estado").is("ACTIVO"), Criteria.where("estado").exists(false)));
            mav.addObject("shellTotalOfertas", mongoTemplate.count(activos, Empleo.class));
        } catch (Exception ignored) {
            // si falla el conteo el menú simplemente no muestra la insignia
        }

        if (!esTalento) return;
        try {
            // si el controlador ya cargó al usuario de la sesión no se vuelve a consultar
            Object ya = mav.getModel().get("usuario");
            Usuario u = (ya instanceof Usuario uu && uid.equals(uu.getId())) ? uu : null;
            if (u == null) {
                // no es un conteo, pero es una sola lectura por _id y solo con los campos que mide getCompletitudPerfil()
                Query q = new Query(Criteria.where("_id").is(uid));
                q.fields().include("nombre", "rol_titulo", "bio", "stack", "email_contacto", "ubicacion", "modalidad",
                        "portfolio_url", "linkedin_url", "github_url", "cv_experiencias", "cv_educaciones");
                u = mongoTemplate.findOne(q, Usuario.class);
            }
            if (u != null) mav.addObject("shellCompletitud", u.getCompletitudPerfil());
        } catch (Exception ignored) {
            // sin el dato el menú oculta el widget de perfil
        }
    }
}
