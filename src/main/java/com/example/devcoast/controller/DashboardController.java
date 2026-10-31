package com.example.devcoast.controller;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import com.example.devcoast.config.RoleGuard;
import com.example.devcoast.model.Empleo;
import com.example.devcoast.model.Invitacion;
import com.example.devcoast.model.Solicitud;
import com.example.devcoast.model.Usuario;
import com.example.devcoast.repository.EmpleoRepository;
import com.example.devcoast.repository.InvitacionRepository;
import com.example.devcoast.repository.SolicitudRepository;
import com.example.devcoast.repository.UsuarioRepository;
import com.example.devcoast.service.MatchEmpleo;

import jakarta.servlet.http.HttpSession;

@Controller
public class DashboardController {

    private static final int OFERTAS_CANDIDATAS = 150;
    private static final int DIAS_NUEVAS = 7;
    private static final int DIAS_AVISTADAS = 30;
    private static final long DIA = 86_400_000L;

    private final SolicitudRepository solicitudRepository;
    private final UsuarioRepository usuarioRepository;
    private final InvitacionRepository invitacionRepository;
    private final EmpleoRepository empleoRepository;
    private final MongoTemplate mongoTemplate;

    public DashboardController(SolicitudRepository solicitudRepository, UsuarioRepository usuarioRepository,
            InvitacionRepository invitacionRepository, EmpleoRepository empleoRepository, MongoTemplate mongoTemplate) {
        this.solicitudRepository = solicitudRepository;
        this.usuarioRepository = usuarioRepository;
        this.invitacionRepository = invitacionRepository;
        this.empleoRepository = empleoRepository;
        this.mongoTemplate = mongoTemplate;
    }

    @GetMapping("/dashboard")
    public String dashboard(HttpSession session, Model model) {
        if (RoleGuard.sinSesion(session))
            return "redirect:/login";
        model.addAttribute("nombrePila", DashboardCalculos.nombrePila(RoleGuard.getNombre(session)));
        return RoleGuard.esEmpresa(session) ? dashboardEmpresa(session, model) : dashboardTalento(session, model);
    }

