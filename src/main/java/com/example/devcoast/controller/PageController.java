package com.example.devcoast.controller;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.example.devcoast.config.RoleGuard;
import com.example.devcoast.model.Empleo;
import com.example.devcoast.model.Empresa;
import com.example.devcoast.model.Solicitud;
import com.example.devcoast.model.Usuario;
import com.example.devcoast.repository.EmpleoRepository;
import com.example.devcoast.repository.EmpresaRepository;
import com.example.devcoast.repository.InvitacionRepository;
import com.example.devcoast.repository.ProyectoRepository;
import com.example.devcoast.repository.SolicitudRepository;
import com.example.devcoast.repository.UsuarioRepository;
import com.example.devcoast.service.CleanupService;

import jakarta.servlet.http.HttpSession;

@Controller
public class PageController {

    private final UsuarioRepository usuarioRepository;
    private final EmpleoRepository empleoRepository;
    private final SolicitudRepository solicitudRepository;
    private final ProyectoRepository proyectoRepository;
    private final InvitacionRepository invitacionRepository;
    private final MongoTemplate mongoTemplate;
    private final EmpresaRepository empresaRepository;
    private final BCryptPasswordEncoder passwordEncoder;
    private final CleanupService cleanupService;

    public PageController(UsuarioRepository usuarioRepository, EmpleoRepository empleoRepository, SolicitudRepository solicitudRepository, ProyectoRepository proyectoRepository, InvitacionRepository invitacionRepository, MongoTemplate mongoTemplate, EmpresaRepository empresaRepository, BCryptPasswordEncoder passwordEncoder, CleanupService cleanupService) {
        this.usuarioRepository = usuarioRepository;
        this.empleoRepository = empleoRepository;
        this.solicitudRepository = solicitudRepository;
        this.proyectoRepository = proyectoRepository;
        this.invitacionRepository = invitacionRepository;
        this.mongoTemplate = mongoTemplate;
        this.empresaRepository = empresaRepository;
        this.passwordEncoder = passwordEncoder;
        this.cleanupService = cleanupService;
    }

    @GetMapping("/")
    public String root(HttpSession session) {
        return RoleGuard.sinSesion(session) ? "redirect:/about-us" : "redirect:/dashboard";
    }

