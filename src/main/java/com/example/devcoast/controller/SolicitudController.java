package com.example.devcoast.controller;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.example.devcoast.config.RoleGuard;
import com.example.devcoast.model.Cobro;
import com.example.devcoast.model.Empleo;
import com.example.devcoast.model.Empresa;
import com.example.devcoast.model.EventoEstado;
import com.example.devcoast.model.Invitacion;
import com.example.devcoast.model.Notificacion;
import com.example.devcoast.model.Proyecto;
import com.example.devcoast.model.Solicitud;
import com.example.devcoast.model.Usuario;
import com.example.devcoast.repository.CobroRepository;
import com.example.devcoast.repository.EmpleoRepository;
import com.example.devcoast.repository.EmpresaRepository;
import com.example.devcoast.repository.InvitacionRepository;
import com.example.devcoast.repository.NotificacionRepository;
import com.example.devcoast.repository.ProyectoRepository;
import com.example.devcoast.repository.SolicitudRepository;
import com.example.devcoast.repository.UsuarioRepository;
import com.example.devcoast.service.CleanupService;
import com.example.devcoast.service.PlanService;

import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;


@Controller
@RequiredArgsConstructor
public class SolicitudController {

    private final SolicitudRepository solicitudRepository;
    private final EmpleoRepository empleoRepository;
    private final UsuarioRepository usuarioRepository;
    private final ProyectoRepository proyectoRepository;
    private final InvitacionRepository invitacionRepository;
    private final NotificacionRepository notificacionRepository;
    private final CleanupService cleanupService;
    private final EmpresaRepository empresaRepository;
    private final CobroRepository cobroRepository;
    private final PlanService planService;

    @GetMapping("/solicitudes")
    public String listarSolicitudes(
            @RequestParam(defaultValue = "solicitudes") String tab,
            Model model, HttpSession session, RedirectAttributes ra) {

        if (RoleGuard.soloTalento(session)) {
            if (RoleGuard.sinSesion(session))
                return "redirect:/login";
            ra.addFlashAttribute("flashMessage",
                    "Las empresas no tienen solicitudes enviadas. Revisa tus empleos publicados en 'Mis empleos'.");
            ra.addFlashAttribute("flashType", "info");
            return "redirect:/misEmpleos";
        }

        String usuarioId = RoleGuard.getId(session);
        List<Solicitud> solicitudes = solicitudRepository.findByUsuarioIdOrderByFechaSolicitudDesc(usuarioId);
        List<Invitacion> invitaciones = invitacionRepository.findByTalentoIdOrderByFechaEnvioDesc(usuarioId);

        model.addAttribute("solicitudes", solicitudes);
        model.addAttribute("totalSolicitudes", solicitudes.size());
        model.addAttribute("revisionCount", solicitudRepository.countByUsuarioIdAndEstado(usuarioId, "revision"));
        model.addAttribute("aprobadaCount", solicitudRepository.countByUsuarioIdAndEstado(usuarioId, "aprobada"));
        model.addAttribute("rechazadaCount", solicitudRepository.countByUsuarioIdAndEstado(usuarioId, "rechazada"));
        model.addAttribute("invitaciones", invitaciones);
        model.addAttribute("invPendienteCount", invitacionRepository.countByTalentoIdAndEstado(usuarioId, "pendiente"));
        model.addAttribute("invAceptadaCount", invitacionRepository.countByTalentoIdAndEstado(usuarioId, "aceptada"));
        model.addAttribute("invRechazadaCount", invitacionRepository.countByTalentoIdAndEstado(usuarioId, "rechazada"));
        model.addAttribute("tabActiva", tab);
        agregarSesion(session, model);
        return "solicitudes";
    }

