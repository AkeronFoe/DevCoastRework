package com.example.devcoast.controller;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
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
import com.example.devcoast.model.Empleo;
import com.example.devcoast.model.Empresa;
import com.example.devcoast.model.Mensaje;
import com.example.devcoast.model.Notificacion;
import com.example.devcoast.model.Solicitud;
import com.example.devcoast.model.Usuario;
import com.example.devcoast.repository.EmpleoRepository;
import com.example.devcoast.repository.EmpresaRepository;
import com.example.devcoast.repository.MensajeRepository;
import com.example.devcoast.repository.NotificacionRepository;
import com.example.devcoast.repository.SolicitudRepository;
import com.example.devcoast.repository.UsuarioRepository;

import jakarta.servlet.http.HttpSession;

@Controller
public class MensajeController {

    private static final int CHAT_PAGE_SIZE = 50;
    private static final int LOAD_MORE_SIZE = 20;

    private final SolicitudRepository solicitudRepository;
    private final EmpleoRepository empleoRepository;
    private final MensajeRepository mensajeRepository;
    private final NotificacionRepository notificacionRepository;
    private final UsuarioRepository usuarioRepository;
    private final EmpresaRepository empresaRepository;
    private final MongoTemplate mongoTemplate;

    public MensajeController(SolicitudRepository solicitudRepository, EmpleoRepository empleoRepository,
            MensajeRepository mensajeRepository, NotificacionRepository notificacionRepository,
            UsuarioRepository usuarioRepository, EmpresaRepository empresaRepository, MongoTemplate mongoTemplate) {
        this.solicitudRepository = solicitudRepository;
        this.empleoRepository = empleoRepository;
        this.mensajeRepository = mensajeRepository;
        this.notificacionRepository = notificacionRepository;
        this.usuarioRepository = usuarioRepository;
        this.empresaRepository = empresaRepository;
        this.mongoTemplate = mongoTemplate;
    }

    @GetMapping("/mensajes")
    public String bandejaMensajes(Model model, HttpSession session) {
        if (RoleGuard.sinSesion(session))
            return "redirect:/login";

        String userId = RoleGuard.getId(session);
        String tipo = RoleGuard.getTipo(session);
        List<ConversacionItem> conversaciones = new ArrayList<>();
        boolean esTalento = "talento".equals(tipo);

        List<Solicitud> misSolicitudes;
        if (esTalento) {
            misSolicitudes = solicitudRepository.findByUsuarioIdOrderByFechaSolicitudDesc(userId);
        } else {
            List<Empleo> empleos = empleoRepository.findByEmpresaIdOrderByIdDesc(userId);
            List<String> ids = new ArrayList<>();
            for (Empleo e : empleos)
                ids.add(e.getId());
            misSolicitudes = ids.isEmpty() ? List.of() : solicitudRepository.findByEmpleoIdIn(ids);
        }

        List<String> solicitudIds = new ArrayList<>();
        for (Solicitud sol : misSolicitudes)
            solicitudIds.add(sol.getId());

        // traigo todos los mensajes de todas mis conversaciones de una, en vez de 2 consultas por conversación
        List<Mensaje> todosMensajes = solicitudIds.isEmpty()
                ? List.of()
                : mensajeRepository.findBySolicitudIdInOrderByFechaEnvioDesc(solicitudIds);

        // vienen ordenados desc por fecha, así que la primera aparición de cada solicitudId ya es la más reciente
        Map<String, Mensaje> ultimoPorSolicitud = new LinkedHashMap<>();
        Map<String, Long> noLeidosPorSolicitud = new HashMap<>();
        for (Mensaje m : todosMensajes) {
            ultimoPorSolicitud.putIfAbsent(m.getSolicitudId(), m);
            if (userId.equals(m.getDestinatarioId()) && !m.isLeido())
                noLeidosPorSolicitud.merge(m.getSolicitudId(), 1L, Long::sum);
        }

        for (Solicitud sol : misSolicitudes) {
            Mensaje ultimo = ultimoPorSolicitud.get(sol.getId());
            if (ultimo == null)
                continue;
            long noLeidos = noLeidosPorSolicitud.getOrDefault(sol.getId(), 0L);
            String otroNombre = esTalento
                    ? (sol.getEmpleoEmpresaNombre() != null ? sol.getEmpleoEmpresaNombre() : "Empresa")
                    : ((sol.getUsuarioNombre() != null && !sol.getUsuarioNombre().isBlank())
                            ? sol.getUsuarioNombre()
                            : (sol.getUsuarioCorreo() != null ? sol.getUsuarioCorreo() : "Candidato"));
            conversaciones.add(new ConversacionItem(sol, ultimo, noLeidos, otroNombre));
        }

        conversaciones.sort((a, b) -> {
            Date fa = a.getUltimoMensaje().getFechaEnvio();
            Date fb = b.getUltimoMensaje().getFechaEnvio();
            if (fa == null)
                return 1;
            if (fb == null)
                return -1;
            return fb.compareTo(fa);
        });

        long totalNoLeidos = conversaciones.stream().mapToLong(ConversacionItem::getNoLeidos).sum();
        model.addAttribute("conversaciones", conversaciones);
        model.addAttribute("totalNoLeidos", totalNoLeidos);
        agregarSesion(session, model);
        return "mensajes";
    }