    @GetMapping("/dashboard")
    public String dashboard(HttpSession session, Model model) {
        if (RoleGuard.sinSesion(session))
            return "redirect:/login";

        String id = RoleGuard.getId(session);
        cargarUsuario(session, model);

        String tipo = RoleGuard.getTipo(session);
        Usuario usuarioActual = id != null ? usuarioRepository.findById(id).orElse(null) : null;
        model.addAttribute("mostrarOnboarding", usuarioActual != null && !usuarioActual.isOnboardingCompleto() && "talento".equals(tipo));

        if (RoleGuard.esEmpresa(session)) {
            List<Empleo> empleosActivos = empleoRepository.findByEmpresaIdAndEstadoOrderByIdDesc(id, "ACTIVO");
            List<Empleo> empleosCerrados = empleoRepository.findByEmpresaIdAndEstadoOrderByIdDesc(id, "CERRADO");

            List<String> todosEmpleoIds = new ArrayList<>();
            empleosActivos.forEach(e -> todosEmpleoIds.add(e.getId()));
            empleosCerrados.forEach(e -> todosEmpleoIds.add(e.getId()));

            long totalRecibidas = todosEmpleoIds.isEmpty() ? 0
                    : mongoTemplate.count(
                            Query.query(Criteria.where("empleoId").in(todosEmpleoIds)),
                            Solicitud.class);
            long enRevision = todosEmpleoIds.isEmpty() ? 0
                    : mongoTemplate.count(
                            Query.query(Criteria.where("empleoId").in(todosEmpleoIds).and("estado").is("revision")),
                            Solicitud.class);
            long invEnviadas = invitacionRepository.findByEmpresaIdOrderByFechaEnvioDesc(id).size();

            model.addAttribute("statEmpleosActivos", empleosActivos.size());
            model.addAttribute("statEmpleosCerrados", empleosCerrados.size());
            model.addAttribute("statTotalEmpleos", empleosActivos.size() + empleosCerrados.size());
            model.addAttribute("statSolicitudesRecibidas", totalRecibidas);
            model.addAttribute("statSolicitudesPendientes", enRevision);
            model.addAttribute("statInvitacionesEnviadas", invEnviadas);
            model.addAttribute("empleosActivos", empleosActivos);

        } else {
            long enviadas = solicitudRepository.countByUsuarioId(id);
            long enRevision = solicitudRepository.countByUsuarioIdAndEstado(id, "revision");
            long aceptadas = solicitudRepository.countByUsuarioIdAndEstado(id, "aprobada");
            long invPend = invitacionRepository.countByTalentoIdAndEstado(id, "pendiente");

            List<com.example.devcoast.model.Proyecto> todosProyectos =
                    proyectoRepository.findByUsuarioIdOrderByCreadoEnDesc(id);
            long proyectos = todosProyectos.size();
            int maxP = Math.min(3, (int) proyectos);
            model.addAttribute("proyectosRecientes", todosProyectos.subList(0, maxP));

            Usuario talento = usuarioRepository.findById(id).orElse(null);
            if (talento != null && talento.getEmpleosFavoritos() != null && !talento.getEmpleosFavoritos().isEmpty()) {
                List<String> favIds = talento.getEmpleosFavoritos();
                int maxF = Math.min(3, favIds.size());
                List<com.example.devcoast.model.Empleo> favEmpleos = favIds.subList(0, maxF).stream()
                        .map(eid -> empleoRepository.findById(eid).orElse(null))
                        .filter(java.util.Objects::nonNull)
                        .collect(java.util.stream.Collectors.toList());
                model.addAttribute("empleosFavoritosRecientes", favEmpleos);
            } else {
                model.addAttribute("empleosFavoritosRecientes", java.util.Collections.emptyList());
            }

            // recomendaciones basadas en el stack del talento
            if (talento != null && talento.getStack() != null && !talento.getStack().isEmpty()) {
                Set<String> yaAplicados = solicitudRepository
                        .findByUsuarioIdOrderByFechaSolicitudDesc(id).stream()
                        .map(Solicitud::getEmpleoId)
                        .collect(Collectors.toSet());

                // agarro hasta 60 empleos activos recientes como candidatos
                Query recQuery = new Query()
                        .addCriteria(Criteria.where("estado").is("ACTIVO"))
                        .with(Sort.by(Sort.Direction.DESC, "_id"))
                        .limit(60);
                List<com.example.devcoast.model.Empleo> candidatos =
                        mongoTemplate.find(recQuery, com.example.devcoast.model.Empleo.class);

                java.util.TreeMap<Integer, List<com.example.devcoast.model.Empleo>> porScore =
                        new java.util.TreeMap<>(java.util.Collections.reverseOrder());
                for (com.example.devcoast.model.Empleo e : candidatos) {
                    if (yaAplicados.contains(e.getId())) continue;
                    int sc = com.example.devcoast.controller.EmpleoController.calcularScore(talento, e);
                    if (sc >= 30) porScore.computeIfAbsent(sc, k -> new ArrayList<>()).add(e);
                }
                List<com.example.devcoast.model.Empleo> recomendados = new ArrayList<>();
                Map<String, Integer> recScores = new java.util.LinkedHashMap<>();
                outer:
                for (Map.Entry<Integer, List<com.example.devcoast.model.Empleo>> entry : porScore.entrySet()) {
                    for (com.example.devcoast.model.Empleo e : entry.getValue()) {
                        if (recomendados.size() >= 5) break outer;
                        recomendados.add(e);
                        recScores.put(e.getId(), entry.getKey());
                    }
                }
                model.addAttribute("empleosRecomendados", recomendados);
                model.addAttribute("scoresRecomendados",  recScores);
            } else {
                model.addAttribute("empleosRecomendados", java.util.Collections.emptyList());
                model.addAttribute("scoresRecomendados",  java.util.Collections.emptyMap());
            }

            model.addAttribute("statSolicitudesEnviadas", enviadas);
            model.addAttribute("statSolicitudesEnRevision", enRevision);
            model.addAttribute("statSolicitudesAceptadas", aceptadas);
            model.addAttribute("statProyectos", proyectos);
            model.addAttribute("statInvitacionesPendientes", invPend);
            model.addAttribute("invPendienteCount", invPend);
        }

        return "dashboard";
    }

