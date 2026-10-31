package com.example.devcoast.controller;

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
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.bson.Document;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.example.devcoast.config.LimiteTasa;
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
    private static final int MAX_CONVERSACIONES = 100;
    private static final int MAX_TEXTO = 1000;
    private static final int MAX_MENSAJES_POR_MINUTO = 40;

    private final SolicitudRepository solicitudRepository;
    private final EmpleoRepository empleoRepository;
    private final MensajeRepository mensajeRepository;
    private final NotificacionRepository notificacionRepository;
    private final UsuarioRepository usuarioRepository;
    private final EmpresaRepository empresaRepository;
    private final MongoTemplate mongoTemplate;
    private final LimiteTasa limiteTasa;

    public MensajeController(SolicitudRepository solicitudRepository, EmpleoRepository empleoRepository,
            MensajeRepository mensajeRepository, NotificacionRepository notificacionRepository,
            UsuarioRepository usuarioRepository, EmpresaRepository empresaRepository, MongoTemplate mongoTemplate,
            LimiteTasa limiteTasa) {
        this.solicitudRepository = solicitudRepository;
        this.empleoRepository = empleoRepository;
        this.mensajeRepository = mensajeRepository;
        this.notificacionRepository = notificacionRepository;
        this.usuarioRepository = usuarioRepository;
        this.empresaRepository = empresaRepository;
        this.mongoTemplate = mongoTemplate;
        this.limiteTasa = limiteTasa;
    }

    @GetMapping("/mensajes")
    public String bandejaMensajes(@RequestParam(required = false) String q, Model model, HttpSession session) {
        if (RoleGuard.sinSesion(session))
            return "redirect:/login";

        cargarBandeja(model, session, q);
        model.addAttribute("q", q == null || q.isBlank() ? null : q.trim());
        agregarSesion(session, model);
        return "mensajes";
    }

    // oigan, esto de acá arma la lista de conversaciones sin traerse todos los
    // mensajes: la base nos da solo el último de cada charla
    private void cargarBandeja(Model model, HttpSession session, String q) {
        String userId = RoleGuard.getId(session);
        boolean esTalento = "talento".equals(RoleGuard.getTipo(session));
        List<ConversacionItem> conversaciones = conversaciones(userId, esTalento);
        String buscado = normalizar(q);
        if (!buscado.isEmpty())
            conversaciones = conversaciones.stream()
                    .filter(c -> normalizar(c.getOtroNombre() + " " + c.getSolicitud().getEmpleoTitulo())
                            .contains(buscado))
                    .toList();
        model.addAttribute("conversaciones", conversaciones);
        model.addAttribute("totalNoLeidos", conversaciones.stream().mapToLong(ConversacionItem::getNoLeidos).sum());
    }

    private List<ConversacionItem> conversaciones(String userId, boolean esTalento) {
        Criteria mio = new Criteria().orOperator(Criteria.where("destinatarioId").is(userId),
                Criteria.where("remitenteId").is(userId));
        List<Document> ultimos = mongoTemplate.aggregate(Aggregation.newAggregation(
                Aggregation.match(mio),
                Aggregation.sort(Sort.Direction.DESC, "fechaEnvio"),
                Aggregation.group("solicitudId").first("texto").as("texto").first("fechaEnvio").as("fecha")
                        .first("remitenteId").as("remitente"),
                Aggregation.sort(Sort.Direction.DESC, "fecha"),
                Aggregation.limit(MAX_CONVERSACIONES)), "mensajes", Document.class).getMappedResults();
        if (ultimos.isEmpty())
            return List.of();

        Map<String, Long> noLeidos = new HashMap<>();
        for (Document d : mongoTemplate.aggregate(Aggregation.newAggregation(
                Aggregation.match(Criteria.where("destinatarioId").is(userId).and("leido").is(false)),
                Aggregation.group("solicitudId").count().as("n")), "mensajes", Document.class).getMappedResults())
            noLeidos.put(d.getString("_id"), ((Number) d.get("n")).longValue());

        List<String> ids = new ArrayList<>();
        for (Document d : ultimos)
            ids.add(d.getString("_id"));
        Map<String, Solicitud> solicitudes = new HashMap<>();
        solicitudRepository.findAllById(ids).forEach(s -> solicitudes.put(s.getId(), s));

        List<ConversacionItem> lista = new ArrayList<>();
        for (Document d : ultimos) {
            Solicitud sol = solicitudes.get(d.getString("_id"));
            if (sol == null)
                continue;
            Date fecha = d.getDate("fecha");
            lista.add(new ConversacionItem(sol, nombreDelOtro(sol, esTalento), d.getString("texto"),
                    userId.equals(d.getString("remitente")), fecha == null ? 0 : fecha.getTime(),
                    noLeidos.getOrDefault(sol.getId(), 0L)));
        }
        return lista;
    }

    private static String normalizar(String s) {
        return java.text.Normalizer.normalize(s == null ? "" : s, java.text.Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "").toLowerCase().trim();
    }

    private static String nombreDelOtro(Solicitud sol, boolean esTalento) {
        if (esTalento)
            return sol.getEmpleoEmpresaNombre() != null && !sol.getEmpleoEmpresaNombre().isBlank()
                    ? sol.getEmpleoEmpresaNombre()
                    : "Empresa";
        if (sol.getUsuarioNombre() != null && !sol.getUsuarioNombre().isBlank())
            return sol.getUsuarioNombre();
        return sol.getUsuarioCorreo() != null ? sol.getUsuarioCorreo() : "Candidato";
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

        String otroNombre = nombreDelOtro(solicitud, esTalento);
        String otroInitial = otroNombre.substring(0, 1).toUpperCase();

        Pageable pageable = PageRequest.of(0, CHAT_PAGE_SIZE);
        Page<Mensaje> pagina = mensajeRepository
                .findBySolicitudIdOrderByFechaEnvioDesc(solicitudId, pageable);

        List<Mensaje> mensajes = new ArrayList<>(pagina.getContent());
        Collections.reverse(mensajes);

        long ultimaMarca = mensajes.isEmpty() || mensajes.get(mensajes.size() - 1).getFechaEnvio() == null
                ? System.currentTimeMillis()
                : mensajes.get(mensajes.size() - 1).getFechaEnvio().getTime();
        Long primeraMarca = mensajes.isEmpty() || mensajes.get(0).getFechaEnvio() == null ? null
                : mensajes.get(0).getFechaEnvio().getTime();

        marcarLeidos(solicitudId, userId);

        model.addAttribute("solicitud", solicitud);
        model.addAttribute("empleo", empleo);
        model.addAttribute("mensajes", mensajes);
        model.addAttribute("esTalento", esTalento);
        model.addAttribute("otroNombre", otroNombre);
        model.addAttribute("currentUserId", userId);
        model.addAttribute("otroAvatarUrl", otroAvatarUrl);
        model.addAttribute("otroInitial", otroInitial);
        model.addAttribute("hayMasAntiguos", pagina.hasNext());
        model.addAttribute("primeraMarca", primeraMarca);
        model.addAttribute("ultimaMarca", ultimaMarca);
        model.addAttribute("otroPerfilUrl", esTalento
                ? (empleo.getEmpresaId() != null ? "/empresa/" + empleo.getEmpresaId() : null)
                : (solicitud.getUsuarioId() != null ? "/talentos/" + solicitud.getUsuarioId() : null));
        model.addAttribute("solicitudUrl", esTalento ? "/solicitudes/" + solicitudId
                : "/empleos/" + empleo.getId() + "/postulaciones/" + solicitudId);
        model.addAttribute("activaId", solicitudId);
        model.addAttribute("otroTono", Math.floorMod(otroNombre.toLowerCase().hashCode(), 4));
        cargarBandeja(model, session, null);
        agregarSesion(session, model);
        return "chat";
    }

    private void marcarLeidos(String solicitudId, String userId) {
        mongoTemplate.updateMulti(Query.query(Criteria.where("solicitudId").is(solicitudId)
                .and("destinatarioId").is(userId).and("leido").is(false)), Update.update("leido", true), Mensaje.class);
    }

    private boolean participa(Solicitud sol, Empleo emp, String userId) {
        return userId.equals(sol.getUsuarioId()) || userId.equals(emp.getEmpresaId());
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
        Optional<Empleo> empOpt = empleoRepository.findById(solOpt.get().getEmpleoId());
        if (empOpt.isEmpty())
            return ResponseEntity.notFound().build();
        if (!participa(solOpt.get(), empOpt.get(), userId))
            return ResponseEntity.status(403).build();

        Date antesDate = aFecha(antes);
        if (antesDate == null)
            antesDate = new Date();

        List<Mensaje> raw = mensajeRepository.findBySolicitudIdAndFechaEnvioBeforeOrderByFechaEnvioDesc(solicitudId,
                antesDate, PageRequest.of(0, LOAD_MORE_SIZE + 1));

        boolean hayMas = raw.size() > LOAD_MORE_SIZE;
        if (hayMas)
            raw = new ArrayList<>(raw.subList(0, LOAD_MORE_SIZE));
        Collections.reverse(raw);

        List<Map<String, Object>> items = new ArrayList<>();
        for (Mensaje m : raw)
            items.add(mensajeAJson(m));

        Map<String, Object> resp = new LinkedHashMap<>();
        resp.put("mensajes", items);
        resp.put("hayMas", hayMas);
        if (!raw.isEmpty() && raw.get(0).getFechaEnvio() != null)
            resp.put("primera", raw.get(0).getFechaEnvio().getTime());

        return ResponseEntity.ok(resp);
    }

    @PostMapping("/chat/{solicitudId}")
    public String enviarMensaje(@PathVariable String solicitudId,
            @RequestParam String texto,
            HttpSession session) {

        if (RoleGuard.sinSesion(session))
            return "redirect:/login";

        if (!pasoElLimite(session))
            crearMensaje(solicitudId, texto, session);
        return "redirect:/chat/" + solicitudId;
    }

    @PostMapping(value = "/chat/{solicitudId}/enviar", produces = "application/json")
    @ResponseBody
    public ResponseEntity<?> enviarMensajeAjax(@PathVariable String solicitudId,
            @RequestParam String texto,
            HttpSession session) {

        if (RoleGuard.sinSesion(session))
            return ResponseEntity.status(401).build();
        if (pasoElLimite(session))
            return ResponseEntity.status(429).body(Map.of("ok", false, "error", "limite"));

        Mensaje msg = crearMensaje(solicitudId, texto, session);
        if (msg == null)
            return ResponseEntity.badRequest().body(Map.of("ok", false));

        return ResponseEntity.ok(Map.of("ok", true, "mensaje", mensajeAJson(msg)));
    }

    private boolean pasoElLimite(HttpSession session) {
        return limiteTasa.excedido("chat:" + RoleGuard.getId(session), MAX_MENSAJES_POR_MINUTO, 60_000L);
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
        Optional<Empleo> empOpt = empleoRepository.findById(solOpt.get().getEmpleoId());
        if (empOpt.isEmpty())
            return ResponseEntity.notFound().build();
        if (!participa(solOpt.get(), empOpt.get(), userId))
            return ResponseEntity.status(403).build();

        Date despuesDate = aFecha(despues);
        List<Mensaje> nuevos = despuesDate == null ? List.of()
                : mensajeRepository.findBySolicitudIdAndFechaEnvioAfterOrderByFechaEnvioAsc(solicitudId, despuesDate);

        marcarLeidos(solicitudId, userId);

        List<Map<String, Object>> items = new ArrayList<>();
        for (Mensaje m : nuevos)
            items.add(mensajeAJson(m));

        Query propiosSinLeer = Query.query(Criteria.where("solicitudId").is(solicitudId).and("remitenteId").is(userId)
                .and("leido").is(false));
        propiosSinLeer.fields().include("_id");
        propiosSinLeer.limit(200);
        List<String> sinLeer = new ArrayList<>();
        for (Mensaje m : mongoTemplate.find(propiosSinLeer, Mensaje.class))
            sinLeer.add(m.getId());

        Map<String, Object> resp = new LinkedHashMap<>();
        resp.put("mensajes", items);
        resp.put("sinLeer", sinLeer);
        return ResponseEntity.ok(resp);
    }

    private static Date aFecha(String marca) {
        try {
            return new Date(Long.parseLong(marca.trim()));
        } catch (RuntimeException e) {
            return null;
        }
    }

    private Mensaje crearMensaje(String solicitudId, String texto, HttpSession session) {
        if (texto == null || texto.isBlank())
            return null;
        texto = texto.trim();
        if (texto.length() > MAX_TEXTO)
            texto = texto.substring(0, MAX_TEXTO);

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
        msg.setTexto(texto);
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
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("id", m.getId());
        item.put("remitenteId", m.getRemitenteId());
        item.put("remitenteNombre", m.getRemitenteNombre());
        item.put("texto", m.getTexto());
        item.put("t", m.getFechaEnvio() != null ? m.getFechaEnvio().getTime() : null);
        item.put("leido", m.isLeido());
        return item;
    }

    public static class ConversacionItem {
        private final Solicitud solicitud;
        private final String otroNombre;
        private final String ultimoTexto;
        private final boolean ultimoPropio;
        private final long ultimaMarca;
        private final long noLeidos;

        public ConversacionItem(Solicitud s, String otroNombre, String ultimoTexto, boolean ultimoPropio,
                long ultimaMarca, long noLeidos) {
            this.solicitud = s;
            this.otroNombre = otroNombre;
            this.ultimoTexto = ultimoTexto == null ? "" : ultimoTexto;
            this.ultimoPropio = ultimoPropio;
            this.ultimaMarca = ultimaMarca;
            this.noLeidos = noLeidos;
        }

        public Solicitud getSolicitud() {
            return solicitud;
        }

        public String getOtroNombre() {
            return otroNombre;
        }

        public String getUltimoTexto() {
            return ultimoTexto;
        }

        public boolean isUltimoPropio() {
            return ultimoPropio;
        }

        public long getUltimaMarca() {
            return ultimaMarca;
        }

        public long getNoLeidos() {
            return noLeidos;
        }

        public String getInicial() {
            return otroNombre.isBlank() ? "?" : otroNombre.substring(0, 1).toUpperCase();
        }

        public int getTono() {
            return Math.floorMod(otroNombre.toLowerCase().hashCode(), 4);
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