    private String dashboardTalento(HttpSession session, Model model) {
        String id = RoleGuard.getId(session);
        Usuario talento = usuarioRepository.findById(id).orElse(null);
        Date ahora = new Date();

        List<Solicitud> solicitudes = solicitudRepository.findByUsuarioIdOrderByFechaSolicitudDesc(id);
        List<SolicitudVista> vistas = new ArrayList<>();
        for (Solicitud s : solicitudes)
            vistas.add(SolicitudVista.de(s, talento, ahora));
        CartaVista carta = CartaVista.de(CartaVista.deSolicitudes(vistas), ahora);

        List<Invitacion> invitaciones = invitacionRepository.findByTalentoIdOrderByFechaEnvioDesc(id);
        Invitacion pendiente = invitaciones.stream().filter(i -> "pendiente".equals(i.getEstado())).findFirst()
                .orElse(null);
        long invPendientes = invitaciones.stream().filter(i -> "pendiente".equals(i.getEstado())).count();

        boolean tieneStack = talento != null && talento.getStack() != null && !talento.getStack().isEmpty();
        Set<String> yaSolicitadas = new HashSet<>();
        solicitudes.forEach(s -> yaSolicitadas.add(s.getEmpleoId()));
        Map<String, MatchEmpleo.Resultado> matches = new HashMap<>();
        List<Empleo> candidatas = new ArrayList<>();
        if (tieneStack) {
            Query q = new Query(new Criteria().orOperator(
                    Criteria.where("estado").is("ACTIVO"), Criteria.where("estado").exists(false)))
                    .with(Sort.by(Sort.Direction.DESC, "_id")).limit(OFERTAS_CANDIDATAS);
            for (Empleo e : mongoTemplate.find(q, Empleo.class)) {
                if (yaSolicitadas.contains(e.getId()))
                    continue;
                MatchEmpleo.Resultado m = MatchEmpleo.calcular(talento, e);
                if (m != null) {
                    matches.put(e.getId(), m);
                    candidatas.add(e);
                }
            }
        }

        Empleo mejor = null;
        for (Empleo e : candidatas)
            if (matches.get(e.getId()).porcentaje() >= MatchEmpleo.NIVEL_MEDIO
                    && (mejor == null || matches.get(e.getId()).porcentaje() > matches.get(mejor.getId()).porcentaje()))
                mejor = e;
        EmpleoVista destacado = null;
        if (mejor != null)
            destacado = EmpleoVista.de(mejor, matches.get(mejor.getId()), false, true,
                    solicitudRepository.countByEmpleoId(mejor.getId()), ahora);

        long nuevasAltas = candidatas.stream().filter(e -> dias(e, ahora) <= DIAS_NUEVAS
                && matches.get(e.getId()).porcentaje() >= MatchEmpleo.NIVEL_ALTO).count();

        List<Empleo> avist = new ArrayList<>();
        for (Empleo e : candidatas)
            if (e != mejor && dias(e, ahora) <= DIAS_AVISTADAS
                    && matches.get(e.getId()).porcentaje() >= MatchEmpleo.NIVEL_MEDIO)
                avist.add(e);
        avist.sort((x, y) -> Integer.compare(matches.get(y.getId()).porcentaje(), matches.get(x.getId()).porcentaje()));
        List<EmpleoVista> avistadas = new ArrayList<>();
        for (Empleo e : avist.subList(0, Math.min(3, avist.size())))
            avistadas.add(EmpleoVista.de(e, matches.get(e.getId()), false, false, null, ahora));

        List<Integer> matchesEnRuta = new ArrayList<>();
        for (CartaVista.Zona z : carta.getZonas())
            z.barcos().forEach(b -> {
                if (b.match() != null)
                    matchesEnRuta.add(b.match());
            });

        model.addAttribute("carta", carta.conTextos("/solicitudes",
                "Todavía no has zarpado. Postúlate a una oferta desde Empleos."));
        model.addAttribute("nEnRuta", carta.getTotalEnRuta());
        model.addAttribute("nInvPendientes", invPendientes);
        model.addAttribute("nNuevasAltas", nuevasAltas);
        model.addAttribute("matchPromedio", DashboardCalculos.promedio(matchesEnRuta));
        model.addAttribute("destacado", destacado);
        model.addAttribute("destacadoMotivo", "match");
        model.addAttribute("destacadoEtiqueta", "MEJOR MATCH DE HOY");
        model.addAttribute("invitacion", pendiente == null ? null : InvitacionVista.de(pendiente, ahora));
        model.addAttribute("avistadas", avistadas);
        model.addAttribute("talentoSinStack", talento != null && !tieneStack);
        model.addAttribute("esTalento", true);
        model.addAttribute("completitud", talento == null ? 0 : talento.getCompletitudPerfil());
        List<DashboardCalculos.Pendiente> pendientes = DashboardCalculos.pendientesPerfil(talento);
        model.addAttribute("pendientes", pendientes.subList(0, Math.min(5, pendientes.size())));
        model.addAttribute("pendientesMas", Math.max(0, pendientes.size() - 5));
        model.addAttribute("eventos", DashboardCalculos.eventosTalento(solicitudes, invitaciones, ahora, 6));
        model.addAttribute("mostrarOnboarding", talento != null && !talento.isOnboardingCompleto());
        model.addAttribute("usuario", talento);
        return "dashboard";
    }