    @GetMapping("/chat/{solicitudId}")
    public String verChat(@PathVariable String solicitudId,
            Model model, HttpSession session,
            RedirectAttributes ra) {

        if (RoleGuard.sinSesion(session))
            return "redirect:/login";

        String userId = RoleGuard.getId(session);

        Optional<Solicitud> solOpt = solicitudRepository.findById(solicitudId);
        if (solOpt.isEmpty())
            return "redirect:/mensajes";
        Solicitud solicitud = solOpt.get();

        Optional<Empleo> empOpt = empleoRepository.findById(solicitud.getEmpleoId());
        if (empOpt.isEmpty())
            return "redirect:/mensajes";
        Empleo empleo = empOpt.get();

        boolean esTalento = userId.equals(solicitud.getUsuarioId());
        boolean esEmpresa = userId.equals(empleo.getEmpresaId());
        if (!esTalento && !esEmpresa) {
            ra.addFlashAttribute("flashMessage", "No tienes acceso a este chat");
            ra.addFlashAttribute("flashType", "error");
            return "redirect:/dashboard";
        }

        String miAvatarUrl = (String) session.getAttribute("usuarioActualFotoUrl");
        String otroAvatarUrl = null;
        String otroUserId = esTalento ? empleo.getEmpresaId() : solicitud.getUsuarioId();
        if (otroUserId != null) {
            if (esTalento) {
                otroAvatarUrl = empresaRepository.findById(otroUserId).map(Empresa::getFotoUrl).orElse(null);
            } else {
                Optional<Usuario> otroOpt = usuarioRepository.findById(otroUserId);
                if (otroOpt.isPresent())
                    otroAvatarUrl = otroOpt.get().getFotoUrl();
            }
        }
        if (otroAvatarUrl == null && esTalento && empleo.getLogoUrl() != null)
            otroAvatarUrl = empleo.getLogoUrl();

        String otroNombre = esTalento
                ? solicitud.getEmpleoEmpresaNombre()
                : (solicitud.getUsuarioNombre() != null
                        ? solicitud.getUsuarioNombre()
                        : solicitud.getUsuarioCorreo());
        String otroInitial = (otroNombre != null && !otroNombre.isBlank())
                ? otroNombre.substring(0, 1).toUpperCase()
                : "?";

        Pageable pageable = PageRequest.of(0, CHAT_PAGE_SIZE);
        Page<Mensaje> pagina = mensajeRepository
                .findBySolicitudIdOrderByFechaEnvioDesc(solicitudId, pageable);

        List<Mensaje> mensajes = new ArrayList<>(pagina.getContent());
        Collections.reverse(mensajes);

        // marco todo como leído de una sola pasada en vez de uno por uno
        Query markQuery = Query.query(
                Criteria.where("solicitudId").is(solicitudId)
                        .and("destinatarioId").is(userId)
                        .and("leido").is(false));
        mongoTemplate.updateMulti(markQuery, Update.update("leido", true), Mensaje.class);

        String primerMensajeFecha = null;
        if (!mensajes.isEmpty() && mensajes.get(0).getFechaEnvio() != null)
            primerMensajeFecha = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS")
                    .format(mensajes.get(0).getFechaEnvio());

        model.addAttribute("solicitud", solicitud);
        model.addAttribute("empleo", empleo);
        model.addAttribute("mensajes", mensajes);
        model.addAttribute("esTalento", esTalento);
        model.addAttribute("otroNombre", otroNombre);
        model.addAttribute("currentUserId", userId);
        model.addAttribute("miAvatarUrl", miAvatarUrl);
        model.addAttribute("otroAvatarUrl", otroAvatarUrl);
        model.addAttribute("otroInitial", otroInitial);
        model.addAttribute("hayMasAntiguos", pagina.hasNext());
        model.addAttribute("primerMensajeFecha", primerMensajeFecha);
        agregarSesion(session, model);
        return "chat";
    }