    @GetMapping("/solicitudes/{id}")
    public String verSolicitud(@PathVariable String id, Model model, HttpSession session, RedirectAttributes ra) {
        if (RoleGuard.soloTalento(session)) {
            if (RoleGuard.sinSesion(session))
                return "redirect:/login";
            ra.addFlashAttribute("flashMessage", "Solo los talentos pueden ver sus solicitudes");
            ra.addFlashAttribute("flashType", "warning");
            return "redirect:/dashboard";
        }
        String usuarioId = RoleGuard.getId(session);
        Optional<Solicitud> solicitud = solicitudRepository.findByIdAndUsuarioId(id, usuarioId);
        if (solicitud.isEmpty())
            return "redirect:/solicitudes";

        model.addAttribute("solicitud", solicitud.get());
        agregarSesion(session, model);
        return "solicitud-detalle";
    }

    @PostMapping("/empleos/{id}/aplicar")
    public String aplicarEmpleo(
            @PathVariable String id,
            @RequestParam(required = false) List<String> proyectosIds,
            @RequestParam(required = false) String mensajeMotivacion,
            HttpSession session, RedirectAttributes ra) {

        if (RoleGuard.soloTalento(session)) {
            if (RoleGuard.sinSesion(session))
                return "redirect:/login";
            ra.addFlashAttribute("flashMessage", "Las empresas no pueden aplicar a empleos");
            ra.addFlashAttribute("flashType", "error");
            return "redirect:/empleos/" + id;
        }

        String usuarioId = RoleGuard.getId(session);

        Optional<Empleo> empleoOpt = empleoRepository.findById(id);
        if (empleoOpt.isEmpty()) {
            ra.addFlashAttribute("flashMessage", "El empleo ya no está disponible");
            ra.addFlashAttribute("flashType", "error");
            return "redirect:/empleos";
        }
        String estadoEmpleo = empleoOpt.get().getEstado();
        if (estadoEmpleo != null && !"ACTIVO".equals(estadoEmpleo)) {
            ra.addFlashAttribute("flashMessage", "Esta vacante ya no está activa");
            ra.addFlashAttribute("flashType", "error");
            return "redirect:/empleos/" + id;
        }

        Optional<Solicitud> existente = solicitudRepository.findByUsuarioIdAndEmpleoId(usuarioId, id);
        if (existente.isPresent()) {
            ra.addFlashAttribute("flashMessage", "Ya aplicaste a este empleo");
            ra.addFlashAttribute("flashType", "info");
            return "redirect:/solicitudes/" + existente.get().getId();
        }

        Empleo empleo = empleoOpt.get();
        Usuario usuario = usuarioRepository.findById(usuarioId).orElse(null);

        Solicitud solicitud = new Solicitud();
        solicitud.setUsuarioId(usuarioId);
        solicitud.setUsuarioCorreo(RoleGuard.getCorreo(session));
        if (usuario != null) {
            solicitud.setUsuarioNombre(usuario.getNombre());
            solicitud.setUsuarioRolTitulo(usuario.getRolTitulo());
        }
        solicitud.setEmpleoId(empleo.getId());
        solicitud.setEmpleoTitulo(empleo.getTitulo());
        solicitud.setEmpleoCategoria(empleo.getCategoria());
        solicitud.setEmpleoSeniority(empleo.getSeniority());
        solicitud.setEmpleoModalidad(empleo.getModalidad());
        solicitud.setEmpleoContrato(empleo.getContrato());
        solicitud.setEmpleoEmpresaNombre(empleo.getEmpresaNombre());
        solicitud.setEmpleoEmpresaSector(empleo.getEmpresaSector());
        solicitud.setEmpleoEmpresaTamano(empleo.getEmpresaTamano());
        solicitud.setEmpleoEmpresaWeb(empleo.getEmpresaWeb());
        solicitud.setEmpleoEmpresaDescripcion(empleo.getEmpresaDescripcion());
        solicitud.setEmpleoIcono(empleo.getIcono());
        solicitud.setEmpleoColorIcono(empleo.getColorIcono());
        solicitud.setEmpleoStack(empleo.getStack());
        solicitud.setEmpleoSoftSkills(empleo.getSoftSkills());
        solicitud.setEmpleoDescripcionRol(empleo.getDescripcionRol());
        solicitud.setEmpleoRequisitosDeseables(empleo.getRequisitosDeseables());
        solicitud.setEmpleoMoneda(empleo.getMoneda());
        solicitud.setEmpleoSalarioMin(empleo.getSalarioMin());
        solicitud.setEmpleoSalarioMax(empleo.getSalarioMax());
        solicitud.setEmpleoSalarioPeriodo(empleo.getSalarioPeriodo());
        solicitud.setEmpleoTimezone(empleo.getTimezone());
        solicitud.setEmpleoIngles(empleo.getIngles());
        solicitud.setEmpleoVacantes(empleo.getVacantes());
        solicitud.setEmpleoMostrarSalario(empleo.isMostrarSalario());
        solicitud.setEstado("revision");
        solicitud.setFechaSolicitud(new Date());

        EventoEstado eventoInicial = new EventoEstado("revision", solicitud.getFechaSolicitud(), "Postulación enviada");
        solicitud.setHistorial(new ArrayList<>(List.of(eventoInicial)));

        if (proyectosIds != null && !proyectosIds.isEmpty()) {
            List<String> proyectosPropios = proyectosIds.stream()
                    .filter(pid -> proyectoRepository.findById(pid)
                            .map(p -> usuarioId.equals(p.getUsuarioId()))
                            .orElse(false))
                    .collect(java.util.stream.Collectors.toList());
            solicitud.setProyectosIds(proyectosPropios);
        }
        if (mensajeMotivacion != null && !mensajeMotivacion.isBlank())
            solicitud.setMensajeMotivacion(mensajeMotivacion.trim());

        Solicitud guardada;
        try {
            guardada = solicitudRepository.save(solicitud);
        } catch (org.springframework.dao.DuplicateKeyException e) {
            // dos postulaciones al mismo empleo casi al mismo tiempo, la BD rechazó la segunda
            Optional<Solicitud> yaExiste = solicitudRepository.findByUsuarioIdAndEmpleoId(usuarioId, id);
            ra.addFlashAttribute("flashMessage", "Ya aplicaste a este empleo");
            ra.addFlashAttribute("flashType", "info");
            return "redirect:/solicitudes/" + yaExiste.map(Solicitud::getId).orElse("");
        }

        boolean descartadoPorScreening = false;
        if (empleo.isScreeningActivo()
                && empleo.getScreeningKeywords() != null
                && !empleo.getScreeningKeywords().isBlank()) {
            boolean pasaScreening = evaluarScreening(usuario, empleo.getScreeningKeywords());
            if (!pasaScreening) {
                guardada.setEstado("descartado");
                EventoEstado evScreening = new EventoEstado(
                        "descartado", new Date(),
                        "Descartado automáticamente: stack no cumple los requisitos mínimos del empleo");
                if (guardada.getHistorial() == null) guardada.setHistorial(new ArrayList<>());
                guardada.getHistorial().add(evScreening);
                guardada = solicitudRepository.save(guardada);
                descartadoPorScreening = true;
            }
        }

        if (!descartadoPorScreening) {
            try {
                Notificacion notif = new Notificacion();
                notif.setDestinatarioId(empleo.getEmpresaId());
                notif.setTipo("NUEVA_SOLICITUD");
                notif.setTitulo("Nueva postulación recibida");
                notif.setMensaje((usuario != null ? usuario.getNombre() : "Un talento")
                        + " aplicó a " + empleo.getTitulo());
                notif.setFechaCreacion(new Date());
                notif.setUrl("/empleos/" + empleo.getId() + "/postulaciones/" + guardada.getId());
                notificacionRepository.save(notif);
            } catch (Exception ignored) {}
        }

        if (descartadoPorScreening) {
            ra.addFlashAttribute("flashMessage",
                    "Postulación enviada, pero tu stack no cumple los requisitos mínimos indicados por la empresa. Puedes mejorar tu perfil e intentarlo con otros empleos.");
            ra.addFlashAttribute("flashType", "warning");
        } else {
            ra.addFlashAttribute("flashMessage", "Postulación enviada correctamente");
            ra.addFlashAttribute("flashType", "success");
        }
        return "redirect:/solicitudes/" + guardada.getId();
    }