    private String dashboardEmpresa(HttpSession session, Model model) {
        String id = RoleGuard.getId(session);
        Date ahora = new Date();

        List<Empleo> activos = empleoRepository.findByEmpresaIdAndEstadoOrderByIdDesc(id, "ACTIVO");
        List<Empleo> cerrados = empleoRepository.findByEmpresaIdAndEstadoOrderByIdDesc(id, "CERRADO");
        List<String> empleoIds = new ArrayList<>();
        activos.forEach(e -> empleoIds.add(e.getId()));
        cerrados.forEach(e -> empleoIds.add(e.getId()));

        List<Solicitud> recibidas = empleoIds.isEmpty() ? List.of()
                : solicitudRepository.findByEmpleoIdIn(empleoIds).stream()
                        .sorted(Comparator.comparing(Solicitud::getFechaSolicitud,
                                Comparator.nullsLast(Comparator.reverseOrder())))
                        .limit(300).collect(Collectors.toList());
        Map<String, Usuario> candidatos = new HashMap<>();
        if (!recibidas.isEmpty()) {
            Set<String> ids = recibidas.stream().map(Solicitud::getUsuarioId).filter(java.util.Objects::nonNull)
                    .collect(Collectors.toSet());
            usuarioRepository.findAllById(ids).forEach(u -> candidatos.put(u.getId(), u));
        }

        List<SolicitudVista> vistas = new ArrayList<>();
        List<CartaVista.Item> items = new ArrayList<>();
        Map<String, Long> porOferta = new HashMap<>();
        for (Solicitud s : recibidas) {
            SolicitudVista v = SolicitudVista.de(s, candidatos.get(s.getUsuarioId()), ahora);
            vistas.add(v);
            porOferta.merge(s.getEmpleoId(), 1L, Long::sum);
            items.add(new CartaVista.Item(v.getId(), v.getCandidato() != null ? v.getCandidato() : "Candidato",
                    v.getCargo(), v.getMatch(),
                    v.getAreaClave(), v.getEstado(), v.getFechaEnvio(), v.getFechaDecision(),
                    "/empleos/" + s.getEmpleoId() + "/postulaciones/" + s.getId()));
        }
        CartaVista carta = CartaVista.de(items, ahora).conTextos("/candidatos",
                "Todavía no recibes postulaciones. Publica una oferta para empezar.").paraEmpresa();

        List<SolicitudVista> enRevision = vistas.stream().filter(v -> "revision".equals(v.getEstado()))
                .collect(Collectors.toList());
        long candidatosAltos = enRevision.stream()
                .filter(v -> v.getMatch() != null && v.getMatch() >= MatchEmpleo.NIVEL_ALTO).count();
        List<SolicitudVista> mejores = enRevision.stream().filter(v -> v.getMatch() != null)
                .sorted(Comparator.comparingInt(SolicitudVista::getMatch).reversed()).limit(3)
                .collect(Collectors.toList());

        List<Invitacion> invitaciones = invitacionRepository.findByEmpresaIdOrderByFechaEnvioDesc(id);

        List<Map<String, Object>> ofertas = new ArrayList<>();
        for (Empleo e : activos.subList(0, Math.min(5, activos.size()))) {
            Map<String, Object> o = new HashMap<>();
            o.put("id", e.getId());
            o.put("titulo", e.getTitulo());
            o.put("postulaciones", porOferta.getOrDefault(e.getId(), 0L));
            ofertas.add(o);
        }

        model.addAttribute("carta", carta);
        model.addAttribute("nActivas", activos.size());
        long enRevisionTotal = activos.isEmpty() ? 0
                : mongoTemplate.count(
                        Query.query(Criteria.where("empleoId").in(activosIds(activos)).and("estado").is("revision")),
                        Solicitud.class);
        model.addAttribute("nNuevas", enRevisionTotal);
        model.addAttribute("solicitudesPendientesEmpresa", enRevisionTotal);
        model.addAttribute("nCandidatosAltos", candidatosAltos);
        model.addAttribute("nInvitaciones", invitaciones.size());
        model.addAttribute("mejores", mejores);
        model.addAttribute("ofertas", ofertas);
        model.addAttribute("ofertasMas", Math.max(0, activos.size() - 5));
        model.addAttribute("eventos", DashboardCalculos.eventosEmpresa(recibidas, invitaciones, ahora, 6));
        return "dashboard-empresa";
    }

    private static List<String> activosIds(List<Empleo> activos) {
        return activos.stream().map(Empleo::getId).collect(Collectors.toList());
    }

    private static long dias(Empleo e, Date ahora) {
        Date f = e.getFechaPublicacionEfectiva();
        return f == null ? Long.MAX_VALUE : Math.max(0, (ahora.getTime() - f.getTime()) / DIA);
    }
}
