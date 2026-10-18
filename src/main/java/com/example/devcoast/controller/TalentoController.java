package com.example.devcoast.controller;

import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.bson.types.ObjectId;
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
import com.example.devcoast.service.EtiquetasPerfil;

import jakarta.servlet.http.HttpSession;

@Controller
public class TalentoController {

    private static final int TALENTOS_POR_PAGINA = 12;

    private final UsuarioRepository usuarioRepository;
    private final EmpleoRepository empleoRepository;
    private final EmpresaRepository empresaRepository;
    private final InvitacionRepository invitacionRepository;
    private final NotificacionRepository notificacionRepository;
    private final ProyectoRepository proyectoRepository;
    private final MongoTemplate mongoTemplate;

    public TalentoController(UsuarioRepository usuarioRepository, EmpleoRepository empleoRepository, EmpresaRepository empresaRepository, InvitacionRepository invitacionRepository, NotificacionRepository notificacionRepository, ProyectoRepository proyectoRepository, MongoTemplate mongoTemplate) {
        this.usuarioRepository = usuarioRepository;
        this.empleoRepository = empleoRepository;
        this.empresaRepository = empresaRepository;
        this.invitacionRepository = invitacionRepository;
        this.notificacionRepository = notificacionRepository;
        this.proyectoRepository = proyectoRepository;
        this.mongoTemplate = mongoTemplate;
    }