    @GetMapping("/perfil")
    public String perfil(HttpSession session, Model model) {
        if (RoleGuard.sinSesion(session))
            return "redirect:/login";
        cargarUsuario(session, model);
        String id = RoleGuard.getId(session);
        if (id != null && !RoleGuard.esEmpresa(session)) {
            long proyectos  = proyectoRepository.findByUsuarioIdOrderByCreadoEnDesc(id).size();
            long invPend    = invitacionRepository.countByTalentoIdAndEstado(id, "pendiente");
            long invTotal   = invitacionRepository.findByTalentoIdOrderByFechaEnvioDesc(id).size();
            long solActivas = solicitudRepository.countByUsuarioIdAndEstado(id, "revision");
            long solTotal   = solicitudRepository.countByUsuarioId(id);
            long solAcept   = solicitudRepository.countByUsuarioIdAndEstado(id, "aprobada");
            model.addAttribute("statProyectos",            proyectos);
            model.addAttribute("statInvitacionesPendientes", invPend);
            model.addAttribute("statInvitacionesTotal",    invTotal);
            model.addAttribute("statSolicitudesActivas",   solActivas);
            model.addAttribute("statSolicitudesTotal",     solTotal);
            model.addAttribute("statSolicitudesAceptadas", solAcept);
        }
        return "perfil";
    }

    @GetMapping("/curriculum")
    public String curriculum(HttpSession session, Model model, RedirectAttributes ra) {
        if (RoleGuard.soloTalento(session)) {
            if (RoleGuard.sinSesion(session))
                return "redirect:/login";
            ra.addFlashAttribute("flashMessage", "El curriculum es una función exclusiva para talentos");
            ra.addFlashAttribute("flashType", "info");
            return "redirect:/dashboard";
        }
        cargarUsuario(session, model);
        model.addAttribute("modoLectura", false);
        String id = RoleGuard.getId(session);
        if (id != null) {
            model.addAttribute("misProyectos", proyectoRepository.findByUsuarioIdOrderByCreadoEnDesc(id));
        } else {
            model.addAttribute("misProyectos", java.util.Collections.emptyList());
        }
        return "curriculum";
    }

    @GetMapping("/ajustes")
    public String ajustes(HttpSession session, Model model) {
        if (RoleGuard.sinSesion(session))
            return "redirect:/login";
        cargarUsuario(session, model);
        return "ajustes";
    }

    @GetMapping("/cambiarPassword")
    public String cambiarPasswordForm(HttpSession session, Model model) {
        if (RoleGuard.sinSesion(session))
            return "redirect:/login";
        cargarUsuario(session, model);
        return "cambiarPassword";
    }

    @PostMapping("/cambiarPassword")
    public String cambiarPassword(
            @RequestParam String passwordActual,
            @RequestParam String passwordNueva,
            @RequestParam String passwordConfirmar,
            HttpSession session,
            RedirectAttributes ra) {

        if (RoleGuard.sinSesion(session))
            return "redirect:/login";

        String id   = RoleGuard.getId(session);
        String tipo = RoleGuard.getTipo(session);

        if (passwordNueva == null || passwordNueva.trim().length() < 8) {
            ra.addFlashAttribute("flashMessage", "La nueva contraseña debe tener al menos 8 caracteres");
            ra.addFlashAttribute("flashType", "error");
            return "redirect:/cambiarPassword";
        }
        if (!passwordNueva.equals(passwordConfirmar)) {
            ra.addFlashAttribute("flashMessage", "Las contraseñas no coinciden");
            ra.addFlashAttribute("flashType", "error");
            return "redirect:/cambiarPassword";
        }

        if ("empresa".equals(tipo)) {
            Empresa empresa = empresaRepository.findById(id).orElse(null);
            if (empresa == null || !passwordEncoder.matches(passwordActual, empresa.getPassword())) {
                ra.addFlashAttribute("flashMessage", "La contraseña actual es incorrecta");
                ra.addFlashAttribute("flashType", "error");
                return "redirect:/cambiarPassword";
            }
            empresa.setPassword(passwordEncoder.encode(passwordNueva));
            empresaRepository.save(empresa);
        } else {
            Usuario usuario = usuarioRepository.findById(id).orElse(null);
            if (usuario == null || !passwordEncoder.matches(passwordActual, usuario.getPassword())) {
                ra.addFlashAttribute("flashMessage", "La contraseña actual es incorrecta");
                ra.addFlashAttribute("flashType", "error");
                return "redirect:/cambiarPassword";
            }
            usuario.setPassword(passwordEncoder.encode(passwordNueva));
            usuarioRepository.save(usuario);
        }

        ra.addFlashAttribute("flashMessage", "Contraseña actualizada correctamente");
        ra.addFlashAttribute("flashType", "success");
        return "redirect:/ajustes";
    }

