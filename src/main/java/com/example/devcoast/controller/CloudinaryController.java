package com.example.devcoast.controller;

import java.io.IOException;
import java.util.Map;
import java.util.Optional;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.example.devcoast.config.LimiteTasa;
import com.example.devcoast.config.RoleGuard;
import com.example.devcoast.model.Empresa;
import com.example.devcoast.model.Proyecto;
import com.example.devcoast.model.Usuario;
import com.example.devcoast.repository.EmpresaRepository;
import com.example.devcoast.repository.ProyectoRepository;
import com.example.devcoast.repository.UsuarioRepository;
import com.example.devcoast.service.CloudinaryService;
import com.example.devcoast.service.ValidadorImagen;

import jakarta.servlet.http.HttpSession;

@RestController
public class CloudinaryController {

    private final CloudinaryService cloudinaryService;
    private final UsuarioRepository usuarioRepository;
    private final EmpresaRepository empresaRepository;
    private final ProyectoRepository proyectoRepository;
    private final LimiteTasa limiteTasa;

    public CloudinaryController(CloudinaryService cloudinaryService, UsuarioRepository usuarioRepository,
            EmpresaRepository empresaRepository, ProyectoRepository proyectoRepository, LimiteTasa limiteTasa) {
        this.limiteTasa = limiteTasa;
        this.cloudinaryService = cloudinaryService;
        this.usuarioRepository = usuarioRepository;
        this.empresaRepository = empresaRepository;
        this.proyectoRepository = proyectoRepository;
    }

    @PostMapping("/perfil/foto")
    public ResponseEntity<Map<String, Object>> subirFotoPerfil(
            @RequestParam("file") MultipartFile file,
            HttpSession session) {

        String id = (String) session.getAttribute("usuarioActualId");
        String tipo = (String) session.getAttribute("usuarioActualTipo");

        if (id == null)
            return ResponseEntity.status(401).body(Map.of("ok", false, "error", "No autenticado"));
        ResponseEntity<Map<String, Object>> rechazo = rechazarSubida(file, id, session);
        if (rechazo != null)
            return rechazo;

        try {
            String url = cloudinaryService.subirFotoPerfil(file, id);

            if ("empresa".equals(tipo)) {
                Empresa empresa = empresaRepository.findById(id).orElse(null);
                if (empresa != null) {
                    empresa.setFotoUrl(url);
                    empresaRepository.save(empresa);
                }
            } else {
                Usuario usuario = usuarioRepository.findById(id).orElse(null);
                if (usuario != null) {
                    usuario.setFotoUrl(url);
                    usuarioRepository.save(usuario);
                }
            }

            session.setAttribute("usuarioActualFotoUrl", url);
            return ResponseEntity.ok(Map.of("ok", true, "url", url));

        } catch (IOException e) {
            return ResponseEntity.status(500)
                    .body(Map.of("ok", false, "error", "No se pudo subir la imagen. Inténtalo de nuevo."));
        }
    }

    @PostMapping("/perfil/foto/quitar")
    public ResponseEntity<Map<String, Object>> quitarFotoPerfil(HttpSession session) {
        String id = (String) session.getAttribute("usuarioActualId");
        String tipo = (String) session.getAttribute("usuarioActualTipo");

        if (id == null)
            return ResponseEntity.status(401).body(Map.of("ok", false, "error", "No autenticado"));
        if (Boolean.TRUE.equals(session.getAttribute("modoDemo")))
            return ResponseEntity.status(403).body(Map.of("ok", false, "error",
                    "En la demo no se puede cambiar la foto: las cuentas son compartidas."));

        if ("empresa".equals(tipo)) {
            Empresa empresa = empresaRepository.findById(id).orElse(null);
            if (empresa != null) {
                empresa.setFotoUrl(null);
                empresaRepository.save(empresa);
            }
        } else {
            Usuario usuario = usuarioRepository.findById(id).orElse(null);
            if (usuario != null) {
                usuario.setFotoUrl(null);
                usuarioRepository.save(usuario);
            }
        }

        session.removeAttribute("usuarioActualFotoUrl");
        return ResponseEntity.ok(Map.of("ok", true));
    }