    @GetMapping(value = "/chat/{solicitudId}/mas-mensajes", produces = "application/json")
    @ResponseBody
    public ResponseEntity<?> masMensajes(@PathVariable String solicitudId,
            @RequestParam String antes,
            HttpSession session) {

        if (RoleGuard.sinSesion(session))
            return ResponseEntity.status(401).build();
        String userId = RoleGuard.getId(session);

        Optional<Solicitud> solOpt = solicitudRepository.findById(solicitudId);
        if (solOpt.isEmpty())
            return ResponseEntity.notFound().build();
        Solicitud sol = solOpt.get();

        Optional<Empleo> empOpt = empleoRepository.findById(sol.getEmpleoId());
        if (empOpt.isEmpty())
            return ResponseEntity.notFound().build();
        Empleo emp = empOpt.get();

        if (!userId.equals(sol.getUsuarioId()) && !userId.equals(emp.getEmpresaId()))
            return ResponseEntity.status(403).build();

        Date antesDate;
        try {
            antesDate = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS").parse(antes);
        } catch (java.text.ParseException e) {
            antesDate = new Date();
        }

        Pageable pageable = PageRequest.of(0, LOAD_MORE_SIZE + 1);
        List<Mensaje> raw = mensajeRepository
                .findBySolicitudIdAndFechaEnvioBeforeOrderByFechaEnvioDesc(solicitudId, antesDate, pageable);

        boolean hayMas = raw.size() > LOAD_MORE_SIZE;
        if (hayMas)
            raw = raw.subList(0, LOAD_MORE_SIZE);
        Collections.reverse(raw);

        List<Map<String, Object>> items = new ArrayList<>();
        for (Mensaje m : raw)
            items.add(mensajeAJson(m));

        Map<String, Object> resp = new LinkedHashMap<>();
        resp.put("mensajes", items);
        resp.put("hayMas", hayMas);
        if (!raw.isEmpty() && raw.get(0).getFechaEnvio() != null)
            resp.put("primerFecha", new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS").format(raw.get(0).getFechaEnvio()));

        return ResponseEntity.ok(resp);
    }

    // fallback por si el JS no cargó
    @PostMapping("/chat/{solicitudId}")
    public String enviarMensaje(@PathVariable String solicitudId,
            @RequestParam String texto,
            HttpSession session) {

        if (RoleGuard.sinSesion(session))
            return "redirect:/login";

        crearMensaje(solicitudId, texto, session);
        return "redirect:/chat/" + solicitudId;
    }

    // este es el que usa el chat de verdad, para no recargar la página en cada mensaje
    @PostMapping(value = "/chat/{solicitudId}/enviar", produces = "application/json")
    @ResponseBody
    public ResponseEntity<?> enviarMensajeAjax(@PathVariable String solicitudId,
            @RequestParam String texto,
            HttpSession session) {

        if (RoleGuard.sinSesion(session))
            return ResponseEntity.status(401).build();

        Mensaje msg = crearMensaje(solicitudId, texto, session);
        if (msg == null)
            return ResponseEntity.badRequest().body(Map.of("ok", false));

        return ResponseEntity.ok(Map.of("ok", true, "mensaje", mensajeAJson(msg)));
    }

    @GetMapping(value = "/chat/{solicitudId}/nuevos", produces = "application/json")
    @ResponseBody
    public ResponseEntity<?> mensajesNuevos(@PathVariable String solicitudId,
            @RequestParam String despues,
            HttpSession session) {

        if (RoleGuard.sinSesion(session))
            return ResponseEntity.status(401).build();
        String userId = RoleGuard.getId(session);

        Optional<Solicitud> solOpt = solicitudRepository.findById(solicitudId);
        if (solOpt.isEmpty())
            return ResponseEntity.notFound().build();
        Solicitud sol = solOpt.get();

        Optional<Empleo> empOpt = empleoRepository.findById(sol.getEmpleoId());
        if (empOpt.isEmpty())
            return ResponseEntity.notFound().build();
        Empleo emp = empOpt.get();

        if (!userId.equals(sol.getUsuarioId()) && !userId.equals(emp.getEmpresaId()))
            return ResponseEntity.status(403).build();

        Date despuesDate;
        try {
            despuesDate = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS").parse(despues);
        } catch (java.text.ParseException e) {
            return ResponseEntity.ok(Map.of("mensajes", List.of()));
        }

        List<Mensaje> nuevos = mensajeRepository
                .findBySolicitudIdAndFechaEnvioAfterOrderByFechaEnvioAsc(solicitudId, despuesDate);

        Query markQuery = Query.query(
                Criteria.where("solicitudId").is(solicitudId)
                        .and("destinatarioId").is(userId)
                        .and("leido").is(false));
        mongoTemplate.updateMulti(markQuery, Update.update("leido", true), Mensaje.class);

        List<Map<String, Object>> items = new ArrayList<>();
        for (Mensaje m : nuevos)
            items.add(mensajeAJson(m));

        return ResponseEntity.ok(Map.of("mensajes", items));
    }

    // devuelve null si algo no cuadra
    private Mensaje crearMensaje(String solicitudId, String texto, HttpSession session) {
        if (texto == null || texto.isBlank())
            return null;

        String userId = RoleGuard.getId(session);
        String userName = RoleGuard.getNombre(session);

        Optional<Solicitud> solOpt = solicitudRepository.findById(solicitudId);
        if (solOpt.isEmpty())
            return null;
        Solicitud solicitud = solOpt.get();

        Optional<Empleo> empOpt = empleoRepository.findById(solicitud.getEmpleoId());
        if (empOpt.isEmpty())
            return null;
        Empleo empleo = empOpt.get();

        boolean esTalento = userId.equals(solicitud.getUsuarioId());
        boolean esEmpresa = userId.equals(empleo.getEmpresaId());
        if (!esTalento && !esEmpresa)
            return null;

        String destinatarioId = esTalento ? empleo.getEmpresaId() : solicitud.getUsuarioId();
        String senderName = (userName != null && !userName.isBlank()) ? userName
                : (esTalento ? solicitud.getUsuarioNombre() : solicitud.getEmpleoEmpresaNombre());

        Mensaje msg = new Mensaje();
        msg.setSolicitudId(solicitudId);
        msg.setRemitenteId(userId);
        msg.setRemitenteNombre(senderName);
        msg.setDestinatarioId(destinatarioId);
        msg.setTexto(texto.trim());
        msg.setFechaEnvio(new Date());
        mensajeRepository.save(msg);

        try {
            String preview = texto.length() > 60 ? texto.substring(0, 60) + "…" : texto;
            Notificacion notif = new Notificacion();
            notif.setDestinatarioId(destinatarioId);
            notif.setTipo("NUEVO_MENSAJE");
            notif.setTitulo("Nuevo mensaje de " + senderName);
            notif.setMensaje("\"" + preview + "\"");
            notif.setFechaCreacion(new Date());
            notif.setUrl("/chat/" + solicitudId);
            notificacionRepository.save(notif);
        } catch (Exception ignored) {
        }

        return msg;
    }

    private Map<String, Object> mensajeAJson(Mensaje m) {
        SimpleDateFormat iso = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS");
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("id", m.getId());
        item.put("remitenteId", m.getRemitenteId());
        item.put("remitenteNombre", m.getRemitenteNombre());
        item.put("texto", m.getTexto());
        item.put("fechaIso", m.getFechaEnvio() != null ? iso.format(m.getFechaEnvio()) : null);
        item.put("leido", m.isLeido());
        return item;
    }

    public static class ConversacionItem {
        private final Solicitud solicitud;
        private final Mensaje ultimoMensaje;
        private final long noLeidos;
        private final String otroNombre;

        public ConversacionItem(Solicitud s, Mensaje m, long nl, String on) {
            this.solicitud = s;
            this.ultimoMensaje = m;
            this.noLeidos = nl;
            this.otroNombre = on;
        }

        public Solicitud getSolicitud() {
            return solicitud;
        }

        public Mensaje getUltimoMensaje() {
            return ultimoMensaje;
        }

        public long getNoLeidos() {
            return noLeidos;
        }

        public String getOtroNombre() {
            return otroNombre;
        }
    }

    private void agregarSesion(HttpSession session, Model model) {
        model.addAttribute("usuarioActualId", RoleGuard.getId(session));
        model.addAttribute("usuarioActualCorreo", RoleGuard.getCorreo(session));
        model.addAttribute("usuarioActualNombre", RoleGuard.getNombre(session));
        model.addAttribute("usuarioActualTipo", RoleGuard.getTipo(session));
        model.addAttribute("usuarioActualFotoUrl", session.getAttribute("usuarioActualFotoUrl"));
    }
}