    @PostMapping("/cuenta/eliminar")
    public String eliminarCuenta(@RequestParam String password, HttpSession session, RedirectAttributes ra) {
        if (RoleGuard.sinSesion(session))
            return "redirect:/login";

        String id   = RoleGuard.getId(session);
        String tipo = RoleGuard.getTipo(session);

        if ("empresa".equals(tipo)) {
            Empresa empresa = empresaRepository.findById(id).orElse(null);
            if (empresa == null || !passwordEncoder.matches(password, empresa.getPassword())) {
                ra.addFlashAttribute("flashMessage", "Contraseña incorrecta. Tu cuenta no fue eliminada.");
                ra.addFlashAttribute("flashType", "error");
                return "redirect:/ajustes";
            }
            cleanupService.limpiarDatosDeEmpresa(id);
            empresaRepository.deleteById(id);
        } else {
            Usuario usuario = usuarioRepository.findById(id).orElse(null);
            if (usuario == null || !passwordEncoder.matches(password, usuario.getPassword())) {
                ra.addFlashAttribute("flashMessage", "Contraseña incorrecta. Tu cuenta no fue eliminada.");
                ra.addFlashAttribute("flashType", "error");
                return "redirect:/ajustes";
            }
            cleanupService.limpiarDatosDeTalento(id);
            usuarioRepository.deleteById(id);
        }

        session.invalidate();
        ra.addFlashAttribute("flashMessage", "Tu cuenta ha sido eliminada");
        ra.addFlashAttribute("flashType", "info");
        return "redirect:/login";
    }

    @GetMapping("/register-options")
    public String registerOptions() {
        return "Register-options";
    }

    @GetMapping("/about-us")
    public String aboutUs(Model model) {
        long totalTalentos = mongoTemplate.count(
                Query.query(Criteria.where("perfil_publico").is(true).and("tipo").ne("empresa")), Usuario.class);
        long totalEmpresas = empresaRepository.count();
        long totalEmpleosActivos = mongoTemplate.count(
                Query.query(new Criteria().orOperator(
                        Criteria.where("estado").is("ACTIVO"), Criteria.where("estado").exists(false))),
                Empleo.class);
        long totalSolicitudes = solicitudRepository.count();
        long solicitudesAprobadas = mongoTemplate.count(
                Query.query(Criteria.where("estado").is("aprobada")), Solicitud.class);
        long invitacionesAceptadas = mongoTemplate.count(
                Query.query(Criteria.where("estado").is("aceptada")),
                com.example.devcoast.model.Invitacion.class);
        long conexiones = solicitudesAprobadas + invitacionesAceptadas;

        long respondidas = solicitudesAprobadas + mongoTemplate.count(
                Query.query(Criteria.where("estado").is("rechazada")), Solicitud.class);
        long tasaRespuesta = totalSolicitudes > 0
                ? Math.min(99, (respondidas * 100) / totalSolicitudes)
                : 0;

        model.addAttribute("kpiTalentos", totalTalentos);
        model.addAttribute("kpiEmpresas", totalEmpresas);
        model.addAttribute("kpiEmpleos", totalEmpleosActivos);
        model.addAttribute("kpiConexiones", conexiones);
        model.addAttribute("kpiTasaRespuesta", tasaRespuesta);
        return "aboutUs";
    }

    // vista previa del perfil, tal como lo ven las empresas
    @GetMapping("/perfil/preview")
    public String perfilPreview(HttpSession session, Model model) {
        if (RoleGuard.sinSesion(session))  return "redirect:/login";
        if (RoleGuard.esEmpresa(session))  return "redirect:/perfil";

        String id = RoleGuard.getId(session);
        Usuario talento = usuarioRepository.findById(id).orElse(null);
        if (talento == null) return "redirect:/perfil";

        model.addAttribute("talento", talento);
        model.addAttribute("esPreview", true);
        model.addAttribute("esEmpresa", false);
        model.addAttribute("yaInvitado", false);
        model.addAttribute("misEmpleos", java.util.Collections.emptyList());
        model.addAttribute("usuarioActualId",     RoleGuard.getId(session));
        model.addAttribute("usuarioActualCorreo", RoleGuard.getCorreo(session));
        model.addAttribute("usuarioActualNombre", RoleGuard.getNombre(session));
        model.addAttribute("usuarioActualTipo",   RoleGuard.getTipo(session));
        model.addAttribute("usuarioActualFotoUrl", session.getAttribute("usuarioActualFotoUrl"));
        return "talento-perfil";
    }

