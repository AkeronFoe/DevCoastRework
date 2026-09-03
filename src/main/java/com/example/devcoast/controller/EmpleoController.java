package com.example.devcoast.controller;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.example.devcoast.config.RoleGuard;
import com.example.devcoast.model.Empleo;
import com.example.devcoast.model.Empresa;
import com.example.devcoast.model.Proyecto;
import com.example.devcoast.model.Solicitud;
import com.example.devcoast.model.Usuario;
import com.example.devcoast.repository.EmpleoRepository;
import com.example.devcoast.repository.EmpresaRepository;
import com.example.devcoast.repository.ProyectoRepository;
import com.example.devcoast.repository.SolicitudRepository;
import com.example.devcoast.repository.UsuarioRepository;
import com.example.devcoast.service.CleanupService;

import jakarta.servlet.http.HttpSession;

@Controller
public class EmpleoController {

    // 11 = 1 card destacada/mejor-match a ancho completo + 10 en pares de 2
    // columnas — así la grilla siempre cierra en filas completas en vez de
    // dejar una card sola con un hueco vacío al lado
    private static final int EMPLEOS_POR_PAGINA_DEFAULT = 11;

    private final EmpleoRepository empleoRepository;
    private final MongoTemplate mongoTemplate;
    private final SolicitudRepository solicitudRepository;
    private final ProyectoRepository proyectoRepository;
    private final UsuarioRepository usuarioRepository;
    private final EmpresaRepository empresaRepository;
    private final CleanupService cleanupService;

    public EmpleoController(EmpleoRepository empleoRepository, MongoTemplate mongoTemplate,
            SolicitudRepository solicitudRepository, ProyectoRepository proyectoRepository,
            UsuarioRepository usuarioRepository, EmpresaRepository empresaRepository,
            CleanupService cleanupService) {
        this.empleoRepository = empleoRepository;
        this.mongoTemplate = mongoTemplate;
        this.solicitudRepository = solicitudRepository;
        this.proyectoRepository = proyectoRepository;
        this.usuarioRepository = usuarioRepository;
        this.empresaRepository = empresaRepository;
        this.cleanupService = cleanupService;
    }