    @GetMapping("/empleos/{empleoId}/postulaciones")
    public String listarPostulaciones(
            @PathVariable String empleoId,
            @RequestParam(required = false, defaultValue = "todas") String estado,
            Model model, HttpSession session) {

        if (RoleGuard.soloEmpresa(session)) return "redirect:/login";

        Optional<Empleo> empleoOpt = empleoRepository.findById(empleoId);
        if (empleoOpt.isEmpty()) return "redirect:/misEmpleos";

        Empleo empleo = empleoOpt.get();
        if (!Objects.equals(empleo.getEmpresaId(), RoleGuard.getId(session)))
            return "redirect:/misEmpleos";

        List<Solicitud> todas = solicitudRepository.findByEmpleoIdOrderByFechaSolicitudDesc(empleoId);

        List<Solicitud> filtradas = "todas".equals(estado) ? todas
                : todas.stream().filter(s -> estado.equals(s.getEstado())).toList();

        long totalRevision   = todas.stream().filter(s -> "revision".equals(s.getEstado())).count();
        long totalAprobadas  = todas.stream().filter(s -> "aprobada".equals(s.getEstado())).count();
        long totalRechazadas = todas.stream().filter(s -> "rechazada".equals(s.getEstado())).count();
        long totalDescartados = todas.stream().filter(s -> "descartado".equals(s.getEstado())).count();

        model.addAttribute("empleo", empleo);
        model.addAttribute("solicitudes", filtradas);
        model.addAttribute("estadoFiltro", estado);
        model.addAttribute("totalTodas",      todas.size());
        model.addAttribute("totalRevision",   totalRevision);
        model.addAttribute("totalAprobadas",  totalAprobadas);
        model.addAttribute("totalRechazadas", totalRechazadas);
        model.addAttribute("totalDescartados",totalDescartados);
        model.addAttribute("fasesReclutamiento",
                List.of("ABIERTO", "SCREENING", "ENTREVISTAS", "OFERTA", "CERRADO_PROCESO"));
        agregarSesion(session, model);
        return "postulaciones-empleo";
    }

