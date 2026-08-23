package com.example.devcoast.controller;

import java.util.Date;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
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
import com.example.devcoast.model.Usuario;
import com.example.devcoast.repository.EmpleoRepository;
import com.example.devcoast.repository.EmpresaRepository;
import com.example.devcoast.repository.InvitacionRepository;
import com.example.devcoast.repository.NotificacionRepository;
import com.example.devcoast.repository.UsuarioRepository;

import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
public class TalentoController {

    private final UsuarioRepository usuarioRepository;
    private final EmpleoRepository empleoRepository;
    private final EmpresaRepository empresaRepository;
    private final InvitacionRepository invitacionRepository;
    private final NotificacionRepository notificacionRepository;

    @GetMapping("/talentos")
    public String listarTalentos(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size,
            @RequestParam(required = false) String q,
            Model model, HttpSession session) {

        if (RoleGuard.sinSesion(session))
            return "redirect:/login";

        if (size != 10 && size != 25 && size != 50)
            size = 12;
        page = Math.max(0, page);

        String query = q == null ? "" : q.trim();
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "creadoEn"));
        Page<Usuario> paginaTalentos = query.isBlank()
                ? usuarioRepository.findByPerfilPublicoTrueAndTipoNot("empresa", pageable)
                : usuarioRepository.searchTalentos("empresa", java.util.regex.Pattern.quote(query), pageable);

        int totalPages = paginaTalentos.getTotalPages();
        int currentPage = paginaTalentos.getNumber();

        int windowSize = 2;
        int startPage = Math.max(0, currentPage - windowSize);
        int endPage = Math.min(totalPages - 1, currentPage + windowSize);
        boolean showStartEllipsis = startPage > 1;
        boolean showEndEllipsis = endPage < totalPages - 2;

        model.addAttribute("talentos", paginaTalentos.getContent());
        model.addAttribute("currentPage", currentPage);
        model.addAttribute("totalPages", totalPages);
        model.addAttribute("totalTalentos", paginaTalentos.getTotalElements());
        model.addAttribute("pageSize", size);
        model.addAttribute("query", query);
        model.addAttribute("startPage", startPage);
        model.addAttribute("endPage", endPage);
        model.addAttribute("showStartEllipsis", showStartEllipsis);
        model.addAttribute("showEndEllipsis", showEndEllipsis);
        model.addAttribute("esEmpresa", RoleGuard.esEmpresa(session));
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
    }
}