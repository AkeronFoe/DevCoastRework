package com.example.devcoast.controller;

import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.example.devcoast.config.LimiteTasa;
import com.example.devcoast.config.RoleGuard;
import com.example.devcoast.model.Empresa;
import com.example.devcoast.model.Usuario;
import com.example.devcoast.repository.EmpresaRepository;
import com.example.devcoast.repository.UsuarioRepository;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

@Controller
public class RegistroController {

    private final UsuarioRepository usuarioRepository;

    private final EmpresaRepository empresaRepository;

    private final BCryptPasswordEncoder passwordEncoder;

    private final LimiteTasa limiteTasa;

    // la gente de una feria puede registrarse desde una misma red: por eso el tope es holgado
    private static final int MAX_REGISTROS = 40;
    private static final long VENTANA_REGISTROS_MS = 15 * 60 * 1000L;

    public RegistroController(UsuarioRepository usuarioRepository, EmpresaRepository empresaRepository,
            BCryptPasswordEncoder passwordEncoder, LimiteTasa limiteTasa) {
        this.limiteTasa = limiteTasa;
        this.usuarioRepository = usuarioRepository;
        this.empresaRepository = empresaRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @GetMapping("/register/talento")
    public String registrarTalentoPage(Model model) {
        model.asMap().putIfAbsent("fNombre", "");
        model.asMap().putIfAbsent("fRolTitulo", "");
        model.asMap().putIfAbsent("fBio", "");
        model.asMap().putIfAbsent("fStack", "");
        model.asMap().putIfAbsent("fEspecialidad", "");
        model.asMap().putIfAbsent("fModalidad", "");
        model.asMap().putIfAbsent("fSalarioEsperado", "");
        model.asMap().putIfAbsent("fDisponibilidad", "");
        model.asMap().putIfAbsent("fCorreo", "");
        model.asMap().putIfAbsent("fEmailContacto", "");
        model.asMap().putIfAbsent("fTelefono", "");
        model.asMap().putIfAbsent("fPortfolioUrl", "");
        model.asMap().putIfAbsent("fLinkedinUrl", "");
        model.addAttribute("tipoPreset", "talento");
        return "register";
    }

    @GetMapping("/register/empresa")
    public String registrarEmpresaPage(Model model) {
        model.asMap().putIfAbsent("fNombreEmpresa", "");
        model.asMap().putIfAbsent("fNombreRepresentante", "");
        model.asMap().putIfAbsent("fCargoRepresentante", "");
        model.asMap().putIfAbsent("fSector", "");
        model.asMap().putIfAbsent("fTamano", "");
        model.asMap().putIfAbsent("fSitioWeb", "");
        model.asMap().putIfAbsent("fCorreo", "");
        model.asMap().putIfAbsent("fTelefono", "");
        model.asMap().putIfAbsent("fDescripcion", "");
        model.addAttribute("tipoPreset", "empresa");
        return "register";
    }

    @PostMapping("/register/talento")
    public String registrarTalento(
            @RequestParam String nombre,
            @RequestParam String rolTitulo,
            @RequestParam(required = false) String bio,
            @RequestParam String stack,
            @RequestParam(required = false) String especialidad,
            @RequestParam(required = false) String modalidad,
            @RequestParam(required = false) String salarioEsperado,
            @RequestParam(required = false) String disponibilidad,
            @RequestParam String correo,
            @RequestParam(required = false) String emailContacto,
            @RequestParam(required = false) String telefono,
            @RequestParam(required = false) String portfolioUrl,
            @RequestParam(required = false) String linkedinUrl,
            @RequestParam String password,
            HttpServletRequest request,
            HttpSession session,
            RedirectAttributes redirectAttributes) {

        String correoNorm = correo.trim().toLowerCase();

        if (limiteTasa.excedido("registro|" + request.getRemoteAddr(), MAX_REGISTROS, VENTANA_REGISTROS_MS)) {
            reflashTalento(redirectAttributes, nombre, rolTitulo, bio, stack, especialidad, modalidad,
                    salarioEsperado, disponibilidad, correo, emailContacto, telefono, portfolioUrl, linkedinUrl);
            return "redirect:/register/talento?error=limite";
        }
        if (!correoValido(correoNorm)
                || usuarioRepository.findByCorreo(correoNorm) != null
                || empresaRepository.findByCorreo(correoNorm) != null) {
            reflashTalento(redirectAttributes, nombre, rolTitulo, bio, stack, especialidad, modalidad,
                    salarioEsperado, disponibilidad, correo, emailContacto, telefono, portfolioUrl, linkedinUrl);
            return "redirect:/register/talento?error=correo";
        }
        if (!passwordValida(password)) {
            reflashTalento(redirectAttributes, nombre, rolTitulo, bio, stack, especialidad, modalidad,
                    salarioEsperado, disponibilidad, correo, emailContacto, telefono, portfolioUrl, linkedinUrl);
            return "redirect:/register/talento?error=password";
        }

        Usuario usuario = new Usuario();
        usuario.setTipo("talento");
        usuario.setNombre(nombre.trim());
        usuario.setRolTitulo(rolTitulo.trim());
        usuario.setBio(bio);
        usuario.setStack(parseStack(stack));
        usuario.setEspecialidad(especialidad);
        usuario.setModalidad(modalidad);
        usuario.setSalarioEsperado(salarioEsperado);
        usuario.setDisponibilidad(disponibilidad);
        usuario.setCorreo(correoNorm);
        usuario.setEmailContacto(
                emailContacto != null && !emailContacto.isBlank() ? emailContacto.trim() : correoNorm);
        usuario.setTelefono(telefono);
        usuario.setPortfolioUrl(portfolioUrl);
        usuario.setLinkedinUrl(linkedinUrl);
        usuario.setPassword(passwordEncoder.encode(password));
        usuario.setActivo(true);
        usuario.setPerfilPublico(true);
        usuario.setCreadoEn(new Date());
        usuario.setActualizadoEn(new Date());

        usuarioRepository.save(usuario);
        poblarSesion(session,
                usuario.getId(),
                usuario.getCorreo(),
                usuario.getNombre() != null ? usuario.getNombre() : "Usuario",
                RoleGuard.TIPO_TALENTO,
                usuario.getFotoUrl());
        session.setAttribute("mostrarOnboarding", true);

        redirectAttributes.addFlashAttribute("flashMessage", "Cuenta creada. Bienvenido a DevCoast.");
        redirectAttributes.addFlashAttribute("flashType", "success");
        return "redirect:/dashboard";
    }

    private boolean correoValido(String correo) {
        return correo != null && correo.matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");
    }

    private boolean passwordValida(String password) {
        return password != null && password.trim().length() >= 8;
    }

    private void reflashTalento(RedirectAttributes ra, String nombre, String rolTitulo, String bio, String stack,
            String especialidad, String modalidad, String salarioEsperado, String disponibilidad, String correo,
            String emailContacto, String telefono, String portfolioUrl, String linkedinUrl) {
        ra.addFlashAttribute("fNombre", nombre);
        ra.addFlashAttribute("fRolTitulo", rolTitulo);
        ra.addFlashAttribute("fBio", bio);
        ra.addFlashAttribute("fStack", stack);
        ra.addFlashAttribute("fEspecialidad", especialidad);
        ra.addFlashAttribute("fModalidad", modalidad);
        ra.addFlashAttribute("fSalarioEsperado", salarioEsperado);
        ra.addFlashAttribute("fDisponibilidad", disponibilidad);
        ra.addFlashAttribute("fCorreo", correo);
        ra.addFlashAttribute("fEmailContacto", emailContacto);
        ra.addFlashAttribute("fTelefono", telefono);
        ra.addFlashAttribute("fPortfolioUrl", portfolioUrl);
        ra.addFlashAttribute("fLinkedinUrl", linkedinUrl);
    }

    @PostMapping("/register/empresa")
    public String registrarEmpresa(
            @RequestParam String nombreEmpresa,
            @RequestParam String nombreRepresentante,
            @RequestParam String cargoRepresentante,
            @RequestParam(required = false) String sector,
            @RequestParam(required = false) String tamano,
            @RequestParam(required = false) String sitioWeb,
            @RequestParam String correo,
            @RequestParam(required = false) String telefono,
            @RequestParam(required = false) String descripcion,
            @RequestParam String password,
            HttpServletRequest request,
            HttpSession session,
            RedirectAttributes redirectAttributes) {

        String correoNorm = correo.trim().toLowerCase();

        if (limiteTasa.excedido("registro|" + request.getRemoteAddr(), MAX_REGISTROS, VENTANA_REGISTROS_MS)) {
            reflashEmpresa(redirectAttributes, nombreEmpresa, nombreRepresentante, cargoRepresentante, sector,
                    tamano, sitioWeb, correo, telefono, descripcion);
            return "redirect:/register/empresa?error=limite";
        }
        if (!correoValido(correoNorm)
                || usuarioRepository.findByCorreo(correoNorm) != null
                || empresaRepository.findByCorreo(correoNorm) != null) {
            reflashEmpresa(redirectAttributes, nombreEmpresa, nombreRepresentante, cargoRepresentante, sector,
                    tamano, sitioWeb, correo, telefono, descripcion);
            return "redirect:/register/empresa?error=correo";
        }
        if (!passwordValida(password)) {
            reflashEmpresa(redirectAttributes, nombreEmpresa, nombreRepresentante, cargoRepresentante, sector,
                    tamano, sitioWeb, correo, telefono, descripcion);
            return "redirect:/register/empresa?error=password";
        }

        Empresa empresa = new Empresa();
        empresa.setTipo("empresa");
        empresa.setNombreEmpresa(nombreEmpresa.trim());
        empresa.setNombreRepresentante(nombreRepresentante.trim());
        empresa.setCargoRepresentante(cargoRepresentante.trim());
        empresa.setSector(sector);
        empresa.setTamano(tamano);
        empresa.setSitioWeb(sitioWeb);
        empresa.setCorreo(correoNorm);
        empresa.setTelefono(telefono);
        empresa.setDescripcion(descripcion != null && descripcion.length() > 300
                ? descripcion.substring(0, 300) : descripcion);
        empresa.setPassword(passwordEncoder.encode(password));
        empresa.setActivo(true);
        empresa.setCreadoEn(new Date());
        empresa.setActualizadoEn(new Date());

        empresaRepository.save(empresa);
        poblarSesion(session,
                empresa.getId(),
                empresa.getCorreo(),
                empresa.getNombreEmpresa(),
                RoleGuard.TIPO_EMPRESA,
                empresa.getFotoUrl());

        redirectAttributes.addFlashAttribute("flashMessage", "Cuenta creada. Bienvenido a DevCoast.");
        redirectAttributes.addFlashAttribute("flashType", "success");
        return "redirect:/dashboard";
    }

    private void reflashEmpresa(RedirectAttributes ra, String nombreEmpresa, String nombreRepresentante,
            String cargoRepresentante, String sector, String tamano, String sitioWeb, String correo,
            String telefono, String descripcion) {
        ra.addFlashAttribute("fNombreEmpresa", nombreEmpresa);
        ra.addFlashAttribute("fNombreRepresentante", nombreRepresentante);
        ra.addFlashAttribute("fCargoRepresentante", cargoRepresentante);
        ra.addFlashAttribute("fSector", sector);
        ra.addFlashAttribute("fTamano", tamano);
        ra.addFlashAttribute("fSitioWeb", sitioWeb);
        ra.addFlashAttribute("fCorreo", correo);
        ra.addFlashAttribute("fTelefono", telefono);
        ra.addFlashAttribute("fDescripcion", descripcion);
    }

    private List<String> parseStack(String stack) {
        if (stack == null || stack.isBlank()) {
            return List.of();
        }

        return Arrays.stream(stack.split(","))
                .map(String::trim)
                .filter(item -> !item.isBlank())
                .collect(Collectors.toList());
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