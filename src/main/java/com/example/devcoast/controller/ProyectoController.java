package com.example.devcoast.controller;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.example.devcoast.config.RoleGuard;
import com.example.devcoast.model.Proyecto;
import com.example.devcoast.repository.ProyectoRepository;
import com.example.devcoast.service.AreaEmpleo;

import jakarta.servlet.http.HttpSession;

@Controller
public class ProyectoController {

    private static final String BANNER_DEFAULT = "cover-terminal";
    private static final String ICONO_DEFAULT = "layers";
    private static final String COLOR_ICONO_DEFAULT = "#1f8f8f";

    private final ProyectoRepository proyectoRepository;

    public ProyectoController(ProyectoRepository proyectoRepository) {
        this.proyectoRepository = proyectoRepository;
    }

    // Portafolio del talento: sus proyectos (públicos u ocultos) como tarjetas.
    // ?area=programacion|diseno|otras &vis=1|0 &q=   (la lista es del propio usuario, así que se filtra en memoria)
    @GetMapping("/proyectos")
    public String vitrinaProyectos(
            Model model, HttpSession session, RedirectAttributes ra,
            @RequestParam(required = false) String area,
            @RequestParam(required = false) String vis,
            @RequestParam(required = false) String q) {

        if (RoleGuard.soloTalento(session)) {
            if (RoleGuard.sinSesion(session))
                return "redirect:/login";
            ra.addFlashAttribute("flashMessage",
                    "Las empresas no tienen proyectos. ¿Querías ver tus empleos publicados?");
            ra.addFlashAttribute("flashType", "info");
            return "redirect:/misEmpleos";
        }

        AreaEmpleo.Area areaSel = AreaEmpleo.Area.deClave(area);
        String visSel = "1".equals(vis) || "0".equals(vis) ? vis : "";
        String qSel = q == null ? "" : q.trim();
        String qNorm = normalizar(qSel);

        List<ProyectoVista> todos = proyectoRepository.findByUsuarioIdOrderByCreadoEnDesc(RoleGuard.getId(session))
                .stream().map(ProyectoVista::de).collect(Collectors.toList());
        List<ProyectoVista> lista = new ArrayList<>();
        for (ProyectoVista p : todos) {
            if (areaSel != null && !areaClave(areaSel).equals(p.getAreaClave()))
                continue;
            if (!visSel.isEmpty() && ("1".equals(visSel) != p.isPublicado()))
                continue;
            if (!qNorm.isEmpty() && !normalizar(p.getNombre() + " " + p.getCategoria() + " " + p.getDescripcion() + " "
                    + String.join(" ", p.getHerramientas())).contains(qNorm))
                continue;
            lista.add(p);
        }

        long publicos = todos.stream().filter(ProyectoVista::isPublicado).count();
        boolean hayFiltros = areaSel != null || !visSel.isEmpty() || !qSel.isEmpty();

        model.addAttribute("proyectos", lista);
        model.addAttribute("totalProyectos", todos.size());
        model.addAttribute("totalPublicos", publicos);
        model.addAttribute("area", areaSel == null ? "" : areaSel.clave());
        model.addAttribute("areaP", areaSel == null ? null : areaSel.clave());
        model.addAttribute("vis", visSel);
        model.addAttribute("visP", visSel.isEmpty() ? null : visSel);
        model.addAttribute("q", qSel.isEmpty() ? null : qSel);
        model.addAttribute("hayFiltros", hayFiltros);
        model.addAttribute("anioActual", java.time.Year.now().getValue());
        agregarSesion(session, model);
        return "proyectos";
    }

    private static String areaClave(AreaEmpleo.Area a) {
        return a == AreaEmpleo.Area.PROGRAMACION ? "p" : a == AreaEmpleo.Area.DISENO ? "d" : "o";
    }

    private static String normalizar(String s) {
        return java.text.Normalizer.normalize(s == null ? "" : s, java.text.Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "").toLowerCase().trim();
    }