    @GetMapping("/talentos")
    public String listarTalentos(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String modalidad,
            @RequestParam(required = false) String disponibilidad,
            @RequestParam(defaultValue = "recent") String sort,
            Model model, HttpSession session) {

        if (RoleGuard.sinSesion(session))
            return "redirect:/login";

        page = Math.max(0, page);
        int pageSize = TALENTOS_POR_PAGINA;

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

        String modalidadClave = EtiquetasPerfil.modalidadClave(modalidad);
        if (!modalidadClave.isEmpty())
            criteriaList.add(Criteria.where("modalidad").regex(EtiquetasPerfil.modalidadRegex(modalidadClave), "i"));

        String disponibilidadClave = EtiquetasPerfil.disponibilidadClave(disponibilidad);
        if (!disponibilidadClave.isEmpty())
            criteriaList.add(Criteria.where("disponibilidad").regex(EtiquetasPerfil.disponibilidadRegex(disponibilidadClave), "i"));

        boolean hayFiltros = !query.isBlank() || !modalidadClave.isEmpty() || !disponibilidadClave.isEmpty();

        Criteria filtroCriteria = new Criteria().andOperator(criteriaList.toArray(Criteria[]::new));
        Sort.Direction dir = "oldest".equals(sort) ? Sort.Direction.ASC : Sort.Direction.DESC;

        Query listQuery = new Query(filtroCriteria);
        listQuery.with(Sort.by(dir, "_id"));
        long total = mongoTemplate.count(listQuery, Usuario.class);
        int totalPaginas = (int) Math.max(1, Math.ceil(total / (double) pageSize));
        page = Math.min(page, totalPaginas - 1);
        listQuery.skip((long) page * pageSize).limit(pageSize);
        List<Usuario> items = mongoTemplate.find(listQuery, Usuario.class);

        Map<String, List<Proyecto>> proyectosPorTalento = new LinkedHashMap<>();
        try {
            List<String> idsPagina = items.stream().map(Usuario::getId).toList();
            if (!idsPagina.isEmpty())
                proyectosPorTalento = proyectoRepository.findByUsuarioIdInAndPublicadoTrueOrderByCreadoEnDesc(idsPagina).stream()
                        .collect(Collectors.groupingBy(Proyecto::getUsuarioId, LinkedHashMap::new, Collectors.toList()));
        } catch (Exception ignored) {
        }

        java.util.Set<String> invitados = new java.util.HashSet<>();
        boolean esEmpresa = RoleGuard.esEmpresa(session);
        if (esEmpresa && !items.isEmpty()) {
            try {
                invitacionRepository.findByEmpresaIdAndTalentoIdInAndEmpleoIdIsNull(RoleGuard.getId(session),
                        items.stream().map(Usuario::getId).toList())
                        .forEach(i -> invitados.add(i.getTalentoId()));
            } catch (Exception ignored) {
            }
        }

        List<TalentoVista> tarjetas = new ArrayList<>();
        for (Usuario t : items) {
            List<Proyecto> ps = proyectosPorTalento.getOrDefault(t.getId(), List.of());
            List<String> colores = ps.stream().map(Proyecto::getColorIcono)
                    .filter(c -> c != null && c.matches("#[0-9a-fA-F]{3,8}")).distinct().limit(4).toList();
            tarjetas.add(TalentoVista.de(t, ps.size(), colores, t.isDestacar() && page == 0 && tarjetas.isEmpty(),
                    invitados.contains(t.getId())));
        }

        Long totalPublico = null;
        Integer pctRemoto = null;
        String especialidadTop = null;
        long nuevosEstaSemana = 0;
        try {
            Criteria basePublico = new Criteria().andOperator(
                    Criteria.where("tipo").ne("empresa"), Criteria.where("perfilPublico").is(true));
            totalPublico = mongoTemplate.count(new Query(basePublico), Usuario.class);

            if (totalPublico > 0) {
                Criteria remotoCrit = new Criteria().andOperator(basePublico,
                        Criteria.where("modalidad").regex(EtiquetasPerfil.modalidadRegex("remoto"), "i"));
                long remotos = mongoTemplate.count(new Query(remotoCrit), Usuario.class);
                pctRemoto = (int) Math.round((remotos * 100.0) / totalPublico);
            }

            Query especialidadQuery = new Query(new Criteria().andOperator(basePublico,
                    Criteria.where("especialidad").ne(null)));
            especialidadQuery.fields().include("especialidad");
            especialidadQuery.limit(400);
            Map<String, Long> conteo = mongoTemplate.find(especialidadQuery, Usuario.class).stream()
                    .map(u -> EtiquetasPerfil.especialidad(u.getEspecialidad()))
                    .filter(e -> e != null && !e.isBlank())
                    .collect(Collectors.groupingBy(e -> e, Collectors.counting()));
            especialidadTop = conteo.entrySet().stream().max(Map.Entry.comparingByValue()).map(Map.Entry::getKey).orElse(null);

            ObjectId haceUnaSemana = new ObjectId(Date.from(java.time.Instant.now().minus(java.time.Duration.ofDays(7))));
            nuevosEstaSemana = mongoTemplate.count(new Query(new Criteria().andOperator(basePublico,
                    Criteria.where("_id").gte(haceUnaSemana))), Usuario.class);
        } catch (Exception ignored) {
        }

        model.addAttribute("tarjetas", tarjetas);
        model.addAttribute("totalResultados", total);
        model.addAttribute("q", query);
        model.addAttribute("modalidadSel", modalidadClave);
        model.addAttribute("disponibilidadSel", disponibilidadClave);
        model.addAttribute("sort", "oldest".equals(sort) ? "oldest" : "recent");
        model.addAttribute("hayFiltros", hayFiltros);
        model.addAttribute("pagina", page);
        model.addAttribute("totalPaginas", totalPaginas);
        model.addAttribute("totalPublico", totalPublico);
        model.addAttribute("pctRemoto", pctRemoto);
        model.addAttribute("especialidadTop", especialidadTop);
        model.addAttribute("nuevosEstaSemana", nuevosEstaSemana);
        model.addAttribute("esEmpresa", esEmpresa);
        agregarSesion(session, model);
        return "talentos";
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

        boolean yaInvitado = false;
        if (RoleGuard.esEmpresa(session)) {
            String empresaId = RoleGuard.getId(session);
            List<Empleo> misEmpleos = empleoRepository
                    .findByEmpresaIdAndEstadoOrderByIdDesc(empresaId, "ACTIVO");
            yaInvitado = invitacionRepository
                    .existsByEmpresaIdAndTalentoIdAndEmpleoIdIsNull(empresaId, id);
            model.addAttribute("misEmpleos", misEmpleos);
            model.addAttribute("yaInvitado", yaInvitado);
        }
        model.addAttribute("tv", TalentoVista.de(talento, 0, null, false, yaInvitado));

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
