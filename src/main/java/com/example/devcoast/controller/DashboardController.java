package com.example.devcoast.controller;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;


import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import com.example.devcoast.config.RoleGuard;
import com.example.devcoast.model.Solicitud;
import com.example.devcoast.model.Usuario;
import com.example.devcoast.repository.SolicitudRepository;
import com.example.devcoast.repository.UsuarioRepository;

import jakarta.servlet.http.HttpSession;

// Ronda 5 del rediseño náutico (en curso).
// /dashboard/carta es TEMPORAL: muestra solo la carta de navegación para revisarla antes de armar el resto
// del Dashboard. Se elimina cuando /dashboard pase al diseño nuevo.
@Controller
public class DashboardController {

    private final SolicitudRepository solicitudRepository;
    private final UsuarioRepository usuarioRepository;

    public DashboardController(SolicitudRepository solicitudRepository, UsuarioRepository usuarioRepository) {
        this.solicitudRepository = solicitudRepository;
        this.usuarioRepository = usuarioRepository;
    }

    @GetMapping("/dashboard/carta")
    public String carta(HttpSession session, Model model) {
        if (RoleGuard.sinSesion(session))
            return "redirect:/login";
        if (RoleGuard.soloTalento(session))
            return "redirect:/dashboard";

        String id = RoleGuard.getId(session);
        Usuario talento = usuarioRepository.findById(id).orElse(null);
        Date ahora = new Date();
        List<SolicitudVista> solicitudes = new ArrayList<>();
        for (Solicitud s : solicitudRepository.findByUsuarioIdOrderByFechaSolicitudDesc(id))
            solicitudes.add(SolicitudVista.de(s, talento, ahora));
        model.addAttribute("carta", CartaVista.de(solicitudes));
        return "dashboard-carta";
    }
}