    // a dónde se vuelve después de guardar o eliminar: la nueva página del portafolio o la lista de siempre
    private static String destino(String volver) {
        return "proyectos".equals(volver) ? "redirect:/proyectos" : "redirect:/misProyectos";
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
            @RequestParam(required = false) String volver,
            HttpSession session, RedirectAttributes ra) {

        if (RoleGuard.soloTalento(session))
            return "redirect:/login";

        if (isBlank(nombre) || isBlank(categoria) || isBlank(tipo) || isBlank(descripcion)) {
            ra.addFlashAttribute("flashMessage", "Completa nombre, categoría, tipo y descripción del proyecto");
            ra.addFlashAttribute("flashType", "error");
            return "proyectos".equals(volver) ? "redirect:/proyectos"
                    : "redirect:/subirProyecto" + (id != null && !id.isBlank() ? "?editar=" + id : "");
        }
        if (!isBlank(linkProyecto) && ProyectoVista.urlSegura(linkProyecto) == null) {
            ra.addFlashAttribute("flashMessage", "El enlace del proyecto debe empezar con http:// o https://");
            ra.addFlashAttribute("flashType", "error");
            return "proyectos".equals(volver) ? "redirect:/proyectos"
                    : "redirect:/subirProyecto" + (id != null && !id.isBlank() ? "?editar=" + id : "");
        }

        String usuarioId = RoleGuard.getId(session);
        boolean esEdicion = id != null && !id.isBlank();
        Proyecto p;

        if (esEdicion) {
            Optional<Proyecto> existente = proyectoRepository.findByIdAndUsuarioId(id, usuarioId);
            if (existente.isEmpty()) {
                ra.addFlashAttribute("flashMessage", "No tienes permiso para editar ese proyecto");
                ra.addFlashAttribute("flashType", "error");
                return destino(volver);
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
        return destino(volver);
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
    public String eliminar(@PathVariable String id, @RequestParam(required = false) String volver,
            HttpSession session, RedirectAttributes ra) {
        if (RoleGuard.soloTalento(session))
            return "redirect:/login";
        String uid = RoleGuard.getId(session);
        Optional<Proyecto> maybe = proyectoRepository.findById(id);
        if (maybe.isEmpty()) {
            ra.addFlashAttribute("flashMessage", "Proyecto no encontrado");
            ra.addFlashAttribute("flashType", "error");
            return destino(volver);
        }
        if (!uid.equals(maybe.get().getUsuarioId())) {
            ra.addFlashAttribute("flashMessage", "No tienes permiso para eliminar este proyecto");
            ra.addFlashAttribute("flashType", "error");
            return destino(volver);
        }
        proyectoRepository.deleteById(id);
        ra.addFlashAttribute("flashMessage", "Proyecto eliminado correctamente");
        ra.addFlashAttribute("flashType", "success");
        return destino(volver);
    }

    // Edición rápida desde el diálogo del portafolio: solo toca los campos del diálogo
    // (el editor completo, /subirProyecto, sigue siendo el único que cambia banner, rol, duración, etc.)
    @PostMapping("/proyectos/{id}/actualizar")
    public String actualizar(@PathVariable String id,
            @RequestParam String nombre, @RequestParam String categoria, @RequestParam String tipo,
            @RequestParam String descripcion,
            @RequestParam(required = false) String anio,
            @RequestParam(required = false) String stack,
            @RequestParam(required = false) String linkProyecto,
            @RequestParam(required = false) String volver,
            HttpSession session, RedirectAttributes ra) {
        if (RoleGuard.soloTalento(session))
            return "redirect:/login";

        Optional<Proyecto> existente = proyectoRepository.findByIdAndUsuarioId(id, RoleGuard.getId(session));
        if (existente.isEmpty()) {
            ra.addFlashAttribute("flashMessage", "No tienes permiso para editar ese proyecto");
            ra.addFlashAttribute("flashType", "error");
            return destino(volver);
        }
        if (isBlank(nombre) || isBlank(categoria) || isBlank(tipo) || isBlank(descripcion)) {
            ra.addFlashAttribute("flashMessage", "Completa nombre, categoría, tipo y descripción del proyecto");
            ra.addFlashAttribute("flashType", "error");
            return destino(volver);
        }
        if (!isBlank(linkProyecto) && ProyectoVista.urlSegura(linkProyecto) == null) {
            ra.addFlashAttribute("flashMessage", "El enlace del proyecto debe empezar con http:// o https://");
            ra.addFlashAttribute("flashType", "error");
            return destino(volver);
        }

        Proyecto p = existente.get();
        p.setNombre(nombre.trim());
        p.setCategoria(categoria.trim());
        p.setTipo(tipo.trim());
        p.setDescripcionCorta(descripcion.trim().length() > 200 ? descripcion.trim().substring(0, 200) : descripcion.trim());
        p.setAnio(safeTrim(anio));
        p.setStack(splitCsv(stack));
        p.setLinkProyecto(safeTrim(linkProyecto));
        p.setActualizadoEn(new Date());
        proyectoRepository.save(p);

        ra.addFlashAttribute("flashMessage", "Proyecto actualizado correctamente");
        ra.addFlashAttribute("flashType", "success");
        return destino(volver);
    }

    // Interruptor PÚBLICO / OCULTO. Con Accept: application/json (el interruptor de la página) responde JSON;
    // el formulario sin JavaScript recibe una redirección.
    @PostMapping("/proyectos/{id}/visibilidad")
    public Object visibilidad(@PathVariable String id, @RequestParam boolean publicado,
            @RequestHeader(value = "Accept", required = false) String accept,
            HttpSession session, RedirectAttributes ra) {
        boolean json = accept != null && accept.contains("application/json");
        if (RoleGuard.sinSesion(session))
            return json ? ResponseEntity.status(401).body(Map.of("ok", false, "mensaje", "Inicia sesión.")) : "redirect:/login";
        if (RoleGuard.soloTalento(session))
            return json ? ResponseEntity.status(403).body(Map.of("ok", false, "mensaje", "Las empresas no tienen proyectos.")) : "redirect:/dashboard";

        Optional<Proyecto> existente = proyectoRepository.findByIdAndUsuarioId(id, RoleGuard.getId(session));
        if (existente.isEmpty())
            return json ? ResponseEntity.status(404).body(Map.of("ok", false, "mensaje", "Proyecto no encontrado.")) : "redirect:/proyectos";

        Proyecto p = existente.get();
        p.setPublicado(publicado);
        p.setActualizadoEn(new Date());
        proyectoRepository.save(p);
        if (json)
            return ResponseEntity.ok(Map.of("ok", true, "publicado", publicado));
        ra.addFlashAttribute("flashMessage", publicado ? "Ahora “" + p.getNombre() + "” es público." : "“" + p.getNombre() + "” quedó oculto.");
        ra.addFlashAttribute("flashType", "success");
        return "redirect:/proyectos";
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