    // CV para imprimir o exportar a PDF, sin librerías externas
    @GetMapping("/perfil/exportar-pdf")
    public String exportarPerfilPDF(HttpSession session, Model model) {
        if (RoleGuard.sinSesion(session) || RoleGuard.esEmpresa(session))
            return "redirect:/perfil";

        String id = RoleGuard.getId(session);
        Usuario usuario = usuarioRepository.findById(id).orElse(null);
        if (usuario == null) return "redirect:/perfil";

        List<com.example.devcoast.model.Proyecto> proyectos =
                proyectoRepository.findByUsuarioIdOrderByCreadoEnDesc(id);

        model.addAttribute("usuario",   usuario);
        model.addAttribute("proyectos", proyectos);
        return "perfil-cv";
    }

    private void cargarUsuario(HttpSession session, Model model) {
        String id = RoleGuard.getId(session);
        if (id != null) {
            if (RoleGuard.esEmpresa(session)) {
                Empresa empresa = empresaRepository.findById(id).orElse(null);
                if (empresa != null) {
                    Usuario sintetico = new Usuario();
                    sintetico.setId(empresa.getId());
                    sintetico.setNombre(empresa.getNombreEmpresa() != null ? empresa.getNombreEmpresa() : "Empresa");
                    sintetico.setCorreo(empresa.getCorreo());
                    sintetico.setRolTitulo(empresa.getSector());
                    sintetico.setBio(empresa.getDescripcion());
                    sintetico.setPortfolioUrl(empresa.getSitioWeb());
                    sintetico.setFotoUrl(empresa.getFotoUrl());
                    sintetico.setEmpleosFavoritos(new ArrayList<>());
                    model.addAttribute("usuario", sintetico);
                    model.addAttribute("usuarioActualFotoUrl", empresa.getFotoUrl());
                    if (empresa.getNombreEmpresa() != null)
                        session.setAttribute("usuarioActualNombre", empresa.getNombreEmpresa());
                }

                List<Empleo> activos = empleoRepository.findByEmpresaIdAndEstadoOrderByIdDesc(id, "ACTIVO");
                if (!activos.isEmpty()) {
                    List<String> empIds = activos.stream()
                            .map(Empleo::getId)
                            .collect(java.util.stream.Collectors.toList());
                    long solPend = mongoTemplate.count(
                            Query.query(Criteria.where("empleoId").in(empIds).and("estado").is("revision")),
                            Solicitud.class);
                    model.addAttribute("solicitudesPendientesEmpresa", solPend);
                } else {
                    model.addAttribute("solicitudesPendientesEmpresa", 0L);
                }
            } else {
                Usuario usuario = usuarioRepository.findById(id).orElse(null);
                if (usuario != null) {
                    if (usuario.getNombre() == null || usuario.getNombre().isBlank())
                        usuario.setNombre("Dev");
                    if (usuario.getEmpleosFavoritos() == null)
                        usuario.setEmpleosFavoritos(new ArrayList<>());
                    model.addAttribute("usuario", usuario);
                    model.addAttribute("usuarioActualFotoUrl", usuario.getFotoUrl());
                }
            }
        }
        model.addAttribute("usuarioActualId", RoleGuard.getId(session));
        model.addAttribute("usuarioActualNombre", RoleGuard.getNombre(session));
        model.addAttribute("usuarioActualCorreo", RoleGuard.getCorreo(session));
        model.addAttribute("usuarioActualTipo", RoleGuard.getTipo(session));
    }

    @GetMapping("/analytics")
    public String analytics(HttpSession session, Model model) {
        if (RoleGuard.sinSesion(session))
            return "redirect:/login";
        cargarUsuario(session, model);

        String id = RoleGuard.getId(session);

        analyticsPlatStats(model);
        if (RoleGuard.esEmpresa(session)) {
            analyticsEmpresaStats(id, model);
        } else {
            analyticsTalentoStats(id, model);
        }

        return "analytics";
    }

