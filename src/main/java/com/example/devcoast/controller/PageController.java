package com.example.devcoast.controller;

import java.util.ArrayList;
import java.util.List;
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
import com.example.devcoast.model.Proyecto;
import com.example.devcoast.model.Solicitud;
import com.example.devcoast.model.Usuario;
import com.example.devcoast.repository.CobroRepository;
import com.example.devcoast.repository.EmpleoRepository;
import com.example.devcoast.repository.EmpresaRepository;
import com.example.devcoast.repository.InvitacionRepository;
import com.example.devcoast.repository.ProyectoRepository;
import com.example.devcoast.repository.SolicitudRepository;
import com.example.devcoast.repository.UsuarioRepository;
import com.example.devcoast.service.CleanupService;
import com.example.devcoast.service.EtiquetasPerfil;
import com.example.devcoast.service.PlanService;

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
    private final CobroRepository cobroRepository;
    private final PlanService planService;

    public PageController(UsuarioRepository usuarioRepository, EmpleoRepository empleoRepository, SolicitudRepository solicitudRepository, ProyectoRepository proyectoRepository, InvitacionRepository invitacionRepository, MongoTemplate mongoTemplate, EmpresaRepository empresaRepository, BCryptPasswordEncoder passwordEncoder, CleanupService cleanupService, CobroRepository cobroRepository, PlanService planService) {
        this.usuarioRepository = usuarioRepository;
        this.empleoRepository = empleoRepository;
        this.solicitudRepository = solicitudRepository;
        this.proyectoRepository = proyectoRepository;
        this.invitacionRepository = invitacionRepository;
        this.mongoTemplate = mongoTemplate;
        this.empresaRepository = empresaRepository;
        this.passwordEncoder = passwordEncoder;
        this.cleanupService = cleanupService;
        this.cobroRepository = cobroRepository;
        this.planService = planService;
    }

    @GetMapping("/")
    public String root(HttpSession session) {
        return RoleGuard.sinSesion(session) ? "redirect:/about-us" : "redirect:/dashboard";
    }

    @GetMapping("/perfil")
    public String perfil(HttpSession session, Model model) {
        if (RoleGuard.sinSesion(session))
            return "redirect:/login";
        cargarUsuario(session, model);
        String id = RoleGuard.getId(session);
        if (id == null)
            return "perfil";
        if (RoleGuard.esEmpresa(session)) {
            Empresa empresa = empresaRepository.findById(id).orElse(null);
            model.addAttribute("empresa", empresa);
            model.addAttribute("nActivas", empleoRepository.findByEmpresaIdAndEstadoOrderByIdDesc(id, "ACTIVO").size());
            return "perfil-empresa";
        }

        Usuario usuario = (Usuario) model.asMap().get("usuario");
        List<Proyecto> proyectosUsuario = proyectoRepository.findByUsuarioIdOrderByCreadoEnDesc(id);
        long invTotal   = invitacionRepository.findByTalentoIdOrderByFechaEnvioDesc(id).size();
        long solTotal   = solicitudRepository.countByUsuarioId(id);
        long solAcept   = solicitudRepository.countByUsuarioIdAndEstado(id, "aprobada");
        model.addAttribute("statProyectos",            proyectosUsuario.size());
        model.addAttribute("statInvitacionesTotal",    invTotal);
        model.addAttribute("statSolicitudesTotal",     solTotal);
        model.addAttribute("statSolicitudesAceptadas", solAcept);

        List<String> previewColores = proyectosUsuario.stream()
                .filter(Proyecto::isPublicado)
                .map(Proyecto::getColorIcono)
                .filter(c -> c != null && c.matches("#[0-9a-fA-F]{3,8}"))
                .distinct()
                .limit(4)
                .collect(Collectors.toList());
        long publicados = proyectosUsuario.stream().filter(Proyecto::isPublicado).count();
        if (usuario != null) {
            String mod = EtiquetasPerfil.modalidadClave(usuario.getModalidad());
            String disp = EtiquetasPerfil.disponibilidadClave(usuario.getDisponibilidad());
            String esp = EtiquetasPerfil.especialidadClave(usuario.getEspecialidad());
            model.addAttribute("selModalidad", mod);
            model.addAttribute("selDisponibilidad", disp);
            model.addAttribute("selEspecialidad", esp);
            model.addAttribute("extraModalidad", mod.isEmpty() ? sinVacios(usuario.getModalidad()) : null);
            model.addAttribute("extraDisponibilidad", disp.isEmpty() ? sinVacios(usuario.getDisponibilidad()) : null);
            model.addAttribute("extraEspecialidad", esp.isEmpty() ? sinVacios(usuario.getEspecialidad()) : null);
            model.addAttribute("preview", TalentoVista.de(usuario, (int) publicados, previewColores, usuario.isDestacar(), false));
            List<LogrosPerfil.Logro> logros = LogrosPerfil.de(usuario, proyectosUsuario.size(), invTotal, solAcept);
            model.addAttribute("logros", logros);
            model.addAttribute("logrosDesbloqueados", logros.stream().filter(LogrosPerfil.Logro::desbloqueado).count());
            model.addAttribute("pendientes", DashboardCalculos.pendientesPerfil(usuario));
        }
        return "perfil";
    }

    private static String sinVacios(String v) {
        return v == null || v.isBlank() ? null : v.trim();
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

        if (RoleGuard.esEmpresa(session)) {
            String id = RoleGuard.getId(session);
            Empresa empresa = empresaRepository.findById(id).orElse(null);
            long vacantesActivas = empleoRepository.countByEmpresaIdAndEstado(id, "ACTIVO");
            long destacadosActivos = empleoRepository.countByEmpresaIdAndDestacarTrueAndEstado(id, "ACTIVO");

            model.addAttribute("plan", empresa != null ? empresa.getPlan() : "GRATIS");
            model.addAttribute("planVenceEn", empresa != null ? empresa.getPlanVenceEn() : null);
            model.addAttribute("vacantesActivas", vacantesActivas);
            model.addAttribute("limiteVacantes", planService.limiteVacantesActivas(empresa));
            model.addAttribute("destacadosActivos", destacadosActivos);
            model.addAttribute("limiteDestacados", planService.limiteDestacados(empresa));
        }

        return "ajustes";
    }

    @GetMapping("/facturacion")
    public String facturacion(HttpSession session, Model model, RedirectAttributes ra) {
        if (RoleGuard.soloEmpresa(session)) {
            if (RoleGuard.sinSesion(session))
                return "redirect:/login";
            ra.addFlashAttribute("flashMessage", "La facturación es una función exclusiva para empresas");
            ra.addFlashAttribute("flashType", "info");
            return "redirect:/dashboard";
        }
        cargarUsuario(session, model);
        String id = RoleGuard.getId(session);
        List<com.example.devcoast.model.Cobro> cobros = cobroRepository.findByEmpresaIdOrderByFechaDesc(id);
        double total = cobros.stream().mapToDouble(com.example.devcoast.model.Cobro::getMonto).sum();
        model.addAttribute("cobros", cobros);
        model.addAttribute("totalCobrado", total);
        return "facturacion";
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
    public String registerOptions(Model model) {
        model.addAttribute("tipoPreset", null);
        return "register";
    }

    @GetMapping("/about-us")
    public String aboutUs(Model model) {
        Query recientes = new Query(new Criteria().orOperator(
                Criteria.where("estado").is("ACTIVO"), Criteria.where("estado").exists(false)))
                .with(Sort.by(Sort.Direction.DESC, "_id"))
                .limit(3);
        recientes.fields().include("titulo", "empresaNombre", "modalidad", "categoria");
        model.addAttribute("empleosLanding", mongoTemplate.find(recientes, Empleo.class));
        return "aboutUs";
    }

    @GetMapping("/perfil/preview")
    public String perfilPreview(HttpSession session, Model model) {
        if (RoleGuard.sinSesion(session))  return "redirect:/login";
        if (RoleGuard.esEmpresa(session))  return "redirect:/perfil";

        String id = RoleGuard.getId(session);
        Usuario talento = usuarioRepository.findById(id).orElse(null);
        if (talento == null) return "redirect:/perfil";

        model.addAttribute("talento", talento);
        model.addAttribute("tv", TalentoVista.de(talento, 0, null, false, false));
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

    @GetMapping("/perfil/exportar-pdf")
    public String exportarPerfilPDF(HttpSession session) {
        if (RoleGuard.sinSesion(session) || RoleGuard.esEmpresa(session))
            return "redirect:/perfil";
        return "redirect:/curriculum";
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