package com.example.devcoast.controller;

import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.bson.types.ObjectId;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.example.devcoast.config.RoleGuard;
import com.example.devcoast.model.Empleo;
import com.example.devcoast.model.Empresa;
import com.example.devcoast.model.Invitacion;
import com.example.devcoast.model.Notificacion;
import com.example.devcoast.model.Proyecto;
import com.example.devcoast.model.Usuario;
import com.example.devcoast.repository.EmpleoRepository;
import com.example.devcoast.repository.EmpresaRepository;
import com.example.devcoast.repository.InvitacionRepository;
import com.example.devcoast.repository.NotificacionRepository;
import com.example.devcoast.repository.ProyectoRepository;
import com.example.devcoast.repository.UsuarioRepository;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
public class TalentoController {

    // 13 = 1 destacado a ancho completo + 12 en grilla de 3 columnas — cierra
    // en filas completas en vez de dejar una card sola con hueco al lado
    private static final int TALENTOS_POR_PAGINA_DEFAULT = 13;

    // Usuario no tiene un colorIcono propio (a diferencia de Empleo/Proyecto),
    // así que el acento de cada card sale de un hash determinista del id sobre
    // esta misma paleta — el mismo talento siempre cae en el mismo color
    private static final String[] PALETA_ACENTOS = { "#4fa0e8", "#a78bfa", "#f2ab5c", "#ef7b92", "#7bd8ae" };

    private static String colorParaId(String id) {
        int idx = Math.floorMod(id == null ? 0 : id.hashCode(), PALETA_ACENTOS.length);
        return PALETA_ACENTOS[idx];
    }

    private final UsuarioRepository usuarioRepository;
    private final EmpleoRepository empleoRepository;
    private final EmpresaRepository empresaRepository;
    private final InvitacionRepository invitacionRepository;
    private final NotificacionRepository notificacionRepository;
    private final ProyectoRepository proyectoRepository;
    private final MongoTemplate mongoTemplate;

