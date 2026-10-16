package com.example.devcoast.config;

import java.util.List;

import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

import com.example.devcoast.model.Notificacion;
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

    public NotificacionInterceptor(InvitacionRepository invitacionRepository, NotificacionRepository notificacionRepository, MensajeRepository mensajeRepository) {
        this.invitacionRepository = invitacionRepository;
        this.notificacionRepository = notificacionRepository;
        this.mensajeRepository = mensajeRepository;
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
    }
}