    @GetMapping("/empleos/{empleoId}/postulaciones/{solicitudId}")
    public String verPostulacionEmpresa(
            @PathVariable String empleoId, @PathVariable String solicitudId,
            Model model, HttpSession session) {

        if (RoleGuard.soloEmpresa(session))
            return "redirect:/login";

        Optional<Empleo> empleoOpt = empleoRepository.findById(empleoId);
        if (empleoOpt.isEmpty())
            return "redirect:/empleos";

        Empleo empleo = empleoOpt.get();
        if (!Objects.equals(empleo.getEmpresaId(), RoleGuard.getId(session)))
            return "redirect:/empleos";

        Optional<Solicitud> solicitudOpt = solicitudRepository.findByIdAndEmpleoId(solicitudId, empleoId);
        if (solicitudOpt.isEmpty())
            return "redirect:/empleos/" + empleoId;

        Solicitud solicitud = solicitudOpt.get();
        Usuario usuario = usuarioRepository.findById(solicitud.getUsuarioId()).orElse(null);
        if (usuario == null)
            return "redirect:/empleos/" + empleoId;

        List<Proyecto> proyectosAdjuntos = new ArrayList<>();
        if (solicitud.getProyectosIds() != null)
            solicitud.getProyectosIds()
                    .forEach(pId -> proyectoRepository.findById(pId).ifPresent(proyectosAdjuntos::add));

        model.addAttribute("empleo", empleo);
        model.addAttribute("solicitud", solicitud);
        model.addAttribute("usuario", usuario);
        model.addAttribute("modoLectura", true);
        model.addAttribute("proyectosAdjuntos", proyectosAdjuntos);
        agregarSesion(session, model);
        return "curriculum";
    }

    @PostMapping("/empleos/{empleoId}/postulaciones/{solicitudId}/aprobar")
    public String aprobar(@PathVariable String empleoId, @PathVariable String solicitudId,
            HttpSession session, RedirectAttributes ra) {
        return cambiarEstado(empleoId, solicitudId, "aprobada", session, ra);
    }

