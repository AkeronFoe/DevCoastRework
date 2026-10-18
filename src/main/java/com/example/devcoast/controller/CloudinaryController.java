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

import com.example.devcoast.config.RoleGuard;
import com.example.devcoast.model.Empresa;
import com.example.devcoast.model.Proyecto;
import com.example.devcoast.model.Usuario;
import com.example.devcoast.repository.EmpresaRepository;
import com.example.devcoast.repository.ProyectoRepository;
import com.example.devcoast.repository.UsuarioRepository;
import com.example.devcoast.service.CloudinaryService;

import jakarta.servlet.http.HttpSession;

@RestController
public class CloudinaryController {

    private final CloudinaryService cloudinaryService;
    private final UsuarioRepository usuarioRepository;
    private final EmpresaRepository empresaRepository;
    private final ProyectoRepository proyectoRepository;

    public CloudinaryController(CloudinaryService cloudinaryService, UsuarioRepository usuarioRepository, EmpresaRepository empresaRepository, ProyectoRepository proyectoRepository) {
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
        String problema = problemaConArchivo(file);
        if (problema != null)
            return ResponseEntity.badRequest().body(Map.of("ok", false, "error", problema));

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
                    .body(Map.of("ok", false, "error", "Error al subir imagen: " + e.getMessage()));
        }
    }

    @PostMapping("/perfil/foto/quitar")
    public ResponseEntity<Map<String, Object>> quitarFotoPerfil(HttpSession session) {
        String id = (String) session.getAttribute("usuarioActualId");
        String tipo = (String) session.getAttribute("usuarioActualTipo");

        if (id == null)
            return ResponseEntity.status(401).body(Map.of("ok", false, "error", "No autenticado"));

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
        String problema = problemaConArchivo(file);
        if (problema != null)
            return ResponseEntity.badRequest().body(Map.of("ok", false, "error", problema));

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
                    .body(Map.of("ok", false, "error", "Error al subir imagen: " + e.getMessage()));
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
        String problema = problemaConArchivo(file);
        if (problema != null)
            return ResponseEntity.badRequest().body(Map.of("ok", false, "error", problema));

        try {
            String url = cloudinaryService.subirImagenProyecto(file);
            return ResponseEntity.ok(Map.of("ok", true, "url", url));
        } catch (IOException e) {
            return ResponseEntity.status(500)
                    .body(Map.of("ok", false, "error", "Error al subir logo: " + e.getMessage()));
        }
    }

    private static final long MAX_IMAGEN = 5L * 1024 * 1024;

    private static String problemaConArchivo(MultipartFile file) {
        if (file.isEmpty())
            return "Archivo vacío";
        String tipo = file.getContentType();
        if (tipo == null || !tipo.toLowerCase().startsWith("image/"))
            return "El archivo debe ser una imagen";
        if (file.getSize() > MAX_IMAGEN)
            return "La imagen pesa más de 5 MB";
        return null;
    }
}