    @PostMapping("/proyectos/{id}/imagen")
    public ResponseEntity<Map<String, Object>> subirImagenProyecto(
            @PathVariable String id,
            @RequestParam("file") MultipartFile file,
            HttpSession session) {

        String usuarioId = RoleGuard.getId(session);
        if (usuarioId == null)
            return ResponseEntity.status(401).body(Map.of("ok", false, "error", "No autenticado"));
        ResponseEntity<Map<String, Object>> rechazo = rechazarSubida(file, usuarioId, session);
        if (rechazo != null)
            return rechazo;

        Optional<Proyecto> maybe = proyectoRepository.findByIdAndUsuarioId(id, usuarioId);
        if (maybe.isEmpty())
            return ResponseEntity.status(404).body(Map.of("ok", false, "error", "Proyecto no encontrado"));

        try {
            String url = cloudinaryService.subirImagenProyecto(file);
            Proyecto proyecto = maybe.get();
            proyecto.setImagenUrl(url);
            proyectoRepository.save(proyecto);
            return ResponseEntity.ok(Map.of("ok", true, "url", url));
        } catch (IOException e) {
            return ResponseEntity.status(500)
                    .body(Map.of("ok", false, "error", "No se pudo subir la imagen. Inténtalo de nuevo."));
        }
    }

    @PostMapping("/proyectos/{id}/logo/quitar")
    public ResponseEntity<Map<String, Object>> quitarLogoProyecto(
            @PathVariable String id,
            HttpSession session) {

        String usuarioId = RoleGuard.getId(session);
        if (usuarioId == null)
            return ResponseEntity.status(401).body(Map.of("ok", false, "error", "No autenticado"));

        Optional<Proyecto> maybe = proyectoRepository.findByIdAndUsuarioId(id, usuarioId);
        if (maybe.isEmpty())
            return ResponseEntity.status(404).body(Map.of("ok", false, "error", "Proyecto no encontrado"));

        Proyecto proyecto = maybe.get();
        proyecto.setLogoDataUrl(null);
        proyectoRepository.save(proyecto);
        return ResponseEntity.ok(Map.of("ok", true));
    }

    @PostMapping("/proyectos/logo/temp")
    public ResponseEntity<Map<String, Object>> subirLogoTemporal(
            @RequestParam("file") MultipartFile file,
            HttpSession session) {

        String id = (String) session.getAttribute("usuarioActualId");
        if (id == null)
            return ResponseEntity.status(401).body(Map.of("ok", false, "error", "No autenticado"));
        ResponseEntity<Map<String, Object>> rechazo = rechazarSubida(file, id, session);
        if (rechazo != null)
            return rechazo;

        try {
            String url = cloudinaryService.subirImagenProyecto(file);
            return ResponseEntity.ok(Map.of("ok", true, "url", url));
        } catch (IOException e) {
            return ResponseEntity.status(500)
                    .body(Map.of("ok", false, "error", "No se pudo subir el logo. Inténtalo de nuevo."));
        }
    }

    private static final int MAX_SUBIDAS = 20;
    private static final long VENTANA_SUBIDAS_MS = 10 * 60 * 1000L;

    private ResponseEntity<Map<String, Object>> rechazarSubida(MultipartFile file, String id, HttpSession session) {
        if (Boolean.TRUE.equals(session.getAttribute("modoDemo")))
            return ResponseEntity.status(403).body(Map.of("ok", false, "error",
                    "En la demo no se pueden subir imágenes: las cuentas son compartidas."));
        if (limiteTasa.excedido("subida|" + id, MAX_SUBIDAS, VENTANA_SUBIDAS_MS))
            return ResponseEntity.status(429).body(Map.of("ok", false, "error",
                    "Estás subiendo muchas imágenes seguidas. Espera unos minutos."));
        if (file.isEmpty())
            return ResponseEntity.badRequest().body(Map.of("ok", false, "error", "Archivo vacío"));
        if (file.getSize() > ValidadorImagen.MAX_BYTES)
            return ResponseEntity.badRequest().body(Map.of("ok", false, "error", "La imagen pesa más de 5 MB"));
        try {
            String problema = ValidadorImagen.problema(file.getBytes());
            if (problema != null)
                return ResponseEntity.badRequest().body(Map.of("ok", false, "error", problema));
        } catch (IOException e) {
            return ResponseEntity.badRequest().body(Map.of("ok", false, "error", "No se pudo leer el archivo"));
        }
        return null;
    }
}