    @GetMapping("/talentos")
    public String listarTalentos(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "13") int size,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String modalidad,
            @RequestParam(required = false) String disponibilidad,
            @RequestParam(defaultValue = "recent") String sort,
            Model model, HttpSession session, HttpServletRequest request) {

        if (RoleGuard.sinSesion(session))
            return "redirect:/login";

        page = Math.max(0, page);
        int pageSize = size > 0 ? Math.min(size, 100) : TALENTOS_POR_PAGINA_DEFAULT;

        List<Criteria> criteriaList = new ArrayList<>();
        criteriaList.add(Criteria.where("tipo").ne("empresa"));
        criteriaList.add(Criteria.where("perfilPublico").is(true));

        String query = q == null ? "" : q.trim();
        if (!query.isBlank()) {
            for (String token : query.split("\\s+")) {
                if (token.length() < 2)
                    continue;
                String rx = ".*" + Pattern.quote(token) + ".*";
                criteriaList.add(new Criteria().orOperator(
                        Criteria.where("nombre").regex(rx, "i"),
                        Criteria.where("rolTitulo").regex(rx, "i"),
                        Criteria.where("especialidad").regex(rx, "i"),
                        Criteria.where("stack").regex(rx, "i"),
                        Criteria.where("bio").regex(rx, "i")));
            }
        }

        String modalidadFiltro = modalidad == null ? "" : modalidad.trim();
        if (!modalidadFiltro.isBlank())
            criteriaList.add(Criteria.where("modalidad").regex("^" + Pattern.quote(modalidadFiltro) + "$", "i"));

        String disponibilidadFiltro = disponibilidad == null ? "" : disponibilidad.trim();
        if (!disponibilidadFiltro.isBlank())
            criteriaList.add(
                    Criteria.where("disponibilidad").regex("^" + Pattern.quote(disponibilidadFiltro) + "$", "i"));

        boolean hayFiltros = !query.isBlank() || !modalidadFiltro.isBlank() || !disponibilidadFiltro.isBlank();

        Criteria filtroCriteria = new Criteria().andOperator(criteriaList.toArray(Criteria[]::new));
        Sort.Direction dir = "oldest".equals(sort) ? Sort.Direction.ASC : Sort.Direction.DESC;

        Query listQuery = new Query(filtroCriteria);
        listQuery.with(Sort.by(dir, "_id"));
        long total = mongoTemplate.count(listQuery, Usuario.class);
        listQuery.skip((long) page * pageSize).limit(pageSize);
        List<Usuario> items = mongoTemplate.find(listQuery, Usuario.class);

        Page<Usuario> talentos = new PageImpl<>(items, PageRequest.of(page, pageSize, Sort.by(dir, "_id")), total);

        int totalPages = talentos.getTotalPages();
        int currentPage = talentos.getNumber();
        int startPage = Math.max(0, currentPage - 5);
        int endPage = Math.min(totalPages - 1, startPage + 9);
        if (endPage - startPage < 9)
            startPage = Math.max(0, endPage - 9);

        // portafolio real por talento de esta página — una sola consulta batched
        // en vez de N+1 (una por card), agrupada en Java por usuarioId
        Map<String, List<String>> coloresPorTalento = new LinkedHashMap<>();
        try {
            List<String> idsPagina = items.stream().map(Usuario::getId).toList();
            if (!idsPagina.isEmpty()) {
                List<Proyecto> proyectos = proyectoRepository
                        .findByUsuarioIdInAndPublicadoTrueOrderByCreadoEnDesc(idsPagina);
                Map<String, List<Proyecto>> porUsuario = proyectos.stream()
                        .collect(Collectors.groupingBy(Proyecto::getUsuarioId, LinkedHashMap::new,
                                Collectors.toList()));
                for (Map.Entry<String, List<Proyecto>> e : porUsuario.entrySet()) {
                    List<String> colores = e.getValue().stream()
                            .map(Proyecto::getColorIcono)
                            .filter(c -> c != null && !c.isBlank())
                            .distinct()
                            .limit(4)
                            .toList();
                    if (!colores.isEmpty())
                        coloresPorTalento.put(e.getKey(), colores);
                }
            }
        } catch (Exception ignored) {
            // el portafolio es decorativo acá — un fallo no debe tumbar el directorio
        }

        // insights del directorio — cada uno es decorativo, envuelto por separado
        // para que un fallo puntual no rompa el resto de la página
        Long totalPublico = null;
        Integer pctRemoto = null;
        String especialidadTop = null;
        Long especialidadTopCount = null;
        long nuevosEstaSemana = 0;
        List<Usuario> recienLlegados = List.of();
        long extraRecienLlegados = 0;

        try {
            Criteria basePublico = new Criteria().andOperator(
                    Criteria.where("tipo").ne("empresa"), Criteria.where("perfilPublico").is(true));
            totalPublico = mongoTemplate.count(new Query(basePublico), Usuario.class);

            if (totalPublico > 0) {
                Criteria remotoCrit = new Criteria().andOperator(basePublico,
                        Criteria.where("modalidad").regex(".*remoto.*", "i"));
                long remotos = mongoTemplate.count(new Query(remotoCrit), Usuario.class);
                pctRemoto = (int) Math.round((remotos * 100.0) / totalPublico);
            }

            Query especialidadQuery = new Query(new Criteria().andOperator(basePublico,
                    Criteria.where("especialidad").ne(null)));
            especialidadQuery.fields().include("especialidad");
            especialidadQuery.limit(400);
            List<Usuario> muestraEspecialidad = mongoTemplate.find(especialidadQuery, Usuario.class);
            Map<String, Long> conteoEspecialidad = muestraEspecialidad.stream()
                    .map(Usuario::getEspecialidad)
                    .filter(e -> e != null && !e.isBlank())
                    .collect(Collectors.groupingBy(String::trim, Collectors.counting()));
            var topEntry = conteoEspecialidad.entrySet().stream()
                    .max(Map.Entry.comparingByValue());
            if (topEntry.isPresent()) {
                especialidadTop = topEntry.get().getKey();
                especialidadTopCount = topEntry.get().getValue();
            }

            ObjectId haceUnaSemana = new ObjectId(Date.from(
                    java.time.Instant.now().minus(java.time.Duration.ofDays(7))));
            Criteria nuevosCrit = new Criteria().andOperator(basePublico,
                    Criteria.where("_id").gte(haceUnaSemana));
            nuevosEstaSemana = mongoTemplate.count(new Query(nuevosCrit), Usuario.class);

            Query recientesQuery = new Query(basePublico);
            recientesQuery.with(Sort.by(Sort.Direction.DESC, "_id")).limit(9);
            List<Usuario> muestraRecientes = mongoTemplate.find(recientesQuery, Usuario.class);
            recienLlegados = muestraRecientes.stream().limit(6).toList();
            extraRecienLlegados = Math.max(0, muestraRecientes.size() - recienLlegados.size());
        } catch (Exception ignored) {
            // insights decorativos — no deben tumbar el listado principal
        }

        Map<String, String> avatarColorPorTalento = new LinkedHashMap<>();
        for (Usuario t : items)
            avatarColorPorTalento.put(t.getId(), colorParaId(t.getId()));
        for (Usuario t : recienLlegados)
            avatarColorPorTalento.putIfAbsent(t.getId(), colorParaId(t.getId()));

        // qué talentos de esta página ya fueron invitados (sin empleo puntual)
        // por la empresa en sesión — evita que "Invitar" duplique solicitudes
        Map<String, Boolean> yaInvitado = new LinkedHashMap<>();
        if (RoleGuard.esEmpresa(session)) {
            String empresaId = RoleGuard.getId(session);
            List<String> idsPagina = items.stream().map(Usuario::getId).toList();
            try {
                var invitados = invitacionRepository
                        .findByEmpresaIdAndTalentoIdInAndEmpleoIdIsNull(empresaId, idsPagina).stream()
                        .map(Invitacion::getTalentoId)
                        .collect(Collectors.toSet());
                for (Usuario t : items)
                    yaInvitado.put(t.getId(), invitados.contains(t.getId()));
            } catch (Exception ignored) {
                for (Usuario t : items)
                    yaInvitado.put(t.getId(), false);
            }
        } else {
            for (Usuario t : items)
                yaInvitado.put(t.getId(), false);
        }

        model.addAttribute("talentos", talentos);
        model.addAttribute("yaInvitado", yaInvitado);
        model.addAttribute("avatarColorPorTalento", avatarColorPorTalento);
        model.addAttribute("q", query);
        model.addAttribute("modalidad", modalidadFiltro.isBlank() ? null : modalidadFiltro);
        model.addAttribute("disponibilidad", disponibilidadFiltro.isBlank() ? null : disponibilidadFiltro);
        model.addAttribute("sort", sort);
        model.addAttribute("hayFiltros", hayFiltros);
        model.addAttribute("currentPage", currentPage);
        model.addAttribute("totalPages", totalPages);
        model.addAttribute("pageSize", pageSize);
        model.addAttribute("startPage", startPage);
        model.addAttribute("endPage", endPage);
        model.addAttribute("showStartEllipsis", startPage > 0);
        model.addAttribute("showEndEllipsis", endPage < totalPages - 1);
        model.addAttribute("coloresPorTalento", coloresPorTalento);
        model.addAttribute("totalTalentosPublicos", totalPublico);
        model.addAttribute("pctRemoto", pctRemoto);
        model.addAttribute("especialidadTop", especialidadTop);
        model.addAttribute("especialidadTopCount", especialidadTopCount);
        model.addAttribute("nuevosEstaSemana", nuevosEstaSemana);
        model.addAttribute("recienLlegados", recienLlegados);
        model.addAttribute("extraRecienLlegados", extraRecienLlegados);
        model.addAttribute("esEmpresa", RoleGuard.esEmpresa(session));
        agregarSesion(session, model);

        // la paginación de /talentos pide el fragmento por fetch en vez de
        // navegar completo, igual que /empleos — el header lo controla el
        // propio fetch() del cliente, no falsificable pegando la URL
        boolean esAjax = "XMLHttpRequest".equals(request.getHeader("X-Requested-With"));
        return esAjax ? "talentos :: resultadosTalentos" : "talentos";
    }

    @GetMapping("/talentos/{id}")
    public String verTalento(@PathVariable String id, Model model, HttpSession session) {
        if (RoleGuard.sinSesion(session))
            return "redirect:/login";

        Usuario talento = usuarioRepository.findById(id).orElse(null);
        if (talento == null || !talento.isPerfilPublico())
            return "redirect:/talentos";

        boolean permiteContacto = talento.getPermitirMensajes() == null || talento.getPermitirMensajes();

        model.addAttribute("talento", talento);
        model.addAttribute("esEmpresa", RoleGuard.esEmpresa(session));
        model.addAttribute("permiteContacto", permiteContacto);
        agregarSesion(session, model);

        if (RoleGuard.esEmpresa(session)) {
            String empresaId = RoleGuard.getId(session);
            List<Empleo> misEmpleos = empleoRepository
                    .findByEmpresaIdAndEstadoOrderByIdDesc(empresaId, "ACTIVO");
            boolean yaInvitado = invitacionRepository
                    .existsByEmpresaIdAndTalentoIdAndEmpleoIdIsNull(empresaId, id);
            model.addAttribute("misEmpleos", misEmpleos);
            model.addAttribute("yaInvitado", yaInvitado);
        }

        return "talento-perfil";
    }

    @PostMapping("/talentos/{talentoId}/invitar")
    public String invitarTalento(
            @PathVariable String talentoId,
            @RequestParam(required = false) String empleoId,
            @RequestParam(required = false) String mensaje,
            HttpSession session, RedirectAttributes ra) {

        if (RoleGuard.soloEmpresa(session)) {
            ra.addFlashAttribute("flashMessage", "Solo las empresas pueden invitar talentos");
            ra.addFlashAttribute("flashType", "error");
            return "redirect:/talentos/" + talentoId;
        }

        String empresaId = RoleGuard.getId(session);
        Usuario talento = usuarioRepository.findById(talentoId).orElse(null);
        if (talento == null || !talento.isPerfilPublico()) {
            ra.addFlashAttribute("flashMessage", "Talento no disponible");
            ra.addFlashAttribute("flashType", "error");
            return "redirect:/talentos";
        }

        boolean permiteContacto = talento.getPermitirMensajes() == null || talento.getPermitirMensajes();
        if (!permiteContacto) {
            ra.addFlashAttribute("flashMessage", "Este talento no acepta invitaciones directas");
            ra.addFlashAttribute("flashType", "error");
            return "redirect:/talentos/" + talentoId;
        }

        boolean duplicado = (empleoId != null && !empleoId.isBlank())
                ? invitacionRepository.existsByEmpresaIdAndTalentoIdAndEmpleoId(empresaId, talentoId, empleoId)
                : invitacionRepository.existsByEmpresaIdAndTalentoIdAndEmpleoIdIsNull(empresaId, talentoId);

        if (duplicado) {
            ra.addFlashAttribute("flashMessage", "Ya enviaste una invitación a este talento");
            ra.addFlashAttribute("flashType", "info");
            return "redirect:/talentos/" + talentoId;
        }

        Empresa empresa = empresaRepository.findById(empresaId).orElse(null);

        Invitacion inv = new Invitacion();
        inv.setTalentoId(talentoId);
        inv.setTalentoNombre(talento.getNombre());
        inv.setTalentoCorreo(talento.getCorreo());
        inv.setEmpresaId(empresaId);
        inv.setEstado("pendiente");
        inv.setFechaEnvio(new Date());

        if (empresa != null) {
            inv.setEmpresaNombre(empresa.getNombreEmpresa());
            inv.setEmpresaSector(empresa.getSector());
            inv.setEmpresaWeb(empresa.getSitioWeb());
            inv.setEmpresaDescripcion(empresa.getDescripcion());
            inv.setEmpresaTamano(empresa.getTamano());
        }

        if (empleoId != null && !empleoId.isBlank()) {
            empleoRepository.findById(empleoId)
                    .filter(e -> java.util.Objects.equals(e.getEmpresaId(), empresaId))
                    .ifPresent(e -> {
                        inv.setEmpleoId(e.getId());
                        inv.setEmpleoTitulo(e.getTitulo());
                        inv.setEmpleoModalidad(e.getModalidad());
                        inv.setEmpleoContrato(e.getContrato());
                        inv.setEmpleoCategoria(e.getCategoria());
                        inv.setEmpleoSalarioMin(e.getSalarioMin());
                        inv.setEmpleoSalarioMax(e.getSalarioMax());
                        inv.setEmpleoMoneda(e.getMoneda());
                        inv.setEmpleoMostrarSalario(e.isMostrarSalario());
                    });
        }

        if (mensaje != null && !mensaje.isBlank())
            inv.setMensaje(mensaje.trim());

        invitacionRepository.save(inv);

        try {
            Notificacion notif = new Notificacion();
            notif.setDestinatarioId(talentoId);
            notif.setTipo("NUEVA_INVITACION");
            notif.setTitulo("Nueva invitación de " + (empresa != null ? empresa.getNombreEmpresa() : "una empresa"));
            notif.setMensaje(inv.getEmpleoTitulo() != null
                    ? "Te invitaron a aplicar a " + inv.getEmpleoTitulo()
                    : "Te contactaron para explorar oportunidades");
            notif.setFechaCreacion(new java.util.Date());
            notif.setUrl("/solicitudes?tab=invitaciones");
            notificacionRepository.save(notif);
        } catch (Exception ignored) {
        }

        ra.addFlashAttribute("flashMessage", "Invitación enviada a " + talento.getNombre());
        ra.addFlashAttribute("flashType", "success");
        return "redirect:/talentos/" + talentoId;
    }

    private void agregarSesion(HttpSession session, Model model) {
        model.addAttribute("usuarioActualId", RoleGuard.getId(session));
        model.addAttribute("usuarioActualCorreo", RoleGuard.getCorreo(session));
        model.addAttribute("usuarioActualNombre", RoleGuard.getNombre(session));
        model.addAttribute("usuarioActualTipo", RoleGuard.getTipo(session));
        model.addAttribute("usuarioActualFotoUrl", session.getAttribute("usuarioActualFotoUrl"));

        if (RoleGuard.esTalento(session)) {
            String id = RoleGuard.getId(session);
            if (id != null)
                usuarioRepository.findById(id).ifPresent(u -> model.addAttribute("usuario", u));
        }
    }
}