    @PostMapping("/empleos/{empleoId}/postulaciones/{solicitudId}/rechazar")
    public String rechazar(@PathVariable String empleoId, @PathVariable String solicitudId,
            HttpSession session, RedirectAttributes ra) {
        return cambiarEstado(empleoId, solicitudId, "rechazada", session, ra);
    }

    // para el drag-and-drop del kanban
    @PostMapping(value = "/empleos/{empleoId}/postulaciones/{solicitudId}/estado",
                 produces = "application/json")
    @ResponseBody
    public ResponseEntity<?> cambiarEstadoAjax(
            @PathVariable String empleoId, @PathVariable String solicitudId,
            @RequestParam String nuevoEstado,
            HttpSession session) {

        if (RoleGuard.sinSesion(session))
            return ResponseEntity.status(401).body(Map.of("error", "Sin sesión"));

        Optional<Empleo> empleoOpt = empleoRepository.findById(empleoId);
        if (empleoOpt.isEmpty() || !Objects.equals(empleoOpt.get().getEmpresaId(), RoleGuard.getId(session)))
            return ResponseEntity.status(403).body(Map.of("error", "Sin permiso"));

        if (!List.of("revision", "aprobada", "rechazada").contains(nuevoEstado))
            return ResponseEntity.badRequest().body(Map.of("error", "Estado inválido"));

        var solicitudOpt = solicitudRepository.findByIdAndEmpleoId(solicitudId, empleoId);
        if (solicitudOpt.isEmpty())
            return ResponseEntity.notFound().build();

        Solicitud s = solicitudOpt.get();
        String estadoAnterior = s.getEstado();
        s.setEstado(nuevoEstado);
        EventoEstado ev = new EventoEstado(nuevoEstado, new Date(),
                "aprobada".equals(nuevoEstado) ? "Aceptada por la empresa"
                : "rechazada".equals(nuevoEstado) ? "Rechazada por la empresa"
                : "Vuelto a revisión");
        if (s.getHistorial() == null) s.setHistorial(new ArrayList<>());
        s.getHistorial().add(ev);
        solicitudRepository.save(s);
        cobrarSiAplica(empleoOpt.get().getEmpresaId(), s, estadoAnterior, nuevoEstado);

        if (!nuevoEstado.equals(estadoAnterior)) {
            try {
                Notificacion notif = new Notificacion();
                notif.setDestinatarioId(s.getUsuarioId());
                notif.setTipo("ESTADO_SOLICITUD");
                notif.setTitulo("aprobada".equals(nuevoEstado) ? "¡Tu postulación fue aceptada! 🎉" : "Actualización de tu postulación");
                notif.setMensaje(s.getEmpleoEmpresaNombre()
                        + ("aprobada".equals(nuevoEstado) ? " aceptó tu postulación a " : " actualizó tu postulación a ")
                        + s.getEmpleoTitulo());
                notif.setFechaCreacion(new Date());
                notif.setUrl("/solicitudes/" + solicitudId);
                notificacionRepository.save(notif);
            } catch (Exception ignored) {}
        }
        return ResponseEntity.ok(Map.of("ok", true, "estado", nuevoEstado));
    }

