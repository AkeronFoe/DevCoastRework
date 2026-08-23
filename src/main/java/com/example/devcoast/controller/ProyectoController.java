package com.example.devcoast.controller;

import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.example.devcoast.config.RoleGuard;
import com.example.devcoast.model.Proyecto;
import com.example.devcoast.repository.ProyectoRepository;

import jakarta.servlet.http.HttpSession;

@Controller
public class ProyectoController {

    private static final String BANNER_DEFAULT = "cover-ocean";
    private static final String ICONO_DEFAULT = "layers";
    private static final String COLOR_ICONO_DEFAULT = "#1f8f8f";

    private final ProyectoRepository proyectoRepository;

    public ProyectoController(ProyectoRepository proyectoRepository) {
        this.proyectoRepository = proyectoRepository;
    }

    @GetMapping("/misProyectos")
    public String listarProyectos(Model model, HttpSession session, RedirectAttributes ra) {
        if (RoleGuard.soloTalento(session)) {
            if (RoleGuard.sinSesion(session))
                return "redirect:/login";
            ra.addFlashAttribute("flashMessage",
                    "Las empresas no tienen proyectos. ¿Querías ver tus empleos publicados?");
            ra.addFlashAttribute("flashType", "info");
            return "redirect:/misEmpleos";
        }
        List<Proyecto> proyectos = proyectoRepository
                .findByUsuarioIdOrderByCreadoEnDesc(RoleGuard.getId(session));
        model.addAttribute("proyectos", proyectos);
        agregarSesion(session, model);
        return "misProyectos";
    }

    @GetMapping("/subirProyecto")
    public String mostrarFormulario(@RequestParam(required = false) String editar,
            HttpSession session, Model model, RedirectAttributes ra) {
        if (RoleGuard.soloTalento(session)) {
            if (RoleGuard.sinSesion(session))
                return "redirect:/login";
            ra.addFlashAttribute("flashMessage", "Las empresas no pueden subir proyectos");
            ra.addFlashAttribute("flashType", "warning");
            return "redirect:/dashboard";
        }

        Proyecto proyecto = new Proyecto();
        if (editar != null && !editar.isBlank()) {
            Optional<Proyecto> existente = proyectoRepository.findByIdAndUsuarioId(editar, RoleGuard.getId(session));
            if (existente.isEmpty()) {
                ra.addFlashAttribute("flashMessage", "No tienes permiso para editar ese proyecto");
                ra.addFlashAttribute("flashType", "error");
                return "redirect:/misProyectos";
            }
            proyecto = existente.get();
        }

        model.addAttribute("proyecto", proyecto);
        agregarSesion(session, model);
        return "subirProyecto";
    }

