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
import org.springframework.web.bind.annotation.RequestHeader;
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


@Controller
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

    public SolicitudController(SolicitudRepository solicitudRepository, EmpleoRepository empleoRepository, UsuarioRepository usuarioRepository, ProyectoRepository proyectoRepository, InvitacionRepository invitacionRepository, NotificacionRepository notificacionRepository, CleanupService cleanupService, EmpresaRepository empresaRepository, CobroRepository cobroRepository, PlanService planService) {
        this.solicitudRepository = solicitudRepository;
        this.empleoRepository = empleoRepository;
        this.usuarioRepository = usuarioRepository;
        this.proyectoRepository = proyectoRepository;
        this.invitacionRepository = invitacionRepository;
        this.notificacionRepository = notificacionRepository;
        this.cleanupService = cleanupService;
        this.empresaRepository = empresaRepository;
        this.cobroRepository = cobroRepository;
        this.planService = planService;
    }

    // Solicitudes y invitaciones del talento como pases de abordaje.
    // ?estado=todas|proceso|aceptadas|cerradas|invitaciones &orden=recientes|estado &q=
    // (tab=invitaciones se conserva porque InvitacionController redirige con ese parámetro)
    @GetMapping("/solicitudes")
    public String listarSolicitudes(
            @RequestParam(required = false) String estado,
            @RequestParam(required = false) String tab,
            @RequestParam(required = false) String orden,
            @RequestParam(required = false) String q,
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
        Usuario talento = usuarioRepository.findById(usuarioId).orElse(null);
        java.util.Date ahora = new java.util.Date();

        List<SolicitudVista> todas = new ArrayList<>();
        for (Solicitud s : solicitudRepository.findByUsuarioIdOrderByFechaSolicitudDesc(usuarioId))
            todas.add(SolicitudVista.de(s, talento, ahora));
        List<InvitacionVista> invitaciones = new ArrayList<>();
        for (Invitacion i : invitacionRepository.findByTalentoIdOrderByFechaEnvioDesc(usuarioId))
            invitaciones.add(InvitacionVista.de(i, ahora));

        String sel = estado != null ? estado : ("invitaciones".equals(tab) ? "invitaciones" : "todas");
        if (!List.of("todas", "proceso", "aceptadas", "cerradas", "invitaciones").contains(sel))
            sel = "todas";
        String ordenSel = "estado".equals(orden) ? "estado" : "recientes";
        String qSel = q == null ? "" : q.trim();
        String qNorm = normalizar(qSel);

        long nProceso = todas.stream().filter(v -> "revision".equals(v.getEstado())).count();
        long nAceptadas = todas.stream().filter(v -> "aprobada".equals(v.getEstado())).count();
        long nCerradas = todas.size() - nProceso - nAceptadas;

        List<SolicitudVista> lista = new ArrayList<>();
        for (SolicitudVista v : todas) {
            boolean ok = switch (sel) {
                case "proceso" -> "revision".equals(v.getEstado());
                case "aceptadas" -> "aprobada".equals(v.getEstado());
                case "cerradas" -> "rechazada".equals(v.getEstado()) || "descartado".equals(v.getEstado());
                case "invitaciones" -> false;
                default -> true;
            };
            if (!ok)
                continue;
            if (!qNorm.isEmpty() && !normalizar(v.getCargo() + " " + v.getEmpresa() + " " + v.getCategoria()).contains(qNorm))
                continue;
            lista.add(v);
        }
        // la lista llega por fecha (más reciente primero); sort() es estable
        if ("estado".equals(ordenSel))
            lista.sort(java.util.Comparator.comparingInt(SolicitudVista::getOrden));

        List<InvitacionVista> listaInv = new ArrayList<>();
        if ("invitaciones".equals(sel))
            for (InvitacionVista v : invitaciones)
                if (qNorm.isEmpty() || normalizar(v.getCargo() + " " + v.getEmpresa() + " " + v.getCategoria()).contains(qNorm))
                    listaInv.add(v);

        long invPendientes = invitaciones.stream().filter(v -> "pendiente".equals(v.getEstado())).count();
        InvitacionVista primeraPendiente = invitaciones.stream().filter(v -> "pendiente".equals(v.getEstado())).findFirst().orElse(null);

        model.addAttribute("solicitudes", lista);
        model.addAttribute("invitaciones", listaInv);
        model.addAttribute("nTodas", todas.size());
        model.addAttribute("nProceso", nProceso);
        model.addAttribute("nAceptadas", nAceptadas);
        model.addAttribute("nCerradas", nCerradas);
        model.addAttribute("nInvitaciones", invitaciones.size());
        model.addAttribute("invPendientes", invPendientes);
        model.addAttribute("primeraPendiente", primeraPendiente);
        model.addAttribute("estadoSel", sel);
        model.addAttribute("ordenSel", ordenSel);
        model.addAttribute("q", qSel.isEmpty() ? null : qSel);
        model.addAttribute("invPendienteCount", invPendientes);
        agregarSesion(session, model);
        return "solicitudes";
    }

    private static String normalizar(String s) {
        return java.text.Normalizer.normalize(s == null ? "" : s, java.text.Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "").toLowerCase().trim();
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

        Solicitud sol = solicitud.get();
        // el encabezado es el mismo pase que en la lista de Solicitudes (etapas, siguiente paso, estado)
        Usuario talento = usuarioRepository.findById(usuarioId).orElse(null);
        List<Proyecto> adjuntos = new ArrayList<>();
        if (sol.getProyectosIds() != null)
            sol.getProyectosIds().forEach(pId -> proyectoRepository.findById(pId).ifPresent(adjuntos::add));

        model.addAttribute("solicitud", sol);
        model.addAttribute("v", SolicitudVista.de(sol, talento, new java.util.Date()));
        model.addAttribute("proyectosAdjuntos", adjuntos);
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

        ResultadoAplicacion r = procesarAplicacion(id, RoleGuard.getId(session), RoleGuard.getCorreo(session),
                proyectosIds, mensajeMotivacion);

        switch (r.salida()) {
            case NO_DISPONIBLE:
                ra.addFlashAttribute("flashMessage", "El empleo ya no está disponible");
                ra.addFlashAttribute("flashType", "error");
                return "redirect:/empleos";
            case NO_ACTIVO:
                ra.addFlashAttribute("flashMessage", "Esta vacante ya no está activa");
                ra.addFlashAttribute("flashType", "error");
                return "redirect:/empleos/" + id;
            case YA_APLICO:
                ra.addFlashAttribute("flashMessage", "Ya aplicaste a este empleo");
                ra.addFlashAttribute("flashType", "info");
                return "redirect:/solicitudes/" + (r.solicitud() != null ? r.solicitud().getId() : "");
            default:
                break;
        }

        if (r.descartado()) {
            ra.addFlashAttribute("flashMessage",
                    "Postulación enviada, pero tu stack no cumple los requisitos mínimos indicados por la empresa. Puedes mejorar tu perfil e intentarlo con otros empleos.");
            ra.addFlashAttribute("flashType", "warning");
        } else {
            ra.addFlashAttribute("flashMessage", "Postulación enviada correctamente");
            ra.addFlashAttribute("flashType", "success");
        }
        return "redirect:/solicitudes/" + r.solicitud().getId();
    }

    // misma postulación que /aplicar, pero para el botón "Postularme" de la lista de empleos: responde JSON
    // (con el id de la solicitud, que sirve para deshacerla con /solicitudes/{id}/delete) en vez de redirigir
    @PostMapping(value = "/empleos/{id}/postular", produces = "application/json")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> postular(@PathVariable String id, HttpSession session) {
        if (RoleGuard.sinSesion(session))
            return ResponseEntity.status(401).body(Map.of("ok", false, "mensaje", "Inicia sesión para postularte."));
        if (RoleGuard.soloTalento(session))
            return ResponseEntity.status(403).body(Map.of("ok", false, "mensaje", "Las empresas no pueden aplicar a empleos."));

        ResultadoAplicacion r = procesarAplicacion(id, RoleGuard.getId(session), RoleGuard.getCorreo(session), null, null);
        switch (r.salida()) {
            case NO_DISPONIBLE:
                return ResponseEntity.status(404).body(Map.of("ok", false, "mensaje", "El empleo ya no está disponible."));
            case NO_ACTIVO:
                return ResponseEntity.status(409).body(Map.of("ok", false, "mensaje", "Esta vacante ya no está activa."));
            case YA_APLICO:
                return ResponseEntity.status(409).body(Map.of("ok", false, "yaAplicaste", true,
                        "mensaje", "Ya aplicaste a este empleo."));
            default:
                return ResponseEntity.ok(Map.of("ok", true,
                        "solicitudId", r.solicitud().getId(),
                        "estado", r.solicitud().getEstado(),
                        "descartado", r.descartado(),
                        "mensaje", r.descartado()
                                ? "Postulación enviada, pero tu stack no cumple los requisitos mínimos indicados por la empresa."
                                : "Postulación enviada."));
        }
    }

    private enum Salida { NO_DISPONIBLE, NO_ACTIVO, YA_APLICO, CREADA }

    private record ResultadoAplicacion(Salida salida, Solicitud solicitud, boolean descartado) {}

    // crea la solicitud (con el screening automático y la notificación a la empresa); lo comparten /aplicar y /postular
    private ResultadoAplicacion procesarAplicacion(String id, String usuarioId, String correo,
            List<String> proyectosIds, String mensajeMotivacion) {
        Optional<Empleo> empleoOpt = empleoRepository.findById(id);
        if (empleoOpt.isEmpty())
            return new ResultadoAplicacion(Salida.NO_DISPONIBLE, null, false);
        String estadoEmpleo = empleoOpt.get().getEstado();
        if (estadoEmpleo != null && !"ACTIVO".equals(estadoEmpleo))
            return new ResultadoAplicacion(Salida.NO_ACTIVO, null, false);

        Optional<Solicitud> existente = solicitudRepository.findByUsuarioIdAndEmpleoId(usuarioId, id);
        if (existente.isPresent())
            return new ResultadoAplicacion(Salida.YA_APLICO, existente.get(), false);

        Empleo empleo = empleoOpt.get();
        Usuario usuario = usuarioRepository.findById(usuarioId).orElse(null);

        Solicitud solicitud = new Solicitud();
        solicitud.setUsuarioId(usuarioId);
        solicitud.setUsuarioCorreo(correo);
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
            return new ResultadoAplicacion(Salida.YA_APLICO,
                    solicitudRepository.findByUsuarioIdAndEmpleoId(usuarioId, id).orElse(null), false);
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

        return new ResultadoAplicacion(Salida.CREADA, guardada, descartadoPorScreening);
    }

    // Postulaciones de una oferta: los mismos pases de candidato que /candidatos, para una sola oferta.
    // ?estado=revision|aceptadas|rechazadas (también acepta los valores de antes: aprobada, rechazada, descartado)
    @GetMapping("/empleos/{empleoId}/postulaciones")
    public String listarPostulaciones(
            @PathVariable String empleoId,
            @RequestParam(required = false) String estado,
            Model model, HttpSession session) {

        if (RoleGuard.soloEmpresa(session)) return "redirect:/login";

        Optional<Empleo> empleoOpt = empleoRepository.findById(empleoId);
        if (empleoOpt.isEmpty()) return "redirect:/misEmpleos";

        Empleo empleo = empleoOpt.get();
        if (!Objects.equals(empleo.getEmpresaId(), RoleGuard.getId(session)))
            return "redirect:/misEmpleos";

        String filtro = estado == null ? "" : estado.trim().toLowerCase();
        filtro = switch (filtro) {
            case "aprobada" -> "aceptadas";
            case "rechazada", "descartado" -> "rechazadas";
            case "revision", "aceptadas", "rechazadas" -> filtro;
            default -> "";
        };

        List<Solicitud> todas = solicitudRepository.findByEmpleoIdOrderByFechaSolicitudDesc(empleoId);
        Map<String, Usuario> candidatos = new java.util.HashMap<>();
        java.util.Set<String> usuarioIds = new java.util.HashSet<>();
        for (Solicitud s : todas)
            if (s.getUsuarioId() != null)
                usuarioIds.add(s.getUsuarioId());
        if (!usuarioIds.isEmpty())
            usuarioRepository.findAllById(usuarioIds).forEach(u -> candidatos.put(u.getId(), u));

        java.util.Date ahora = new java.util.Date();
        List<SolicitudVista> vistas = new ArrayList<>();
        for (Solicitud s : todas)
            vistas.add(SolicitudVista.de(s, candidatos.get(s.getUsuarioId()), ahora));
        final String f = filtro;
        List<SolicitudVista> filtradas = vistas.stream().filter(v -> switch (f) {
            case "revision" -> "revision".equals(v.getEstado());
            case "aceptadas" -> "aprobada".equals(v.getEstado());
            case "rechazadas" -> "rechazada".equals(v.getEstado()) || "descartado".equals(v.getEstado());
            default -> true;
        }).toList();

        com.example.devcoast.service.AreaEmpleo.Area area = com.example.devcoast.service.AreaEmpleo.de(empleo.getCategoria());
        PostulacionesOfertaVista grupo = PostulacionesOfertaVista.de(empleo.getId(), empleo.getTitulo(), empleo.getCategoriaMostrar(),
                area == com.example.devcoast.service.AreaEmpleo.Area.PROGRAMACION ? "p"
                        : area == com.example.devcoast.service.AreaEmpleo.Area.DISENO ? "d" : "o",
                !"CERRADO".equalsIgnoreCase(empleo.getEstado()), vistas, filtradas);

        model.addAttribute("empleo", empleo);
        model.addAttribute("g", grupo);
        model.addAttribute("estadoSel", filtro);
        model.addAttribute("hayFiltros", !filtro.isEmpty());
        model.addAttribute("fasesReclutamiento",
                List.of("ABIERTO", "SCREENING", "ENTREVISTAS", "OFERTA", "CERRADO_PROCESO"));
        // con el plan Gratis, aceptar una postulación registra la tarifa de colocación
        model.addAttribute("cobraColocacion", !planService.esPro(empresaRepository.findById(RoleGuard.getId(session)).orElse(null)));
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

    // Retira (borra) la solicitud propia. Con Accept: application/json (los botones de Empleos y Solicitudes que
    // usan fetch) responde JSON y no deja ningún aviso: la página ya muestra su propio toast. Sin JavaScript
    // redirige a /solicitudes con el aviso "Solicitud retirada."
    @PostMapping("/solicitudes/{id}/delete")
    public Object deleteSolicitud(@PathVariable String id,
            @RequestHeader(value = "Accept", required = false) String accept,
            HttpSession session, RedirectAttributes ra) {
        boolean json = accept != null && accept.contains("application/json");
        if (RoleGuard.soloTalento(session))
            return json ? ResponseEntity.status(401).body(Map.of("ok", false)) : "redirect:/login";
        String uid = RoleGuard.getId(session);
        boolean[] retirada = { false };
        solicitudRepository.findById(id).ifPresent(s -> {
            if (uid.equals(s.getUsuarioId())) {
                cleanupService.limpiarDatosDeSolicitud(id);
                solicitudRepository.deleteById(id);
                retirada[0] = true;
            }
        });
        if (json)
            return retirada[0] ? ResponseEntity.ok(Map.of("ok", true)) : ResponseEntity.status(404).body(Map.of("ok", false));
        if (retirada[0]) {
            ra.addFlashAttribute("flashMessage", "Solicitud retirada.");
            ra.addFlashAttribute("flashType", "success");
        }
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