package com.example.devcoast.controller;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.example.devcoast.config.RoleGuard;
import com.example.devcoast.model.Empresa;
import com.example.devcoast.model.Usuario;
import com.example.devcoast.repository.EmpresaRepository;
import com.example.devcoast.repository.UsuarioRepository;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

@Controller
public class AuthController {

    private final UsuarioRepository usuarioRepository;
    private final EmpresaRepository empresaRepository;
    private final BCryptPasswordEncoder passwordEncoder;

    public AuthController(UsuarioRepository usuarioRepository, EmpresaRepository empresaRepository,
            BCryptPasswordEncoder passwordEncoder) {
        this.usuarioRepository = usuarioRepository;
        this.empresaRepository = empresaRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @GetMapping("/login")
    public String loginPage(HttpSession session) {
        if (!RoleGuard.sinSesion(session))
            return "redirect:/dashboard";
        return "login";
    }

    @PostMapping("/login")
    public String login(
            @RequestParam String correo,
            @RequestParam String password,
            HttpServletRequest request,
            HttpSession session,
            RedirectAttributes ra) {

        String correoNorm = correo.trim().toLowerCase();
        Usuario usuario = usuarioRepository.findByCorreo(correoNorm);
        if (usuario != null && passwordEncoder.matches(password, usuario.getPassword())) {
            // regenero la sesión para evitar session fixation
            session.invalidate();
            session = request.getSession(true);
            poblarSesion(session,
                    usuario.getId(),
                    usuario.getCorreo(),
                    usuario.getNombre() != null ? usuario.getNombre() : "Usuario",
                    usuario.getTipo() != null ? usuario.getTipo() : RoleGuard.TIPO_TALENTO,
                    usuario.getFotoUrl());

            boolean perfilVacio = (usuario.getBio() == null || usuario.getBio().isBlank())
                    && (usuario.getStack() == null || usuario.getStack().isEmpty());
            if (perfilVacio) {
                session.setAttribute("mostrarOnboarding", true);
            }

            return "redirect:/dashboard";
        }

        Empresa empresa = empresaRepository.findByCorreo(correoNorm);
        if (empresa != null && passwordEncoder.matches(password, empresa.getPassword())) {
            session.invalidate();
            session = request.getSession(true);
            poblarSesion(session,
                    empresa.getId(),
                    empresa.getCorreo(),
                    empresa.getNombreEmpresa(),
                    RoleGuard.TIPO_EMPRESA,
                    empresa.getFotoUrl());
            return "redirect:/dashboard";
        }

        ra.addFlashAttribute("flashMessage", "Correo o contraseña incorrectos");
        ra.addFlashAttribute("flashType", "error");
        return "redirect:/login";
    }

    @GetMapping("/logout")
    public String logout(HttpSession session) {
        session.invalidate();
        return "redirect:/login";
    }

    private void poblarSesion(HttpSession session, String id, String correo,
            String nombre, String tipo, String fotoUrl) {
        session.setAttribute("usuarioActualId", id);
        session.setAttribute("usuarioActualCorreo", correo);
        session.setAttribute("usuarioActualNombre", nombre);
        session.setAttribute("usuarioActualTipo", tipo);
        if (fotoUrl != null && !fotoUrl.isBlank()) {
            session.setAttribute("usuarioActualFotoUrl", fotoUrl);
        } else {
            session.removeAttribute("usuarioActualFotoUrl");
        }
    }
}