    private void analyticsPlatStats(Model model) {
        long aprobadas = mongoTemplate.count(
                Query.query(Criteria.where("estado").is("aprobada")), Solicitud.class);
        long aceptadas = mongoTemplate.count(
                Query.query(Criteria.where("estado").is("aceptada")),
                com.example.devcoast.model.Invitacion.class);
        model.addAttribute("platTalentos",   usuarioRepository.count());
        model.addAttribute("platEmpresas",   empresaRepository.count());
        model.addAttribute("platEmpleos",    empleoRepository.count());
        model.addAttribute("platConexiones", aprobadas + aceptadas);
    }

    private void analyticsEmpresaStats(String empresaId, Model model) {
        List<Empleo> misEmpleos = empleoRepository.findByEmpresaIdOrderByIdDesc(empresaId);
        List<String> misIds = misEmpleos.stream()
                .map(Empleo::getId).collect(java.util.stream.Collectors.toList());

        List<Solicitud> misSolicitudes = misIds.isEmpty()
                ? java.util.Collections.emptyList()
                : solicitudRepository.findByEmpleoIdIn(misIds);

        long activos       = misEmpleos.stream().filter(e -> "ACTIVO".equalsIgnoreCase(e.getEstado())).count();
        long cerrados      = misEmpleos.stream().filter(e -> "CERRADO".equalsIgnoreCase(e.getEstado())).count();
        long solTotal      = misSolicitudes.size();
        long solAprobadas  = misSolicitudes.stream().filter(s -> "aprobada".equals(s.getEstado())).count();
        long solRechazadas = misSolicitudes.stream().filter(s -> "rechazada".equals(s.getEstado())).count();
        long solRevision   = misSolicitudes.stream().filter(s -> "revision".equals(s.getEstado())).count();
        long tasa          = solTotal > 0 ? Math.round(solAprobadas * 100.0 / solTotal) : 0;

        List<com.example.devcoast.model.Invitacion> misInvs =
                invitacionRepository.findByEmpresaIdOrderByFechaEnvioDesc(empresaId);
        long invAceptadas = misInvs.stream().filter(i -> "aceptada".equals(i.getEstado())).count();

        model.addAttribute("empActivos",       activos);
        model.addAttribute("empCerrados",      cerrados);
        model.addAttribute("empSolTotal",      solTotal);
        model.addAttribute("empSolAprobadas",  solAprobadas);
        model.addAttribute("empSolRechazadas", solRechazadas);
        model.addAttribute("empSolRevision",   solRevision);
        model.addAttribute("empTasa",          tasa);
        model.addAttribute("empInvEnviadas",   (long) misInvs.size());
        model.addAttribute("empInvAceptadas",  invAceptadas);

        anBreakdown(model, "empModalidad", misEmpleos.stream()
                .filter(e -> e.getModalidad() != null && !e.getModalidad().isBlank())
                .collect(java.util.stream.Collectors.groupingBy(
                        e -> anCapFirst(e.getModalidad()), java.util.stream.Collectors.counting())), 99);
        anBreakdown(model, "empCategoria", misEmpleos.stream()
                .filter(e -> e.getCategoria() != null && !e.getCategoria().isBlank())
                .collect(java.util.stream.Collectors.groupingBy(
                        Empleo::getCategoria, java.util.stream.Collectors.counting())), 6);
        anBreakdown(model, "empSeniority", misEmpleos.stream()
                .filter(e -> e.getSeniority() != null && !e.getSeniority().isBlank())
                .collect(java.util.stream.Collectors.groupingBy(
                        Empleo::getSeniority, java.util.stream.Collectors.counting())), 99);

        java.util.Map<String, Long> solPorEmpleo = misSolicitudes.stream()
                .collect(java.util.stream.Collectors.groupingBy(
                        Solicitud::getEmpleoId, java.util.stream.Collectors.counting()));

        List<java.util.Map<String, Object>> topEmpleos = misEmpleos.stream()
                .map(e -> {
                    java.util.Map<String, Object> row = new java.util.LinkedHashMap<>();
                    row.put("titulo",           e.getTitulo());
                    row.put("modalidad",        e.getModalidad());
                    row.put("estado",           e.getEstado());
                    row.put("categoria",        e.getCategoria());
                    row.put("totalSolicitudes", solPorEmpleo.getOrDefault(e.getId(), 0L));
                    return row;
                })
                .sorted((a, b) -> Long.compare(
                        (Long) b.get("totalSolicitudes"), (Long) a.get("totalSolicitudes")))
                .limit(8)
                .collect(java.util.stream.Collectors.toList());
        model.addAttribute("empTopEmpleos", topEmpleos);

        List<java.util.Map<String, Object>> top6 = topEmpleos.stream()
                .limit(6).collect(java.util.stream.Collectors.toList());
        model.addAttribute("empTopLabels", top6.stream()
                .map(r -> anTruncate((String) r.get("titulo"), 22))
                .collect(java.util.stream.Collectors.toList()));
        model.addAttribute("empTopValues", top6.stream()
                .map(r -> r.get("totalSolicitudes"))
                .collect(java.util.stream.Collectors.toList()));
    }

