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
import com.example.devcoast.service.AreaEmpleo;
import com.example.devcoast.service.CleanupService;
import com.example.devcoast.service.MatchEmpleo;
import com.example.devcoast.service.PlanService;

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
    private final PlanService planService;

    public EmpleoController(EmpleoRepository empleoRepository, MongoTemplate mongoTemplate,
            SolicitudRepository solicitudRepository, ProyectoRepository proyectoRepository,
            UsuarioRepository usuarioRepository, EmpresaRepository empresaRepository,
            CleanupService cleanupService, PlanService planService) {
        this.empleoRepository = empleoRepository;
        this.mongoTemplate = mongoTemplate;
        this.solicitudRepository = solicitudRepository;
        this.proyectoRepository = proyectoRepository;
        this.usuarioRepository = usuarioRepository;
        this.empresaRepository = empresaRepository;
        this.cleanupService = cleanupService;
        this.planService = planService;
    }

    // ofertas por página debajo del destacado
    private static final int OTRAS_POR_PAGINA = 10;
    // tope de ofertas que se puntúan y ordenan en memoria (el match no está guardado en la base)
    private static final int CANDIDATOS_LIMITE = 600;

    // Lista de empleos como pases de abordaje. Filtros, búsqueda y orden se resuelven aquí con parámetros GET:
    // ?area=programacion|diseno|otras &modalidad=remoto|hibrido|presencial &hi=1 &q= &orden=match|recientes|salario &page=
    @GetMapping("/empleos")
    public String listarEmpleos(
            Model model, HttpSession session,
            @RequestParam(required = false) String area,
            @RequestParam(required = false) String modalidad,
            @RequestParam(required = false) String hi,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String orden,
            @RequestParam(defaultValue = "0") int page) {

        if (RoleGuard.sinSesion(session))
            return "redirect:/login";

        Usuario talento = RoleGuard.esTalento(session) ? obtenerUsuario(session) : null;
        boolean tieneStack = talento != null && talento.getStack() != null && !talento.getStack().isEmpty();

        AreaEmpleo.Area areaSel = AreaEmpleo.Area.deClave(area);
        String modSel = modalidadClave(modalidad);
        boolean soloAlto = "1".equals(hi);
        String qSel = q == null ? "" : q.trim();

        // base de datos: texto libre, modalidad y estado
        Criteria activas = new Criteria().orOperator(
                Criteria.where("estado").is("ACTIVO"), Criteria.where("estado").exists(false));
        List<Criteria> criterios = new ArrayList<>();
        criterios.add(activas);
        for (String token : qSel.split("\\s+")) {
            if (token.length() < 2)
                continue;
            String rx = ".*" + Pattern.quote(token) + ".*";
            criterios.add(new Criteria().orOperator(
                    Criteria.where("titulo").regex(rx, "i"),
                    Criteria.where("empresaNombre").regex(rx, "i"),
                    Criteria.where("descripcionRol").regex(rx, "i"),
                    Criteria.where("stack").regex(rx, "i"),
                    Criteria.where("softSkills").regex(rx, "i"),
                    Criteria.where("categoria").regex(rx, "i"),
                    Criteria.where("empresaSector").regex(rx, "i"),
                    Criteria.where("seniority").regex(rx, "i")));
        }
        if (modSel != null)
            criterios.add(Criteria.where("modalidad").regex(MODALIDAD_REGEX.get(modSel), "i"));

        long totalActivas = mongoTemplate.count(new Query(activas), Empleo.class);
        Query consulta = new Query(new Criteria().andOperator(criterios.toArray(Criteria[]::new)))
                .with(Sort.by(Sort.Direction.DESC, "_id")).limit(CANDIDATOS_LIMITE);
        List<Empleo> candidatos = mongoTemplate.find(consulta, Empleo.class);

        // en memoria: área y match (se calculan sobre los candidatos ya filtrados)
        Map<String, MatchEmpleo.Resultado> matches = new HashMap<>();
        List<Empleo> lista = new ArrayList<>();
        for (Empleo e : candidatos) {
            if (areaSel != null && AreaEmpleo.de(e.getCategoria()) != areaSel)
                continue;
            MatchEmpleo.Resultado m = MatchEmpleo.calcular(talento, e);
            if (m != null)
                matches.put(e.getId(), m);
            if (soloAlto && (m == null || m.porcentaje() < MatchEmpleo.NIVEL_ALTO))
                continue;
            lista.add(e);
        }

        boolean matchDisponible = !matches.isEmpty();
        String ordenSel = "salario".equals(orden) ? "salario" : "recientes".equals(orden) ? "recientes"
                : (matchDisponible ? "match" : "recientes");
        if ("match".equals(orden) && !matchDisponible)
            ordenSel = "recientes";
        // la lista llega por fecha (más reciente primero); sort() es estable, así que el empate conserva ese orden
        if ("match".equals(ordenSel))
            lista.sort((x, y) -> Integer.compare(
                    matches.containsKey(y.getId()) ? matches.get(y.getId()).porcentaje() : -1,
                    matches.containsKey(x.getId()) ? matches.get(x.getId()).porcentaje() : -1));
        else if ("salario".equals(ordenSel))
            lista.sort((x, y) -> Integer.compare(salarioVisible(y), salarioVisible(x)));

        // destacado: el de mayor match de lo que se ve; sin match, la oferta que la empresa promociona
        Empleo destacado = null;
        String destacadoMotivo = null;
        if (matchDisponible) {
            for (Empleo e : lista)
                if (matches.containsKey(e.getId())
                        && (destacado == null || matches.get(e.getId()).porcentaje() > matches.get(destacado.getId()).porcentaje()))
                    destacado = e;
            destacadoMotivo = "match";
        } else {
            destacado = lista.stream().filter(Empleo::isDestacar).findFirst().orElse(null);
            destacadoMotivo = "promocionado";
        }
        List<Empleo> otras = new ArrayList<>(lista);
        if (destacado != null)
            otras.remove(destacado);

        int totalPaginas = Math.max(1, (int) Math.ceil(otras.size() / (double) OTRAS_POR_PAGINA));
        int pagina = Math.min(Math.max(0, page), totalPaginas - 1);
        List<Empleo> pagItems = otras.subList(pagina * OTRAS_POR_PAGINA, Math.min(otras.size(), (pagina + 1) * OTRAS_POR_PAGINA));

        // qué ofertas de esta página ya tienen solicitud del talento
        Set<String> solicitadas = new java.util.HashSet<>();
        if (talento != null) {
            List<String> ids = new ArrayList<>(pagItems.stream().map(Empleo::getId).toList());
            if (destacado != null && pagina == 0)
                ids.add(destacado.getId());
            solicitudRepository.findByUsuarioIdAndEmpleoIdIn(talento.getId(), ids)
                    .forEach(sol -> solicitadas.add(sol.getEmpleoId()));
        }

        java.util.Date ahora = new java.util.Date();
        EmpleoVista destacadoVista = null;
        if (destacado != null && pagina == 0) {
            long postulantes = solicitudRepository.countByEmpleoId(destacado.getId());
            destacadoVista = EmpleoVista.de(destacado, matches.get(destacado.getId()), solicitadas.contains(destacado.getId()),
                    true, postulantes, ahora);
        }
        List<EmpleoVista> tarjetas = new ArrayList<>();
        for (Empleo e : pagItems)
            tarjetas.add(EmpleoVista.de(e, matches.get(e.getId()), solicitadas.contains(e.getId()), false, null, ahora));

        long conMatchAlto = lista.stream()
                .filter(e -> matches.containsKey(e.getId()) && matches.get(e.getId()).porcentaje() >= MatchEmpleo.NIVEL_ALTO)
                .count();
        boolean hayFiltros = areaSel != null || modSel != null || soloAlto || !qSel.isEmpty();

        model.addAttribute("destacado", destacadoVista);
        model.addAttribute("destacadoMotivo", destacadoMotivo);
        model.addAttribute("tarjetas", tarjetas);
        model.addAttribute("totalMostradas", lista.size());
        model.addAttribute("totalActivas", totalActivas);
        model.addAttribute("conMatchAlto", conMatchAlto);
        model.addAttribute("matchDisponible", matchDisponible);
        model.addAttribute("talentoSinStack", talento != null && !tieneStack);
        model.addAttribute("esTalento", talento != null);
        model.addAttribute("area", areaSel == null ? "" : areaSel.clave());
        model.addAttribute("modalidadSel", modSel == null ? "" : modSel);
        model.addAttribute("hi", soloAlto ? "1" : "");
        // los mismos valores como null cuando no hay filtro: así los enlaces no arrastran parámetros vacíos
        model.addAttribute("areaP", areaSel == null ? null : areaSel.clave());
        model.addAttribute("modP", modSel);
        model.addAttribute("hiP", soloAlto ? "1" : null);
        model.addAttribute("q", qSel.isEmpty() ? null : qSel);
        model.addAttribute("orden", ordenSel);
        model.addAttribute("hayFiltros", hayFiltros);
        model.addAttribute("pagina", pagina);
        model.addAttribute("totalPaginas", totalPaginas);
        agregarSesion(session, model);
        return "empleos";
    }

    private static final Map<String, String> MODALIDAD_REGEX = Map.of(
            "remoto", "remot", "hibrido", "h[ií]brid", "presencial", "presencial");

    private static String modalidadClave(String valor) {
        if (valor == null)
            return null;
        String m = java.text.Normalizer.normalize(valor.trim().toLowerCase(), java.text.Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "");
        return MODALIDAD_REGEX.containsKey(m) ? m : null;
    }

    // para ordenar por salario: solo cuenta el que la empresa decidió mostrar; el resto va al final
    private static int salarioVisible(Empleo e) {
        if (!e.isMostrarSalario())
            return -1;
        Integer v = e.getSalarioMax() != null ? e.getSalarioMax() : e.getSalarioMin();
        return v == null ? -1 : v;
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

        Empresa empresa = empresaRepository.findById(RoleGuard.getId(session)).orElse(null);
        long destacadosActivos = empleoRepository.countByEmpresaIdAndDestacarTrueAndEstado(RoleGuard.getId(session), "ACTIVO");
        model.addAttribute("destacadosActivos", destacadosActivos);
        model.addAttribute("limiteDestacados", planService.limiteDestacados(empresa));

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
        boolean yaEraDestacado = false;

        if (esEdicion) {
            Optional<Empleo> existente = empleoRepository.findById(empleoForm.getId());
            if (existente.isEmpty() || !Objects.equals(existente.get().getEmpresaId(), empresaId)) {
                ra.addFlashAttribute("flashMessage", "No tienes permiso para editar ese empleo");
                ra.addFlashAttribute("flashType", "error");
                return "redirect:/misEmpleos";
            }
            empleo = existente.get();
            yaEraDestacado = empleo.isDestacar();
        } else {
            empleo = new Empleo();
            empleo.setFechaPublicacion(new java.util.Date());
            empleo.setEmpresaId(empresaId);
            empleo.setEmpresaCorreo(RoleGuard.getCorreo(session));
        }

        Empresa empresa = empresaRepository.findById(empresaId).orElse(null);

        if (!esEdicion) {
            long activas = empleoRepository.countByEmpresaIdAndEstado(empresaId, "ACTIVO");
            if (activas >= planService.limiteVacantesActivas(empresa)) {
                ra.addFlashAttribute("flashMessage",
                        "Alcanzaste el límite de vacantes activas de tu plan. Actualiza a Pro para publicar más.");
                ra.addFlashAttribute("flashType", "error");
                return "redirect:/crearEmpleo";
            }
        }

        if (empleoForm.isDestacar() && !yaEraDestacado) {
            long destacadosActuales = empleoRepository.countByEmpresaIdAndDestacarTrueAndEstado(empresaId, "ACTIVO");
            if (destacadosActuales >= planService.limiteDestacados(empresa)) {
                ra.addFlashAttribute("flashMessage",
                        "Alcanzaste el límite de vacantes destacadas de tu plan.");
                ra.addFlashAttribute("flashType", "error");
                String idParaEditar = empleoForm.getId();
                return "redirect:/crearEmpleo" + (idParaEditar != null && !idParaEditar.isBlank() ? "?editar=" + idParaEditar : "");
            }
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

        Empresa empresa = empresaRepository.findById(empresaId).orElse(null);
        model.addAttribute("limiteVacantesActivas", planService.limiteVacantesActivas(empresa));

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

    // porcentaje de lo que pide la oferta que el talento ya tiene (0-100); ver MatchEmpleo.
    // 0 si no se puede calcular (talento o oferta sin stack)
    public static int calcularScore(Usuario usuario, Empleo empleo) {
        return com.example.devcoast.service.MatchEmpleo.porcentaje(usuario, empleo);
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