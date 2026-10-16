package com.example.devcoast.controller;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import com.example.devcoast.config.RoleGuard;
import com.example.devcoast.model.Empleo;
import com.example.devcoast.model.Solicitud;
import com.example.devcoast.repository.EmpleoRepository;
import com.example.devcoast.repository.SolicitudRepository;

import jakarta.servlet.http.HttpSession;

@Controller
public class CandidatosController {

    private    final EmpleoRepository    empleoRepository;
    private final SolicitudRepository solicitudRepository;

    // agrupa un empleo con sus solicitudes y las stats ya calculadas
    public static class EmpleoData {
        private final Empleo         empleo;
        private final List<Solicitud> solicitudes;
        private final long           cntRevision;
        private final long           cntAprobada;
        private final long           cntRechazada;

        public EmpleoData(Empleo empleo, List<Solicitud> solicitudes) {
            this.empleo       = empleo;
            this.solicitudes  = solicitudes;
            this.cntRevision  = solicitudes.stream().filter(s -> "revision" .equals(s.getEstado()) || s.getEstado() == null).count();
            this.cntAprobada  = solicitudes.stream().filter(s -> "aprobada" .equals(s.getEstado())).count();
            this.cntRechazada = solicitudes.stream().filter(s -> "rechazada".equals(s.getEstado())).count();
        }

        public Empleo          getEmpleo()       { return empleo;       }
        public List<Solicitud> getSolicitudes()  { return solicitudes;  }
        public long            getCntRevision()  { return cntRevision;  }
        public long            getCntAprobada()  { return cntAprobada;  }
        public long            getCntRechazada() { return cntRechazada; }
    }

    public CandidatosController(EmpleoRepository empleoRepository, SolicitudRepository solicitudRepository) {
        this.empleoRepository = empleoRepository;
        this.solicitudRepository = solicitudRepository;
    }

    @GetMapping("/candidatos")
    public String candidatos(Model model, HttpSession session) {

        if (RoleGuard.sinSesion(session))   return "redirect:/login";
        if (RoleGuard.soloEmpresa(session)) return "redirect:/dashboard";

        String empresaId = RoleGuard.getId(session);

        List<Empleo> empleos = empleoRepository
                .findByEmpresaIdOrderByIdDesc(empresaId)
                .stream()
                .filter(e -> !"eliminado".equalsIgnoreCase(e.getEstado()))
                .collect(Collectors.toList());

        List<String> empleoIds = empleos.stream().map(Empleo::getId).collect(Collectors.toList());
        List<Solicitud> todasSolicitudes = empleoIds.isEmpty()
                ? List.of()
                : solicitudRepository.findByEmpleoIdIn(empleoIds);
        todasSolicitudes.sort((a, b) -> {
            if (a.getFechaSolicitud() == null) return 1;
            if (b.getFechaSolicitud() == null) return -1;
            return b.getFechaSolicitud().compareTo(a.getFechaSolicitud());
        });

        Map<String, List<Solicitud>> solicitudesPorEmpleo = new HashMap<>();
        for (Solicitud s : todasSolicitudes)
            solicitudesPorEmpleo.computeIfAbsent(s.getEmpleoId(), k -> new ArrayList<>()).add(s);

        List<EmpleoData> data = empleos.stream()
                .map(e -> new EmpleoData(e, solicitudesPorEmpleo.getOrDefault(e.getId(), List.of())))
                .collect(Collectors.toList());

        long total      = data.stream().mapToLong(d -> d.getSolicitudes().size()).sum();
        long revision   = data.stream().mapToLong(EmpleoData::getCntRevision) .sum();
        long aprobadas  = data.stream().mapToLong(EmpleoData::getCntAprobada) .sum();
        long rechazadas = data.stream().mapToLong(EmpleoData::getCntRechazada).sum();

        model.addAttribute("empleosData",    data);
        model.addAttribute("statsTotal",     total);
        model.addAttribute("statsRevision",  revision);
        model.addAttribute("statsAprobadas", aprobadas);
        model.addAttribute("statsRechazadas",rechazadas);
        agregarSesion(session, model);
        return "candidatos";
    }

    private void agregarSesion(HttpSession session, Model model) {
        model.addAttribute("usuarioActualId",      RoleGuard.getId(session));
        model.addAttribute("usuarioActualCorreo",  RoleGuard.getCorreo(session));
        model.addAttribute("usuarioActualNombre",  RoleGuard.getNombre(session));
        model.addAttribute("usuarioActualTipo",    RoleGuard.getTipo(session));
        model.addAttribute("usuarioActualFotoUrl", session.getAttribute("usuarioActualFotoUrl"));
    }
}