    @PostMapping("/proyectos/guardar")
    public String guardarProyecto(
            @RequestParam(required = false) String id,
            @RequestParam String nombre,
            @RequestParam String categoria,
            @RequestParam String tipo,
            @RequestParam String descripcion,
            @RequestParam(required = false) String anio,
            @RequestParam(required = false) String linkProyecto,
            @RequestParam(required = false) String bannerClase,
            @RequestParam(required = false) String bannerDataUrl,
            @RequestParam(required = false) String iconoClave,
            @RequestParam(required = false) String colorIcono,
            @RequestParam(required = false) String logoDataUrl,
            @RequestParam(required = false) String stack,
            @RequestParam(required = false) String etiquetas,
            @RequestParam(required = false) String rol,
            @RequestParam(required = false) String duracion,
            @RequestParam(required = false) String descripcionExtendida,
            @RequestParam(defaultValue = "true") boolean publicar,
            @RequestParam(defaultValue = "true") boolean incluirCv,
            @RequestParam(defaultValue = "false") boolean destacar,
            HttpSession session, RedirectAttributes ra) {

        if (RoleGuard.soloTalento(session))
            return "redirect:/login";

        if (isBlank(nombre) || isBlank(categoria) || isBlank(tipo) || isBlank(descripcion)) {
            ra.addFlashAttribute("flashMessage", "Completa nombre, categoría, tipo y descripción del proyecto");
            ra.addFlashAttribute("flashType", "error");
            return "redirect:/subirProyecto" + (id != null && !id.isBlank() ? "?editar=" + id : "");
        }

        String usuarioId = RoleGuard.getId(session);
        boolean esEdicion = id != null && !id.isBlank();
        Proyecto p;

        if (esEdicion) {
            Optional<Proyecto> existente = proyectoRepository.findByIdAndUsuarioId(id, usuarioId);
            if (existente.isEmpty()) {
                ra.addFlashAttribute("flashMessage", "No tienes permiso para editar ese proyecto");
                ra.addFlashAttribute("flashType", "error");
                return "redirect:/misProyectos";
            }
            p = existente.get();
        } else {
            p = new Proyecto();
            p.setUsuarioId(usuarioId);
            p.setUsuarioCorreo(RoleGuard.getCorreo(session));
            p.setCreadoEn(new Date());
        }

        p.setNombre(nombre.trim());
        p.setCategoria(categoria.trim());
        p.setTipo(tipo.trim());
        p.setDescripcionCorta(descripcion.trim().length() > 200 ? descripcion.trim().substring(0, 200) : descripcion.trim());
        p.setAnio(safeTrim(anio));
        p.setLinkProyecto(safeTrim(linkProyecto));
        p.setBannerClase(isBlank(bannerClase) ? BANNER_DEFAULT : bannerClase.trim());
        p.setBannerDataUrl(safeTrim(bannerDataUrl));
        p.setIconoClave(isBlank(iconoClave) ? ICONO_DEFAULT : iconoClave.trim());
        p.setColorIcono(isBlank(colorIcono) ? COLOR_ICONO_DEFAULT : colorIcono.trim());
        p.setLogoDataUrl(safeTrim(logoDataUrl));
        p.setStack(splitCsv(stack));
        p.setEtiquetas(splitCsv(etiquetas));
        p.setRol(safeTrim(rol));
        p.setDuracion(safeTrim(duracion));
        p.setDescripcionExtendida(safeTrim(descripcionExtendida));
        p.setPublicado(publicar);
        p.setIncluirCv(incluirCv);
        p.setDestacar(destacar);
        p.setActualizadoEn(new Date());
        proyectoRepository.save(p);

        ra.addFlashAttribute("flashMessage",
                esEdicion ? "Proyecto actualizado correctamente"
                        : (publicar ? "Proyecto publicado correctamente" : "Proyecto guardado como borrador"));
        ra.addFlashAttribute("flashType", "success");
        return "redirect:/misProyectos";
    }

    @GetMapping("/misProyectos/{id}")
    public String verDetalle(@PathVariable String id, Model model, HttpSession session) {
        if (RoleGuard.soloTalento(session)) {
            return RoleGuard.sinSesion(session) ? "redirect:/login" : "redirect:/dashboard";
        }
        Optional<Proyecto> proyecto = proyectoRepository
                .findByIdAndUsuarioId(id, RoleGuard.getId(session));
        if (proyecto.isEmpty())
            return "redirect:/misProyectos";
        model.addAttribute("proyecto", proyecto.get());
        agregarSesion(session, model);
        return "detalle-proyecto";
    }

    @PostMapping("/proyectos/eliminar/{id}")
    public String eliminar(@PathVariable String id, HttpSession session, RedirectAttributes ra) {
        if (RoleGuard.soloTalento(session))
            return "redirect:/login";
        String uid = RoleGuard.getId(session);
        Optional<Proyecto> maybe = proyectoRepository.findById(id);
        if (maybe.isEmpty()) {
            ra.addFlashAttribute("flashMessage", "Proyecto no encontrado");
            ra.addFlashAttribute("flashType", "error");
            return "redirect:/misProyectos";
        }
        if (!uid.equals(maybe.get().getUsuarioId())) {
            ra.addFlashAttribute("flashMessage", "No tienes permiso para eliminar este proyecto");
            ra.addFlashAttribute("flashType", "error");
            return "redirect:/misProyectos";
        }
        proyectoRepository.deleteById(id);
        ra.addFlashAttribute("flashMessage", "Proyecto eliminado correctamente");
        ra.addFlashAttribute("flashType", "success");
        return "redirect:/misProyectos";
    }

    private void agregarSesion(HttpSession session, Model model) {
        model.addAttribute("usuarioActualId", RoleGuard.getId(session));
        model.addAttribute("usuarioActualCorreo", RoleGuard.getCorreo(session));
        model.addAttribute("usuarioActualNombre", RoleGuard.getNombre(session));
        model.addAttribute("usuarioActualTipo", RoleGuard.getTipo(session));
        model.addAttribute("usuarioActualFotoUrl", session.getAttribute("usuarioActualFotoUrl"));
    }

    private static List<String> splitCsv(String value) {
        if (isBlank(value))
            return List.of();
        return Arrays.stream(value.split(","))
                .map(String::trim).filter(s -> !s.isEmpty())
                .distinct().collect(Collectors.toList());
    }

    private static String safeTrim(String v) {
        return isBlank(v) ? null : v.trim();
    }

    private static boolean isBlank(String v) {
        return v == null || v.trim().isEmpty();
    }
}