package com.example.devcoast.controller;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.example.devcoast.config.RoleGuard;
import com.example.devcoast.model.Empleo;
import com.example.devcoast.model.Empresa;
import com.example.devcoast.model.Solicitud;
import com.example.devcoast.model.Usuario;
import com.example.devcoast.repository.EmpleoRepository;
import com.example.devcoast.repository.EmpresaRepository;
import com.example.devcoast.repository.SolicitudRepository;
import com.example.devcoast.repository.UsuarioRepository;
import com.example.devcoast.service.AreaEmpleo;
import com.example.devcoast.service.PlanService;

import jakarta.servlet.http.HttpSession;

@Controller
public class CandidatosController {

    private static final int POR_PAGINA = 25;

    private final EmpleoRepository empleoRepository;
    private final SolicitudRepository solicitudRepository;
    private final UsuarioRepository usuarioRepository;
    private final EmpresaRepository empresaRepository;
    private final PlanService planService;

    public CandidatosController(EmpleoRepository empleoRepository, SolicitudRepository solicitudRepository,
            UsuarioRepository usuarioRepository, EmpresaRepository empresaRepository, PlanService planService) {
        this.empleoRepository = empleoRepository;
        this.solicitudRepository = solicitudRepository;
        this.usuarioRepository = usuarioRepository;
        this.empresaRepository = empresaRepository;
        this.planService = planService;
    }

