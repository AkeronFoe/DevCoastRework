package com.example.devcoast.controller;

import java.util.Date;
import java.util.Objects;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.example.devcoast.model.Invitacion;
import com.example.devcoast.model.Notificacion;
import com.example.devcoast.repository.InvitacionRepository;
import com.example.devcoast.repository.NotificacionRepository;

import jakarta.servlet.http.HttpSession;

@Controller
public class InvitacionController {

    private final InvitacionRepository invitacionRepository;
    private final NotificacionRepository notificacionRepository;

    public InvitacionController(InvitacionRepository invitacionRepository,
            NotificacionRepository notificacionRepository) {
        this.invitacionRepository = invitacionRepository;
        this.notificacionRepository = notificacionRepository;
    }

    @PostMapping("/invitaciones/{id}/aceptar")
    public String aceptar(@PathVariable String id, HttpSession session, RedirectAttributes ra) {
        return cambiarEstado(id, "aceptada", session, ra);
    }

    @PostMapping("/invitaciones/{id}/rechazar")
    public String rechazar(@PathVariable String id, HttpSession session, RedirectAttributes ra) {
        return cambiarEstado(id, "rechazada", session, ra);
    }

    private String cambiarEstado(String id, String nuevoEstado, HttpSession session, RedirectAttributes ra) {
        Object usuarioId = session.getAttribute("usuarioActualId");
        if (usuarioId == null)
            return "redirect:/login";

        Invitacion inv = invitacionRepository.findById(id).orElse(null);
        if (inv == null || !Objects.equals(inv.getTalentoId(), usuarioId.toString())) {
            ra.addFlashAttribute("flashMessage", "Invitación no encontrada");
            ra.addFlashAttribute("flashType", "error");
            return "redirect:/solicitudes?tab=invitaciones";
        }

        // si ya respondiste no dejo que se cambie de nuevo, evita duplicar notificaciones
        if (!"pendiente".equals(inv.getEstado())) {
            ra.addFlashAttribute("flashMessage", "Ya respondiste esta invitación");
            ra.addFlashAttribute("flashType", "info");
            return "redirect:/solicitudes?tab=invitaciones";
        }

        inv.setEstado(nuevoEstado);
        invitacionRepository.save(inv);

        try {
            Notificacion notif = new Notificacion();
            notif.setDestinatarioId(inv.getEmpresaId());
            notif.setTipo("ESTADO_INVITACION");
            notif.setTitulo("aceptada".equals(nuevoEstado)
                    ? "¡" + inv.getTalentoNombre() + " aceptó tu invitación!"
                    : inv.getTalentoNombre() + " rechazó tu invitación");
            notif.setMensaje(inv.getEmpleoTitulo() != null
                    ? "Sobre la vacante: " + inv.getEmpleoTitulo()
                    : "Invitación directa a explorar oportunidades");
            notif.setFechaCreacion(new Date());
            notif.setUrl("/talentos/" + inv.getTalentoId());
            notificacionRepository.save(notif);
        } catch (Exception ignored) {
        }

        String msg = "aceptada".equals(nuevoEstado) ? "Invitación aceptada" : "Invitación rechazada";
        ra.addFlashAttribute("flashMessage", msg);
        ra.addFlashAttribute("flashType", "aceptada".equals(nuevoEstado) ? "success" : "info");
        return "redirect:/solicitudes?tab=invitaciones";
    }
}