    private String cambiarEstado(String empleoId, String solicitudId, String estado,
            HttpSession session, RedirectAttributes ra) {
        if (RoleGuard.soloEmpresa(session))
            return "redirect:/login";

        Optional<Empleo> empleoOpt = empleoRepository.findById(empleoId);
        if (empleoOpt.isEmpty() || !Objects.equals(empleoOpt.get().getEmpresaId(), RoleGuard.getId(session)))
            return "redirect:/empleos";

        solicitudRepository.findByIdAndEmpleoId(solicitudId, empleoId).ifPresent(s -> {
            String estadoAnterior = s.getEstado();
            s.setEstado(estado);
            EventoEstado ev = new EventoEstado(estado, new Date(),
                    "aprobada".equals(estado) ? "Aceptada por la empresa" : "Rechazada por la empresa");
            if (s.getHistorial() == null) s.setHistorial(new ArrayList<>());
            s.getHistorial().add(ev);
            solicitudRepository.save(s);
            cobrarSiAplica(empleoOpt.get().getEmpresaId(), s, estadoAnterior, estado);

            try {
                boolean aprobada = "aprobada".equals(estado);
                Notificacion notif = new Notificacion();
                notif.setDestinatarioId(s.getUsuarioId());
                notif.setTipo("ESTADO_SOLICITUD");
                notif.setTitulo(aprobada ? "¡Tu postulación fue aceptada! 🎉" : "Actualización de tu postulación");
                notif.setMensaje((aprobada ? "Felicidades, " : "")
                        + s.getEmpleoEmpresaNombre()
                        + (aprobada ? " aceptó tu postulación a " : " revisó tu postulación a ")
                        + s.getEmpleoTitulo());
                notif.setFechaCreacion(new Date());
                notif.setUrl("/solicitudes/" + solicitudId);
                notificacionRepository.save(notif);
            } catch (Exception ignored) {}
        });

        ra.addFlashAttribute("flashMessage",
                "aprobada".equals(estado) ? "Postulación aprobada" : "Postulación rechazada");
        ra.addFlashAttribute("flashType", "aprobada".equals(estado) ? "success" : "info");
        return "redirect:/empleos/" + empleoId + "/postulaciones/" + solicitudId;
    }

    @PostMapping("/solicitudes/{id}/delete")
    public String deleteSolicitud(@PathVariable String id, HttpSession session) {
        if (RoleGuard.soloTalento(session))
            return "redirect:/login";
        String uid = RoleGuard.getId(session);
        solicitudRepository.findById(id).ifPresent(s -> {
            if (uid.equals(s.getUsuarioId())) {
                cleanupService.limpiarDatosDeSolicitud(id);
                solicitudRepository.deleteById(id);
            }
        });
        return "redirect:/solicitudes";
    }

    // vuelve a poner en 'revision' a alguien descartado por el screening automático
    @PostMapping("/empleos/{empleoId}/postulaciones/{solicitudId}/reconsiderar")
    public String reconsiderar(@PathVariable String empleoId, @PathVariable String solicitudId,
            HttpSession session, RedirectAttributes ra) {
        return cambiarEstado(empleoId, solicitudId, "revision", session, ra);
    }

    private void cobrarSiAplica(String empresaId, Solicitud s, String estadoAnterior, String nuevoEstado) {
        if (!"aprobada".equals(nuevoEstado) || "aprobada".equals(estadoAnterior))
            return;

        Empresa empresa = empresaRepository.findById(empresaId).orElse(null);
        if (planService.esPro(empresa))
            return;

        Cobro cobro = new Cobro();
        cobro.setEmpresaId(empresaId);
        cobro.setSolicitudId(s.getId());
        cobro.setEmpleoTitulo(s.getEmpleoTitulo());
        cobro.setMonto(PlanService.TARIFA_COLOCACION);
        cobro.setMotivo("Tarifa de colocación exitosa");
        cobro.setFecha(new Date());
        cobroRepository.save(cobro);
    }

    // pasa si el talento tiene al menos una de las skills requeridas
    private boolean evaluarScreening(Usuario usuario, String keywords) {
        if (usuario == null || usuario.getStack() == null || usuario.getStack().isEmpty())
            return false;
        List<String> stackLower = usuario.getStack().stream()
                .filter(s -> s != null && !s.isBlank())
                .map(s -> s.toLowerCase().trim())
                .collect(java.util.stream.Collectors.toList());
        for (String kw : keywords.split(",")) {
            String kwLower = kw.trim().toLowerCase();
            if (kwLower.isBlank()) continue;
            if (stackLower.stream().anyMatch(s -> s.contains(kwLower) || kwLower.contains(s)))
                return true;
        }
        return false;
    }

    private void agregarSesion(HttpSession session, Model model) {
        model.addAttribute("usuarioActualId", RoleGuard.getId(session));
        model.addAttribute("usuarioActualCorreo", RoleGuard.getCorreo(session));
        model.addAttribute("usuarioActualNombre", RoleGuard.getNombre(session));
        model.addAttribute("usuarioActualTipo", RoleGuard.getTipo(session));
        model.addAttribute("usuarioActualFotoUrl", session.getAttribute("usuarioActualFotoUrl"));
    }
}