    @GetMapping("/candidatos")
    public String candidatos(@RequestParam(required = false) String estado, @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "0") int page, Model model, HttpSession session) {
        if (RoleGuard.sinSesion(session))
            return "redirect:/login";
        if (RoleGuard.soloEmpresa(session))
            return "redirect:/dashboard";

        String empresaId = RoleGuard.getId(session);
        String filtro = estado == null ? "" : estado.trim().toLowerCase();
        if (!List.of("revision", "aceptadas", "rechazadas").contains(filtro))
            filtro = "";
        String busqueda = q == null ? "" : q.trim();

        List<Empleo> empleos = empleoRepository.findByEmpresaIdOrderByIdDesc(empresaId).stream()
                .filter(e -> !"eliminado".equalsIgnoreCase(e.getEstado()))
                .collect(Collectors.toList());
        List<String> empleoIds = empleos.stream().map(Empleo::getId).collect(Collectors.toList());
        List<Solicitud> todas = empleoIds.isEmpty() ? List.of() : solicitudRepository.findByEmpleoIdIn(empleoIds);

        Map<String, Usuario> candidatos = new HashMap<>();
        Set<String> usuarioIds = todas.stream().map(Solicitud::getUsuarioId).filter(java.util.Objects::nonNull).collect(Collectors.toSet());
        if (!usuarioIds.isEmpty())
            usuarioRepository.findAllById(usuarioIds).forEach(u -> candidatos.put(u.getId(), u));

        Date ahora = new Date();
        Map<String, List<SolicitudVista>> vistasPorOferta = new HashMap<>();
        for (Solicitud s : todas) {
            SolicitudVista v = SolicitudVista.de(s, candidatos.get(s.getUsuarioId()), ahora);
            vistasPorOferta.computeIfAbsent(s.getEmpleoId(), k -> new ArrayList<>()).add(v);
        }

        List<PostulacionesOfertaVista> grupos = new ArrayList<>();
        int total = 0, revision = 0, aceptadas = 0, noAvanzaron = 0;
        for (Empleo e : empleos) {
            List<SolicitudVista> deLaOferta = vistasPorOferta.getOrDefault(e.getId(), List.of());
            deLaOferta = deLaOferta.stream().sorted((a, b) -> {
                if (a.getFecha() == null) return 1;
                if (b.getFecha() == null) return -1;
                return b.getFecha().compareTo(a.getFecha());
            }).collect(Collectors.toList());
            final String f = filtro;
            final String b = busqueda;
            List<SolicitudVista> filtradas = deLaOferta.stream()
                    .filter(v -> coincideEstado(v, f) && coincideBusqueda(v, b))
                    .collect(Collectors.toList());
            AreaEmpleo.Area area = AreaEmpleo.de(e.getCategoria());
            PostulacionesOfertaVista g = PostulacionesOfertaVista.de(e.getId(), e.getTitulo(), e.getCategoriaMostrar(),
                    area == AreaEmpleo.Area.PROGRAMACION ? "p" : area == AreaEmpleo.Area.DISENO ? "d" : "o",
                    !"CERRADO".equalsIgnoreCase(e.getEstado()), deLaOferta, filtradas);
            total += g.getTotal();
            revision += g.getRevision();
            aceptadas += g.getAceptadas();
            noAvanzaron += g.getNoAvanzaron();
            if (!filtradas.isEmpty())
                grupos.add(g);
        }

        Empresa empresa = empresaRepository.findById(empresaId).orElse(null);

        int totalFiltradas = grupos.stream().mapToInt(g -> g.getItems().size()).sum();
        int totalPaginas = Math.max(1, (int) Math.ceil(totalFiltradas / (double) POR_PAGINA));
        int pagina = Math.min(Math.max(0, page), totalPaginas - 1);
        model.addAttribute("grupos", recortar(grupos, pagina * POR_PAGINA, (pagina + 1) * POR_PAGINA));
        model.addAttribute("pagina", pagina);
        model.addAttribute("totalPaginas", totalPaginas);
        model.addAttribute("estadoSel", filtro);
        model.addAttribute("q", busqueda.isEmpty() ? null : busqueda);
        model.addAttribute("nTotal", total);
        model.addAttribute("nRevision", revision);
        model.addAttribute("nAceptadas", aceptadas);
        model.addAttribute("nNoAvanzaron", noAvanzaron);
        model.addAttribute("hayFiltros", !filtro.isEmpty() || !busqueda.isEmpty());
        model.addAttribute("cobraColocacion", !planService.esPro(empresa));
        agregarSesion(session, model);
        return "candidatos";
    }

    // deja solo las postulaciones [desde, hasta) contando todas las ofertas en orden; las ofertas sin nada en esa página se omiten
    private static List<PostulacionesOfertaVista> recortar(List<PostulacionesOfertaVista> grupos, int desde, int hasta) {
        List<PostulacionesOfertaVista> res = new ArrayList<>();
        int inicio = 0;
        for (PostulacionesOfertaVista g : grupos) {
            int n = g.getItems().size();
            int a = Math.max(desde - inicio, 0), b = Math.min(hasta - inicio, n);
            if (a < b)
                res.add(g.conItems(g.getItems().subList(a, b)));
            inicio += n;
        }
        return res;
    }

    private static boolean coincideEstado(SolicitudVista v, String filtro) {
        return switch (filtro) {
            case "revision" -> "revision".equals(v.getEstado());
            case "aceptadas" -> "aprobada".equals(v.getEstado());
            case "rechazadas" -> "rechazada".equals(v.getEstado()) || "descartado".equals(v.getEstado());
            default -> true;
        };
    }

    private static boolean coincideBusqueda(SolicitudVista v, String q) {
        if (q.isEmpty())
            return true;
        String pajar = sinTildes((v.getCandidato() == null ? "" : v.getCandidato()) + " " + (v.getCandidatoRol() == null ? "" : v.getCandidatoRol()));
        for (String token : sinTildes(q).split("\\s+"))
            if (!token.isEmpty() && !pajar.contains(token))
                return false;
        return true;
    }

    private static String sinTildes(String s) {
        return Normalizer.normalize(s, Normalizer.Form.NFD).replaceAll("\\p{M}+", "").toLowerCase();
    }

    private void agregarSesion(HttpSession session, Model model) {
        model.addAttribute("usuarioActualId", RoleGuard.getId(session));
        model.addAttribute("usuarioActualCorreo", RoleGuard.getCorreo(session));
        model.addAttribute("usuarioActualNombre", RoleGuard.getNombre(session));
        model.addAttribute("usuarioActualTipo", RoleGuard.getTipo(session));
        model.addAttribute("usuarioActualFotoUrl", session.getAttribute("usuarioActualFotoUrl"));
    }
}