    private void analyticsTalentoStats(String usuarioId, Model model) {
        List<Solicitud> misSolicitudes =
                solicitudRepository.findByUsuarioIdOrderByFechaSolicitudDesc(usuarioId);

        long solTotal      = misSolicitudes.size();
        long solAprobadas  = misSolicitudes.stream().filter(s -> "aprobada".equals(s.getEstado())).count();
        long solRechazadas = misSolicitudes.stream().filter(s -> "rechazada".equals(s.getEstado())).count();
        long solRevision   = misSolicitudes.stream().filter(s -> "revision".equals(s.getEstado())).count();
        long tasa          = solTotal > 0 ? Math.round(solAprobadas * 100.0 / solTotal) : 0;

        List<com.example.devcoast.model.Invitacion> misInvs =
                invitacionRepository.findByTalentoIdOrderByFechaEnvioDesc(usuarioId);
        long invPendientes = misInvs.stream().filter(i -> "pendiente".equals(i.getEstado())).count();
        long proyectos     = proyectoRepository.findByUsuarioIdOrderByCreadoEnDesc(usuarioId).size();

        model.addAttribute("talSolTotal",      solTotal);
        model.addAttribute("talSolAprobadas",  solAprobadas);
        model.addAttribute("talSolRechazadas", solRechazadas);
        model.addAttribute("talSolRevision",   solRevision);
        model.addAttribute("talTasa",          tasa);
        model.addAttribute("talInvRecibidas",  (long) misInvs.size());
        model.addAttribute("talInvPendientes", invPendientes);
        model.addAttribute("talProyectos",     proyectos);

        anBreakdown(model, "talCategoria", misSolicitudes.stream()
                .filter(s -> s.getEmpleoCategoria() != null && !s.getEmpleoCategoria().isBlank())
                .collect(java.util.stream.Collectors.groupingBy(
                        Solicitud::getEmpleoCategoria, java.util.stream.Collectors.counting())), 6);
        anBreakdown(model, "talModalidad", misSolicitudes.stream()
                .filter(s -> s.getEmpleoModalidad() != null && !s.getEmpleoModalidad().isBlank())
                .collect(java.util.stream.Collectors.groupingBy(
                        s -> anCapFirst(s.getEmpleoModalidad()), java.util.stream.Collectors.counting())), 99);
        anBreakdown(model, "talSeniority", misSolicitudes.stream()
                .filter(s -> s.getEmpleoSeniority() != null && !s.getEmpleoSeniority().isBlank())
                .collect(java.util.stream.Collectors.groupingBy(
                        Solicitud::getEmpleoSeniority, java.util.stream.Collectors.counting())), 99);

        model.addAttribute("talRecentSolicitudes",
                misSolicitudes.stream().limit(8).collect(java.util.stream.Collectors.toList()));
    }

    private void anBreakdown(Model model, String prefix,
                              java.util.Map<String, Long> raw, int limit) {
        List<java.util.Map.Entry<String, Long>> entries = new ArrayList<>(raw.entrySet());
        entries.sort((a, b) -> Long.compare(b.getValue(), a.getValue()));
        if (entries.size() > limit) entries = entries.subList(0, limit);
        model.addAttribute(prefix + "Labels", entries.stream()
                .map(java.util.Map.Entry::getKey).collect(java.util.stream.Collectors.toList()));
        model.addAttribute(prefix + "Values", entries.stream()
                .map(java.util.Map.Entry::getValue).collect(java.util.stream.Collectors.toList()));
    }

    private static String anCapFirst(String s) {
        if (s == null || s.isBlank()) return s;
        return Character.toUpperCase(s.charAt(0)) + s.substring(1).toLowerCase();
    }

    private static String anTruncate(String s, int max) {
        if (s == null) return "";
        return s.length() <= max ? s : s.substring(0, max - 1) + "…";
    }

}