    @GetMapping("/empleos")
    public String listarEmpleos(
            Model model, HttpSession session,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "11") int size,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String categoria,
            @RequestParam(required = false) String modalidad,
            @RequestParam(required = false) String seniority,
            @RequestParam(required = false) String extras,
            @RequestParam(defaultValue = "match") String sort,
            @RequestParam(defaultValue = "false") boolean favoritos) {

        if (RoleGuard.sinSesion(session))
            return "redirect:/login";

        page = Math.max(0, page);
        int pageSize = size > 0 ? Math.min(size, 100) : EMPLEOS_POR_PAGINA_DEFAULT;
        List<Criteria> criteriaList = new ArrayList<>();

        // se busca antes de armar el resto de los criterios porque "solo
        // favoritos" necesita saber contra qué lista de ids filtrar — antes
        // este toggle escondía cards con display:none en el cliente, lo que
        // rompía la paginación (favoritos en la página 2 no aparecían nunca
        // parado en la página 1)
        Usuario usuarioTalento = RoleGuard.esTalento(session) ? obtenerUsuario(session) : null;

        // tokenizo por espacios y busco en varios campos a la vez
        if (q != null && !q.isBlank()) {
            for (String token : q.trim().split("\\s+")) {
                if (token.length() < 2)
                    continue;
                String rx = ".*" + Pattern.quote(token) + ".*";
                criteriaList.add(new Criteria().orOperator(
                        Criteria.where("titulo").regex(rx, "i"),
                        Criteria.where("empresaNombre").regex(rx, "i"),
                        Criteria.where("descripcionRol").regex(rx, "i"),
                        Criteria.where("stack").regex(rx, "i"),
                        Criteria.where("softSkills").regex(rx, "i"),
                        Criteria.where("categoria").regex(rx, "i"),
                        Criteria.where("empresaSector").regex(rx, "i"),
                        Criteria.where("seniority").regex(rx, "i")));
            }
        }

        if (categoria != null && !categoria.isBlank())
            criteriaList.add(Criteria.where("categoria")
                    .regex(".*" + Pattern.quote(categoria) + ".*", "i"));
        if (modalidad != null && !modalidad.isBlank())
            criteriaList.add(Criteria.where("modalidad")
                    .regex(".*" + Pattern.quote(modalidad) + ".*", "i"));
        if (seniority != null && !seniority.isBlank())
            criteriaList.add(Criteria.where("seniority")
                    .regex(".*" + Pattern.quote(seniority) + ".*", "i"));
        if (extras != null && !extras.isBlank()) {
            for (String ex : extras.split(",")) {
                if (ex.isBlank())
                    continue;
                String r = ".*" + Pattern.quote(ex.trim()) + ".*";
                criteriaList.add(new Criteria().orOperator(
                        Criteria.where("stack").regex(r, "i"),
                        Criteria.where("softSkills").regex(r, "i"),
                        Criteria.where("contrato").regex(r, "i"),
                        Criteria.where("empresaSector").regex(r, "i"),
                        Criteria.where("empresaTamano").regex(r, "i")));
            }
        }

        if (favoritos) {
            List<String> favIds = (usuarioTalento != null && usuarioTalento.getEmpleosFavoritos() != null)
                    ? usuarioTalento.getEmpleosFavoritos() : List.of();
            criteriaList.add(Criteria.where("id").in(favIds));
        }

        criteriaList.add(new Criteria().orOperator(
                Criteria.where("estado").is("ACTIVO"),
                Criteria.where("estado").exists(false)));

        Criteria filtroCriteria = new Criteria().andOperator(criteriaList.toArray(Criteria[]::new));
        Sort.Direction dir = "oldest".equals(sort) ? Sort.Direction.ASC : Sort.Direction.DESC;

        boolean tieneStack = usuarioTalento != null && usuarioTalento.getStack() != null
                && !usuarioTalento.getStack().isEmpty();
        // "match" solo es un orden real si hay contra qué puntuar — si una empresa
        // o un talento sin stack lo pide, caemos silenciosamente a "recent"
        boolean ordenarPorMatch = "match".equals(sort) && tieneStack;

        Page<Empleo> empleos;
        List<Empleo> items;
        Map<String, Integer> scores = new HashMap<>();

        if (ordenarPorMatch) {
            // Mongo no tiene el score guardado para ordenar a nivel de base de datos
            // (se calcula en Java contra el stack del talento), así que traemos un
            // lote acotado de los más recientes que ya cumplen el filtro, los
            // puntuamos y paginamos ese resultado ya ordenado por match. Acotado a
            // propósito: sin límite, con miles de empleos sembrados, esto es
            // exactamente lo que dejaba /empleos colgado la primera vez.
            int candidatosLimite = 600;
            Query matchQuery = new Query(filtroCriteria);
            matchQuery.with(Sort.by(Sort.Direction.DESC, "_id")).limit(candidatosLimite);
            List<Empleo> candidatos = mongoTemplate.find(matchQuery, Empleo.class);

            Map<String, Integer> scoresCandidatos = new HashMap<>();
            for (Empleo e : candidatos)
                scoresCandidatos.put(e.getId(), calcularScore(usuarioTalento, e));

            List<Empleo> ordenados = candidatos.stream()
                    .sorted((a, b) -> scoresCandidatos.get(b.getId()) - scoresCandidatos.get(a.getId()))
                    .toList();

            int total = ordenados.size();
            int desde = Math.min(page * pageSize, total);
            int hasta = Math.min(desde + pageSize, total);
            items = ordenados.subList(desde, hasta);
            for (Empleo e : items)
                scores.put(e.getId(), scoresCandidatos.get(e.getId()));

            empleos = new PageImpl<>(items, PageRequest.of(page, pageSize), total);
        } else {
            Query query = new Query(filtroCriteria);
            query.with(Sort.by(dir, "_id"));
            long total = mongoTemplate.count(query, Empleo.class);
            query.skip((long) page * pageSize).limit(pageSize);
            items = mongoTemplate.find(query, Empleo.class);
            empleos = new PageImpl<>(items, PageRequest.of(page, pageSize, Sort.by(dir, "_id")), total);

            if (tieneStack) {
                for (Empleo e : items)
                    scores.put(e.getId(), calcularScore(usuarioTalento, e));
            }
        }

        int totalPages = empleos.getTotalPages();
        int currentPage = empleos.getNumber();
        int startPage = Math.max(0, currentPage - 5);
        int endPage = Math.min(totalPages - 1, startPage + 9);
        if (endPage - startPage < 9)
            startPage = Math.max(0, endPage - 9);

        Integer avgScore = null;
        if (!scores.isEmpty()) {
            avgScore = (int) Math.round(
                    scores.values().stream().mapToInt(Integer::intValue).average().orElse(0));
        }

        Map<String, Boolean> yaAplico = new HashMap<>();
        if (RoleGuard.esTalento(session)) {
            String userId = RoleGuard.getId(session);
            List<String> idsPagina = items.stream().map(Empleo::getId).toList();
            Set<String> empleosAplicados = solicitudRepository
                    .findByUsuarioIdAndEmpleoIdIn(userId, idsPagina).stream()
                    .map(Solicitud::getEmpleoId)
                    .collect(Collectors.toSet());
            for (Empleo e : items)
                yaAplico.put(e.getId(), empleosAplicados.contains(e.getId()));
        } else {
            // yaAplico[id] tiene que existir para cada empleo, si no Thymeleaf revienta
            // evaluando "and" contra un null cuando la empresa mira la lista
            for (Empleo e : items)
                yaAplico.put(e.getId(), false);
        }

        // empresas contratando esta semana: última publicación activa por empresa,
        // más recientes primero — mismo dispositivo de avatares que el dashboard,
        // pero con datos reales en vez de nombres inventados. Distinct se calcula
        // en Java sobre una muestra acotada de documentos ya traídos, no con el
        // comando distinct de Mongo (documentos viejos/sembrados a mano pueden
        // traer empresaId con tipos mixtos y ese comando revienta ante eso) ni
        // sobre la colección entera (una consulta sin límite contra Atlas en
        // cada carga de /empleos es lo que dejaba la página colgada).
        List<Empleo> empresasContratando = List.of();
        long extraEmpresas = 0;
        try {
            Query empresasQuery = new Query(new Criteria().orOperator(
                    Criteria.where("estado").is("ACTIVO"), Criteria.where("estado").exists(false)));
            empresasQuery.fields().include("empresaId").include("empresaNombre").include("colorIcono");
            empresasQuery.with(Sort.by(Sort.Direction.DESC, "_id")).limit(200);
            List<Empleo> muestraActivos = mongoTemplate.find(empresasQuery, Empleo.class);

            Map<String, Empleo> porEmpresa = new LinkedHashMap<>();
            for (Empleo e : muestraActivos) {
                String clave = e.getEmpresaId() != null ? e.getEmpresaId() : e.getEmpresaNombre();
                if (clave != null)
                    porEmpresa.putIfAbsent(clave, e);
            }
            empresasContratando = porEmpresa.values().stream().limit(6).toList();
            extraEmpresas = Math.max(0, porEmpresa.size() - empresasContratando.size());
        } catch (Exception ex) {
            // no dejamos que el listado de empleos entero se caiga por este dato
            // decorativo — sin avatares de "contratando esta semana" es aceptable
        }

        model.addAttribute("empleos", empleos);
        model.addAttribute("ordenarPorMatch", ordenarPorMatch);
        model.addAttribute("matchDisponible", tieneStack);
        model.addAttribute("avgScore", avgScore);
        model.addAttribute("empresasContratando", empresasContratando);
        model.addAttribute("extraEmpresas", extraEmpresas);
        model.addAttribute("yaAplico", yaAplico);
        model.addAttribute("currentPage", currentPage);
        model.addAttribute("totalPages", totalPages);
        model.addAttribute("pageSize", pageSize);
        model.addAttribute("startPage", startPage);
        model.addAttribute("endPage", endPage);
        model.addAttribute("showStartEllipsis", startPage > 0);
        model.addAttribute("showEndEllipsis", endPage < totalPages - 1);
        model.addAttribute("q", q);
        model.addAttribute("categoria", categoria);
        model.addAttribute("modalidad", modalidad);
        model.addAttribute("seniority", seniority);
        model.addAttribute("extras", extras);
        model.addAttribute("sort", sort);
        model.addAttribute("favoritos", favoritos);
        model.addAttribute("hayFiltros",
                (q != null && !q.isBlank()) || (categoria != null && !categoria.isBlank())
                        || (modalidad != null && !modalidad.isBlank()) || (seniority != null && !seniority.isBlank())
                        || (extras != null && !extras.isBlank()) || favoritos);
        model.addAttribute("scores", scores);
        agregarSesion(session, model);
        return "empleos";
    }

    @GetMapping("/empleos/guardados")
    public String empleosGuardados(Model model, HttpSession session, RedirectAttributes ra) {
        if (RoleGuard.sinSesion(session))
            return "redirect:/login";

        if (!RoleGuard.esTalento(session)) {
            ra.addFlashAttribute("flashMessage", "Esta sección es solo para talento.");
            ra.addFlashAttribute("flashType", "warning");
            return "redirect:/dashboard";
        }

        Usuario usuario = obtenerUsuario(session);
        List<Empleo> guardados = new ArrayList<>();
        Map<String, Integer> scores = new LinkedHashMap<>();

        if (usuario != null && usuario.getEmpleosFavoritos() != null
                && !usuario.getEmpleosFavoritos().isEmpty()) {

            // más reciente primero
            List<String> ids = new ArrayList<>(usuario.getEmpleosFavoritos());
            java.util.Collections.reverse(ids);

            for (String id : ids) {
                empleoRepository.findById(id).ifPresent(e -> {
                    guardados.add(e);
                    scores.put(e.getId(), calcularScore(usuario, e));
                });
            }
        }

        Map<String, Boolean> yaAplico = new HashMap<>();
        String userId = RoleGuard.getId(session);
        for (Empleo e : guardados) {
            yaAplico.put(e.getId(),
                    solicitudRepository.findByUsuarioIdAndEmpleoId(userId, e.getId()).isPresent());
        }

        long altaCompat = guardados.stream()
                .filter(e -> scores.getOrDefault(e.getId(), 0) >= 70).count();
        long yaAplicoCount = yaAplico.values().stream().filter(Boolean::booleanValue).count();

        model.addAttribute("guardados", guardados);
        model.addAttribute("scores", scores);
        model.addAttribute("yaAplico", yaAplico);
        model.addAttribute("guardTotal", guardados.size());
        model.addAttribute("guardAltaCompat", altaCompat);
        model.addAttribute("guardYaAplico", yaAplicoCount);
        agregarSesion(session, model);
        return "empleos-guardados";
    }

    @GetMapping("/empresa/{empresaId}")
    public String perfilEmpresa(@PathVariable String empresaId,
            Model model, HttpSession session) {
        if (RoleGuard.sinSesion(session))
            return "redirect:/login";

        Optional<Empresa> empresaOpt = empresaRepository.findById(empresaId);

        List<Empleo> empleosActivos = empleoRepository
                .findByEmpresaIdAndEstadoOrderByIdDesc(empresaId, "ACTIVO");

        // preferimos los datos del registro de Empresa, y completamos con el primer Empleo si falta algo
        String nombreEmpresa, sectorEmpresa = null, webEmpresa,
                tamanoEmpresa = null, descripcionEmpresa, logoUrl;

        if (empresaOpt.isPresent()) {
            Empresa emp = empresaOpt.get();
            nombreEmpresa = emp.getNombreEmpresa();
            descripcionEmpresa = emp.getDescripcion();
            logoUrl = emp.getFotoUrl();
            webEmpresa = emp.getSitioWeb();
            sectorEmpresa = emp.getSector();
            tamanoEmpresa = emp.getTamano();
        } else if (!empleosActivos.isEmpty()) {
            Empleo ref = empleosActivos.get(0);
            nombreEmpresa = ref.getEmpresaNombre();
            sectorEmpresa = ref.getEmpresaSector();
            webEmpresa = ref.getEmpresaWeb();
            tamanoEmpresa = ref.getEmpresaTamano();
            descripcionEmpresa = ref.getEmpresaDescripcion();
            logoUrl = ref.getLogoUrl();
        } else {
            return "redirect:/empleos";
        }

        if (empresaOpt.isPresent() && !empleosActivos.isEmpty()) {
            Empleo ref = empleosActivos.get(0);
            if (sectorEmpresa == null)
                sectorEmpresa = ref.getEmpresaSector();
            if (tamanoEmpresa == null)
                tamanoEmpresa = ref.getEmpresaTamano();
            if (webEmpresa == null)
                webEmpresa = ref.getEmpresaWeb();
            if (logoUrl == null)
                logoUrl = ref.getLogoUrl();
        }

        Map<String, Integer> scores = new HashMap<>();
        if (RoleGuard.esTalento(session)) {
            Usuario usu = obtenerUsuario(session);
            if (usu != null && usu.getStack() != null && !usu.getStack().isEmpty()) {
                for (Empleo e : empleosActivos)
                    scores.put(e.getId(), calcularScore(usu, e));
            }
        }

        String inicial = (nombreEmpresa != null && !nombreEmpresa.isBlank())
                ? nombreEmpresa.substring(0, 1).toUpperCase()
                : "E";

        model.addAttribute("nombreEmpresa", nombreEmpresa);
        model.addAttribute("sectorEmpresa", sectorEmpresa);
        model.addAttribute("webEmpresa", webEmpresa);
        model.addAttribute("tamanoEmpresa", tamanoEmpresa);
        model.addAttribute("descripcionEmpresa", descripcionEmpresa);
        model.addAttribute("logoUrl", logoUrl);
        model.addAttribute("inicial", inicial);
        model.addAttribute("empleosActivos", empleosActivos);
        model.addAttribute("scores", scores);
        model.addAttribute("empresaId", empresaId);
        agregarSesion(session, model);
        return "empresa-perfil";
    }

    @GetMapping("/empleos/{id}")
    public String verDetalle(@PathVariable String id, Model model, HttpSession session) {
        if (RoleGuard.sinSesion(session))
            return "redirect:/login";

        Optional<Empleo> opt = empleoRepository.findById(id);
        if (opt.isEmpty())
            return "redirect:/empleos";

        Empleo empleo = opt.get();
        String usuarioId = RoleGuard.getId(session);
        boolean esEmpresaPropietaria = RoleGuard.esEmpresa(session)
                && empleo.getEmpresaId() != null
                && empleo.getEmpresaId().equals(usuarioId);

        boolean usuarioYaAplico = false;
        int score = 0;

        if (RoleGuard.esTalento(session)) {
            usuarioYaAplico = solicitudRepository
                    .findByUsuarioIdAndEmpleoId(usuarioId, id).isPresent();
            List<Proyecto> misProyectos = proyectoRepository
                    .findByUsuarioIdOrderByCreadoEnDesc(usuarioId);
            model.addAttribute("misProyectos", misProyectos);

            Usuario usu = obtenerUsuario(session);
            if (usu != null && usu.getStack() != null && !usu.getStack().isEmpty()) {
                score = calcularScore(usu, empleo);
            }
        }

        if (esEmpresaPropietaria) {
            List<Solicitud> solicitudesEmpleo = solicitudRepository
                    .findByEmpleoIdOrderByFechaSolicitudDesc(id);
            model.addAttribute("solicitudesEmpleo", solicitudesEmpleo);
            model.addAttribute("totalSolicitudesEmpleo", solicitudesEmpleo.size());
        }

        model.addAttribute("empleo", empleo);
        model.addAttribute("usuarioYaAplico", usuarioYaAplico);
        model.addAttribute("esEmpresaPropietaria", esEmpresaPropietaria);
        model.addAttribute("score", score);
        agregarSesion(session, model);
        return "detalles-empleo";
    }

    @GetMapping("/crearEmpleo")
    public String mostrarFormulario(@RequestParam(required = false) String editar,
            Model model, HttpSession session, RedirectAttributes ra) {
        if (RoleGuard.soloEmpresa(session)) {
            if (RoleGuard.sinSesion(session))
                return "redirect:/login";
            ra.addFlashAttribute("flashMessage", "Solo las empresas pueden publicar empleos");
            ra.addFlashAttribute("flashType", "warning");
            return "redirect:/empleos";
        }

        Empleo empleo = new Empleo();
        if (editar != null && !editar.isBlank()) {
            Optional<Empleo> existente = empleoRepository.findById(editar);
            if (existente.isEmpty() || !Objects.equals(existente.get().getEmpresaId(), RoleGuard.getId(session))) {
                ra.addFlashAttribute("flashMessage", "No tienes permiso para editar ese empleo");
                ra.addFlashAttribute("flashType", "error");
                return "redirect:/misEmpleos";
            }
            empleo = existente.get();
        }

        model.addAttribute("empleo", empleo);
        agregarSesion(session, model);
        return "crearEmpleo";
    }

    @PostMapping("/empleos/guardar")
    public String guardarEmpleo(@ModelAttribute Empleo empleoForm, HttpSession session, RedirectAttributes ra) {
        if (RoleGuard.soloEmpresa(session))
            return "redirect:/login";

        String errorValidacion = validarEmpleo(empleoForm);
        if (errorValidacion != null) {
            ra.addFlashAttribute("flashMessage", errorValidacion);
            ra.addFlashAttribute("flashType", "error");
            String idParaEditar = empleoForm.getId();
            return "redirect:/crearEmpleo" + (idParaEditar != null && !idParaEditar.isBlank() ? "?editar=" + idParaEditar : "");
        }

        String empresaId = RoleGuard.getId(session);
        boolean esEdicion = empleoForm.getId() != null && !empleoForm.getId().isBlank();
        Empleo empleo;

        if (esEdicion) {
            Optional<Empleo> existente = empleoRepository.findById(empleoForm.getId());
            if (existente.isEmpty() || !Objects.equals(existente.get().getEmpresaId(), empresaId)) {
                ra.addFlashAttribute("flashMessage", "No tienes permiso para editar ese empleo");
                ra.addFlashAttribute("flashType", "error");
                return "redirect:/misEmpleos";
            }
            empleo = existente.get();
        } else {
            empleo = new Empleo();
            empleo.setEmpresaId(empresaId);
            empleo.setEmpresaCorreo(RoleGuard.getCorreo(session));
        }

        aplicarCamposFormulario(empleo, empleoForm);
        empleoRepository.save(empleo);

        ra.addFlashAttribute("flashMessage", esEdicion ? "Empleo actualizado correctamente" : "Empleo publicado correctamente");
        ra.addFlashAttribute("flashType", "success");
        return "redirect:/misEmpleos";
    }

    // null si está OK, si no devuelve el mensaje de error
    private String validarEmpleo(Empleo f) {
        if (esVacio(f.getTitulo()) || esVacio(f.getCategoria()) || esVacio(f.getSeniority())
                || esVacio(f.getModalidad()) || esVacio(f.getContrato()) || esVacio(f.getEmpresaNombre())
                || esVacio(f.getEmpresaSector()) || esVacio(f.getEmpresaTamano()) || esVacio(f.getEmpresaDescripcion())
                || esVacio(f.getStack())) {
            return "Completa todos los campos obligatorios del empleo";
        }
        if (f.getVacantes() == null || f.getVacantes() < 1) {
            return "Las vacantes disponibles deben ser al menos 1";
        }
        if (f.getSalarioMin() != null && f.getSalarioMax() != null && f.getSalarioMin() > f.getSalarioMax()) {
            return "El salario mínimo no puede ser mayor que el máximo";
        }
        return null;
    }

    private boolean esVacio(String s) {
        return s == null || s.isBlank();
    }

    // ojo: nunca toca id/empresaId/empresaCorreo/estado/faseReclutamiento a propósito,
    // esos campos se manejan aparte (sesión o endpoints dedicados) y no deberían venir del POST
    private void aplicarCamposFormulario(Empleo target, Empleo form) {
        target.setEmpresaNombre(form.getEmpresaNombre());
        target.setEmpresaSector(form.getEmpresaSector());
        target.setEmpresaWeb(form.getEmpresaWeb());
        target.setEmpresaTamano(form.getEmpresaTamano());
        target.setEmpresaDescripcion(form.getEmpresaDescripcion());
        target.setIcono(form.getIcono());
        target.setColorIcono(form.getColorIcono());
        target.setTitulo(form.getTitulo());
        target.setCategoria(form.getCategoria());
        target.setSeniority(form.getSeniority());
        target.setModalidad(form.getModalidad());
        target.setContrato(form.getContrato());
        target.setVacantes(form.getVacantes());
        target.setMoneda(form.getMoneda());
        target.setSalarioMin(form.getSalarioMin());
        target.setSalarioMax(form.getSalarioMax());
        target.setSalarioPeriodo(form.getSalarioPeriodo());
        target.setTimezone(form.getTimezone());
        target.setIngles(form.getIngles());
        target.setDescripcionRol(form.getDescripcionRol());
        target.setRequisitosDeseables(form.getRequisitosDeseables());
        target.setStack(form.getStack());
        target.setSoftSkills(form.getSoftSkills());
        target.setMostrarSalario(form.isMostrarSalario());
        target.setMatchAutomatico(form.isMatchAutomatico());
        target.setDestacar(form.isDestacar());
        target.setNotificacionesEmail(form.isNotificacionesEmail());
        target.setScreeningActivo(form.isScreeningActivo());
        target.setScreeningKeywords(form.getScreeningKeywords());
        target.setLogoUrl(form.getLogoUrl());
    }

    @GetMapping("/misEmpleos")
    public String misEmpleos(Model model, HttpSession session, RedirectAttributes ra) {
        if (RoleGuard.soloEmpresa(session)) {
            if (RoleGuard.sinSesion(session))
                return "redirect:/login";
            ra.addFlashAttribute("flashMessage", "Solo las empresas tienen acceso a esta sección");
            ra.addFlashAttribute("flashType", "warning");
            return "redirect:/dashboard";
        }
        String empresaId = RoleGuard.getId(session);
        model.addAttribute("empleosActivos",
                empleoRepository.findByEmpresaIdAndEstadoOrderByIdDesc(empresaId, "ACTIVO"));
        model.addAttribute("empleosCerrados",
                empleoRepository.findByEmpresaIdAndEstadoOrderByIdDesc(empresaId, "CERRADO"));
        agregarSesion(session, model);
        return "misEmpleos";
    }

    @PostMapping("/empleos/cerrar/{id}")
    public String cerrarEmpleo(@PathVariable String id, HttpSession session, RedirectAttributes ra) {
        if (RoleGuard.soloEmpresa(session))
            return "redirect:/login";
        empleoRepository.findById(id).ifPresent(e -> {
            if (Objects.equals(e.getEmpresaId(), RoleGuard.getId(session))) {
                e.setEstado("CERRADO");
                empleoRepository.save(e);
                ra.addFlashAttribute("flashMessage", "Empleo cerrado correctamente");
                ra.addFlashAttribute("flashType", "success");
            }
        });
        return "redirect:/misEmpleos";
    }

    @PostMapping("/empleos/reabrir/{id}")
    public String reabrirEmpleo(@PathVariable String id, HttpSession session, RedirectAttributes ra) {
        if (RoleGuard.soloEmpresa(session))
            return "redirect:/login";
        empleoRepository.findById(id).ifPresent(e -> {
            if (Objects.equals(e.getEmpresaId(), RoleGuard.getId(session))) {
                e.setEstado("ACTIVO");
                empleoRepository.save(e);
                ra.addFlashAttribute("flashMessage", "Empleo reabierto correctamente");
                ra.addFlashAttribute("flashType", "success");
            }
        });
        return "redirect:/misEmpleos";
    }

    @PostMapping("/empleos/eliminar/{id}")
    public String eliminarEmpleo(@PathVariable String id, HttpSession session, RedirectAttributes ra) {
        if (RoleGuard.soloEmpresa(session))
            return "redirect:/login";
        empleoRepository.findById(id).ifPresent(e -> {
            if (Objects.equals(e.getEmpresaId(), RoleGuard.getId(session))) {
                cleanupService.limpiarDatosDeEmpleo(id);
                empleoRepository.deleteById(id);
                ra.addFlashAttribute("flashMessage", "Empleo eliminado permanentemente");
                ra.addFlashAttribute("flashType", "success");
            }
        });
        return "redirect:/misEmpleos";
    }

    @PostMapping(value = "/empleos/{id}/fase", produces = "application/json")
    @ResponseBody
    public ResponseEntity<?> cambiarFaseReclutamiento(
            @PathVariable String id,
            @RequestParam String fase,
            HttpSession session) {

        if (RoleGuard.sinSesion(session))
            return ResponseEntity.status(401).body(Map.of("error", "Sin sesión"));

        List<String> fasesValidas = List.of("ABIERTO", "SCREENING", "ENTREVISTAS", "OFERTA", "CERRADO_PROCESO");
        if (!fasesValidas.contains(fase))
            return ResponseEntity.badRequest().body(Map.of("error", "Fase inválida"));

        Optional<Empleo> opt = empleoRepository.findById(id);
        if (opt.isEmpty())
            return ResponseEntity.notFound().build();

        Empleo empleo = opt.get();
        if (!Objects.equals(empleo.getEmpresaId(), RoleGuard.getId(session)))
            return ResponseEntity.status(403).body(Map.of("error", "Sin permiso"));

        empleo.setFaseReclutamiento(fase);
        empleoRepository.save(empleo);
        return ResponseEntity.ok(Map.of("ok", true, "fase", fase));
    }

    // cuenta cuántas skills del talento aparecen en el texto del empleo, devuelve 0-100
    public static int calcularScore(Usuario usuario, Empleo empleo) {
        if (usuario == null || usuario.getStack() == null || usuario.getStack().isEmpty())
            return 0;

        String textoEmpleo = String.join(" ",
                empleo.getStack() != null ? empleo.getStack() : "",
                empleo.getSoftSkills() != null ? empleo.getSoftSkills() : "",
                empleo.getCategoria() != null ? empleo.getCategoria() : "",
                empleo.getSeniority() != null ? empleo.getSeniority() : "",
                empleo.getDescripcionRol() != null ? empleo.getDescripcionRol() : "").toLowerCase();

        long matches = usuario.getStack().stream()
                .filter(s -> s != null && !s.isBlank())
                .filter(s -> textoEmpleo.contains(s.toLowerCase().trim()))
                .count();

        return (int) (matches * 100L / usuario.getStack().size());
    }

    private Usuario obtenerUsuario(HttpSession session) {
        String id = RoleGuard.getId(session);
        if (id == null)
            return null;
        return usuarioRepository.findById(id).orElse(null);
    }

    private void agregarSesion(HttpSession session, Model model) {
        model.addAttribute("usuarioActualId", RoleGuard.getId(session));
        model.addAttribute("usuarioActualCorreo", RoleGuard.getCorreo(session));
        model.addAttribute("usuarioActualNombre", RoleGuard.getNombre(session));
        model.addAttribute("usuarioActualTipo", RoleGuard.getTipo(session));
        model.addAttribute("usuarioActualFotoUrl", session.getAttribute("usuarioActualFotoUrl"));

        if (RoleGuard.esTalento(session)) {
            String id = RoleGuard.getId(session);
            if (id != null) {
                usuarioRepository.findById(id).ifPresent(u -> {
                    if (u.getEmpleosFavoritos() == null)
                        u.setEmpleosFavoritos(new ArrayList<>());
                    model.addAttribute("usuario", u);
                });
            }
        }
    }
}