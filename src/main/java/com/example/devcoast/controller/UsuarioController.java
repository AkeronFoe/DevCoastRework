package com.example.devcoast.controller;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.example.devcoast.model.Empresa;
import com.example.devcoast.model.Usuario;
import com.example.devcoast.model.Usuario.CvEducacion;
import com.example.devcoast.model.Usuario.CvExperiencia;
import com.example.devcoast.repository.EmpleoRepository;
import com.example.devcoast.repository.EmpresaRepository;
import com.example.devcoast.repository.UsuarioRepository;

import jakarta.servlet.http.HttpSession;

@RestController
public class UsuarioController {

    private final UsuarioRepository repository;
    private final EmpresaRepository empresaRepository;
    private final EmpleoRepository empleoRepository;
    private final BCryptPasswordEncoder passwordEncoder;

    public UsuarioController(UsuarioRepository repository, EmpresaRepository empresaRepository,
            EmpleoRepository empleoRepository, BCryptPasswordEncoder passwordEncoder) {
        this.repository = repository;
        this.empresaRepository = empresaRepository;
        this.empleoRepository = empleoRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @PostMapping("/perfil/guardar")
    public ResponseEntity<Map<String, Object>> guardarPerfil(
            @RequestBody Map<String, Object> datos,
            HttpSession session) {

        String id = (String) session.getAttribute("usuarioActualId");
        if (id == null)
            return ResponseEntity.status(401).body(Map.of("ok", false, "error", "No autenticado"));

        String tipo = (String) session.getAttribute("usuarioActualTipo");
        if ("empresa".equals(tipo)) {
            Empresa empresa = empresaRepository.findById(id).orElse(null);
            if (empresa == null)
                return ResponseEntity.status(404).body(Map.of("ok", false, "error", "Empresa no encontrada"));
            if (datos.containsKey("nombre") && !str(datos, "nombre").isEmpty()) {
                empresa.setNombreEmpresa(str(datos, "nombre"));
                session.setAttribute("usuarioActualNombre", empresa.getNombreEmpresa());
            }
            if (datos.containsKey("bio"))
                empresa.setDescripcion(str(datos, "bio"));
            if (datos.containsKey("ubicacion"))
                empresa.setUbicacion(str(datos, "ubicacion"));
            if (datos.containsKey("portfolioUrl"))
                empresa.setSitioWeb(str(datos, "portfolioUrl"));
            if (datos.containsKey("rolTitulo"))
                empresa.setSector(str(datos, "rolTitulo"));
            empresaRepository.save(empresa);
            return ResponseEntity.ok(Map.of("ok", true, "mensaje", "Perfil guardado"));
        }

        Usuario usuario = repository.findById(id).orElse(null);
        if (usuario == null)
            return ResponseEntity.status(404).body(Map.of("ok", false, "error", "Usuario no encontrado"));

        if (datos.containsKey("nombre") && !str(datos, "nombre").isEmpty()) {
            usuario.setNombre(str(datos, "nombre"));
            session.setAttribute("usuarioActualNombre", usuario.getNombre());
        }
        if (datos.containsKey("rolTitulo"))
            usuario.setRolTitulo(str(datos, "rolTitulo"));
        if (datos.containsKey("bio"))
            usuario.setBio(str(datos, "bio"));
        if (datos.containsKey("emailContacto"))
            usuario.setEmailContacto(str(datos, "emailContacto"));
        if (datos.containsKey("telefono"))
            usuario.setTelefono(str(datos, "telefono"));
        if (datos.containsKey("portfolioUrl"))
            usuario.setPortfolioUrl(str(datos, "portfolioUrl"));
        if (datos.containsKey("linkedinUrl"))
            usuario.setLinkedinUrl(str(datos, "linkedinUrl"));
        if (datos.containsKey("githubUrl"))
            usuario.setGithubUrl(str(datos, "githubUrl"));
        if (datos.containsKey("modalidad"))
            usuario.setModalidad(str(datos, "modalidad"));
        if (datos.containsKey("disponibilidad"))
            usuario.setDisponibilidad(str(datos, "disponibilidad"));
        if (datos.containsKey("salarioEsperado"))
            usuario.setSalarioEsperado(str(datos, "salarioEsperado"));
        if (datos.containsKey("especialidad"))
            usuario.setEspecialidad(str(datos, "especialidad"));
        if (datos.containsKey("ubicacion"))
            usuario.setUbicacion(str(datos, "ubicacion"));

        try {
            if (datos.containsKey("stack"))
                usuario.setStack(limitarLista(toStringList(datos.get("stack")), 50));
            if (datos.containsKey("habilidadesBlandas"))
                usuario.setHabilidadesBlandas(limitarLista(toStringList(datos.get("habilidadesBlandas")), 30));
        } catch (ClassCastException e) {
            return ResponseEntity.badRequest().body(Map.of("ok", false, "error", "Formato de datos inválido"));
        }

        usuario.setActualizadoEn(new Date());
        repository.save(usuario);
        return ResponseEntity.ok(Map.of("ok", true, "mensaje", "Perfil guardado"));
    }

    @PostMapping("/curriculum/guardar")
    public ResponseEntity<Map<String, Object>> guardarCurriculum(
            @RequestBody Map<String, Object> datos,
            HttpSession session) {

        String id = (String) session.getAttribute("usuarioActualId");
        if (id == null)
            return ResponseEntity.status(401).body(Map.of("ok", false, "error", "No autenticado"));
        if ("empresa".equals(session.getAttribute("usuarioActualTipo")))
            return ResponseEntity.status(400).body(Map.of("ok", false, "error", "El currículum es solo para cuentas de talento"));

        Usuario usuario = repository.findById(id).orElse(null);
        if (usuario == null)
            return ResponseEntity.status(404).body(Map.of("ok", false, "error", "Usuario no encontrado"));

        try {
            if (datos.containsKey("nombre") && !str(datos, "nombre").isEmpty()) {
                usuario.setNombre(str(datos, "nombre"));
                session.setAttribute("usuarioActualNombre", usuario.getNombre());
            }
            if (datos.containsKey("rolTitulo"))
                usuario.setRolTitulo(str(datos, "rolTitulo"));
            if (datos.containsKey("bio"))
                usuario.setBio(str(datos, "bio"));
            if (datos.containsKey("emailContacto"))
                usuario.setEmailContacto(str(datos, "emailContacto"));
            if (datos.containsKey("telefono"))
                usuario.setTelefono(str(datos, "telefono"));
            if (datos.containsKey("portfolioUrl"))
                usuario.setPortfolioUrl(str(datos, "portfolioUrl"));
            if (datos.containsKey("linkedinUrl"))
                usuario.setLinkedinUrl(str(datos, "linkedinUrl"));
            if (datos.containsKey("githubUrl"))
                usuario.setGithubUrl(str(datos, "githubUrl"));
            if (datos.containsKey("ubicacion"))
                usuario.setUbicacion(str(datos, "ubicacion"));
            if (datos.containsKey("modalidad"))
                usuario.setModalidad(str(datos, "modalidad"));
            if (datos.containsKey("disponibilidad"))
                usuario.setDisponibilidad(str(datos, "disponibilidad"));
            if (datos.containsKey("salarioEsperado"))
                usuario.setSalarioEsperado(str(datos, "salarioEsperado"));
            if (datos.containsKey("especialidad"))
                usuario.setEspecialidad(str(datos, "especialidad"));

            if (datos.containsKey("nivelEspanol"))
                usuario.setNivelEspanol(toInt(datos.get("nivelEspanol")));
            if (datos.containsKey("nivelIngles"))
                usuario.setNivelIngles(toInt(datos.get("nivelIngles")));

            if (datos.containsKey("cvBullets"))
                usuario.setCvBullets(limitarLista(toStringList(datos.get("cvBullets")), 30));
            if (datos.containsKey("habilidadesBlandas"))
                usuario.setHabilidadesBlandas(limitarLista(toStringList(datos.get("habilidadesBlandas")), 30));
            if (datos.containsKey("cvExperiencias")) {
                List<Map<String, String>> exps = limitarLista(toMapList(datos.get("cvExperiencias")), 20);
                List<CvExperiencia> lista = exps.stream().map(e -> {
                    CvExperiencia exp = new CvExperiencia();
                    exp.setCargo(e.getOrDefault("cargo", ""));
                    exp.setEmpresa(e.getOrDefault("empresa", ""));
                    exp.setPeriodo(e.getOrDefault("periodo", ""));
                    exp.setModalidad(e.getOrDefault("modalidad", ""));
                    exp.setDescripcion(e.getOrDefault("descripcion", ""));
                    return exp;
                }).collect(Collectors.toList());
                usuario.setCvExperiencias(lista);
            }
            if (datos.containsKey("cvEducaciones")) {
                List<Map<String, String>> edus = limitarLista(toMapList(datos.get("cvEducaciones")), 20);
                List<CvEducacion> listaEdu = edus.stream().map(e -> {
                    CvEducacion edu = new CvEducacion();
                    edu.setInstitucion(e.getOrDefault("institucion", ""));
                    edu.setTitulo(e.getOrDefault("titulo", ""));
                    edu.setPeriodo(e.getOrDefault("periodo", ""));
                    edu.setDescripcion(e.getOrDefault("descripcion", ""));
                    return edu;
                }).collect(Collectors.toList());
                usuario.setCvEducaciones(listaEdu);
            }
        } catch (ClassCastException e) {
            return ResponseEntity.badRequest().body(Map.of("ok", false, "error", "Formato de datos inválido"));
        }

        usuario.setActualizadoEn(new Date());
        repository.save(usuario);
        return ResponseEntity.ok(Map.of("ok", true, "mensaje", "CV guardado correctamente"));
    }

    @SuppressWarnings("unchecked")
    private List<String> toStringList(Object v) {
        return (List<String>) v;
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, String>> toMapList(Object v) {
        return (List<Map<String, String>>) v;
    }

    private <T> List<T> limitarLista(List<T> lista, int max) {
        if (lista == null)
            return new ArrayList<>();
        return lista.size() > max ? new ArrayList<>(lista.subList(0, max)) : lista;
    }

    @PostMapping("/ajustes/perfil-publico")
    public ResponseEntity<Map<String, Object>> togglePerfilPublico(
            @RequestBody Map<String, Object> datos,
            HttpSession session) {

        String id = (String) session.getAttribute("usuarioActualId");
        if (id == null)
            return ResponseEntity.status(401).body(Map.of("ok", false, "error", "No autenticado"));

        Usuario usuario = repository.findById(id).orElse(null);
        if (usuario == null)
            return ResponseEntity.status(404).body(Map.of("ok", false, "error", "Usuario no encontrado"));

        if (datos.containsKey("perfilPublico")) {
            Object val = datos.get("perfilPublico");
            boolean activo = Boolean.TRUE.equals(
                    val instanceof Boolean b ? b : (val != null ? Boolean.valueOf(val.toString()) : Boolean.FALSE));
            usuario.setPerfilPublico(activo);
        }

        usuario.setActualizadoEn(new Date());
        repository.save(usuario);
        return ResponseEntity.ok(Map.of("ok", true, "perfilPublico", usuario.isPerfilPublico()));
    }

    @PostMapping("/empleos/favorito/{empleoId}")
    public ResponseEntity<Map<String, Object>> toggleFavorito(
            @PathVariable String empleoId,
            HttpSession session) {

        String id = (String) session.getAttribute("usuarioActualId");
        if (id == null)
            return ResponseEntity.status(401).body(Map.of("ok", false, "error", "No autenticado"));
        if (!"talento".equals(session.getAttribute("usuarioActualTipo")))
            return ResponseEntity.status(400).body(Map.of("ok", false, "error", "Solo el talento puede guardar empleos"));
        if (!empleoRepository.existsById(empleoId))
            return ResponseEntity.status(404).body(Map.of("ok", false, "error", "Empleo no encontrado"));

        Usuario usuario = repository.findById(id).orElse(null);
        if (usuario == null)
            return ResponseEntity.status(404).body(Map.of("ok", false, "error", "Usuario no encontrado"));

        List<String> favs = usuario.getEmpleosFavoritos();
        if (favs == null)
            favs = new ArrayList<>();

        boolean esFavorito;
        if (favs.contains(empleoId)) {
            favs.remove(empleoId);
            esFavorito = false;
        } else {
            favs.add(empleoId);
            esFavorito = true;
        }
        usuario.setEmpleosFavoritos(favs);
        usuario.setActualizadoEn(new Date());
        repository.save(usuario);

        return ResponseEntity.ok(Map.of("ok", true, "favorito", esFavorito));
    }

    @PostMapping("/ajustes/privacidad")
    public ResponseEntity<Map<String, Object>> guardarPrivacidad(
            @RequestBody Map<String, Object> datos,
            HttpSession session) {

        String id = (String) session.getAttribute("usuarioActualId");
        if (id == null)
            return ResponseEntity.status(401).body(Map.of("ok", false, "error", "No autenticado"));
        if ("empresa".equals(session.getAttribute("usuarioActualTipo")))
            return ResponseEntity.status(400).body(Map.of("ok", false, "error", "Estas opciones son solo para cuentas de talento"));

        Usuario usuario = repository.findById(id).orElse(null);
        if (usuario == null)
            return ResponseEntity.status(404).body(Map.of("ok", false, "error", "Usuario no encontrado"));

        if (datos.containsKey("perfilPublico")) {
            Object val = datos.get("perfilPublico");
            usuario.setPerfilPublico(
                    val instanceof Boolean ? (Boolean) val : Boolean.parseBoolean(String.valueOf(val)));
        }
        if (datos.containsKey("mostrarCorreo")) {
            Object val = datos.get("mostrarCorreo");
            usuario.setMostrarCorreo(
                    val instanceof Boolean ? (Boolean) val : Boolean.parseBoolean(String.valueOf(val)));
        }
        if (datos.containsKey("permitirMensajes")) {
            Object val = datos.get("permitirMensajes");
            usuario.setPermitirMensajes(Boolean.TRUE.equals(val) || "true".equalsIgnoreCase(String.valueOf(val)));
        }

        usuario.setActualizadoEn(new Date());
        repository.save(usuario);
        return ResponseEntity.ok(Map.of("ok", true));
    }

    @PostMapping("/ajustes/cambiarCorreo")
    public ResponseEntity<Map<String, Object>> cambiarCorreo(
            @RequestBody Map<String, Object> datos,
            HttpSession session) {

        String id = (String) session.getAttribute("usuarioActualId");
        if (id == null)
            return ResponseEntity.status(401).body(Map.of("ok", false, "mensaje", "No autenticado"));

        String nuevoCorreo = str(datos, "nuevoCorreo").toLowerCase();
        String password = datos.get("password") == null ? "" : datos.get("password").toString();

        if (nuevoCorreo.isEmpty() || !nuevoCorreo.contains("@"))
            return ResponseEntity.badRequest().body(Map.of("ok", false, "mensaje", "Ingresa un correo válido"));
        if (password.isEmpty())
            return ResponseEntity.badRequest().body(Map.of("ok", false, "mensaje", "Ingresa tu contraseña actual"));

        String tipo = (String) session.getAttribute("usuarioActualTipo");
        if ("empresa".equals(tipo)) {
            Empresa empresa = empresaRepository.findById(id).orElse(null);
            if (empresa == null)
                return ResponseEntity.status(404).body(Map.of("ok", false, "mensaje", "Cuenta no encontrada"));
            if (!passwordEncoder.matches(password, empresa.getPassword()))
                return ResponseEntity.badRequest().body(Map.of("ok", false, "mensaje", "La contraseña actual es incorrecta"));
            if (!nuevoCorreo.equalsIgnoreCase(empresa.getCorreo())
                    && (empresaRepository.findByCorreo(nuevoCorreo) != null || repository.findByCorreo(nuevoCorreo) != null))
                return ResponseEntity.badRequest().body(Map.of("ok", false, "mensaje", "Ese correo ya está en uso"));
            empresa.setCorreo(nuevoCorreo);
            empresaRepository.save(empresa);
        } else {
            Usuario usuario = repository.findById(id).orElse(null);
            if (usuario == null)
                return ResponseEntity.status(404).body(Map.of("ok", false, "mensaje", "Cuenta no encontrada"));
            if (!passwordEncoder.matches(password, usuario.getPassword()))
                return ResponseEntity.badRequest().body(Map.of("ok", false, "mensaje", "La contraseña actual es incorrecta"));
            if (!nuevoCorreo.equalsIgnoreCase(usuario.getCorreo())
                    && (repository.findByCorreo(nuevoCorreo) != null || empresaRepository.findByCorreo(nuevoCorreo) != null))
                return ResponseEntity.badRequest().body(Map.of("ok", false, "mensaje", "Ese correo ya está en uso"));
            usuario.setCorreo(nuevoCorreo);
            usuario.setActualizadoEn(new Date());
            repository.save(usuario);
        }

        session.setAttribute("usuarioActualCorreo", nuevoCorreo);
        return ResponseEntity.ok(Map.of("ok", true, "correo", nuevoCorreo));
    }

    private String str(Map<String, Object> m, String key) {
        Object v = m.get(key);
        return v == null ? "" : v.toString().trim();
    }

    private int toInt(Object v) {
        if (v == null)
            return 0;
        if (v instanceof Number n)
            return n.intValue();
        try {
            return Integer.parseInt(v.toString());
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    @PostMapping("/onboarding/completar")
    public ResponseEntity<Map<String, Object>> completarOnboarding(
            @RequestBody Map<String, Object> datos,
            HttpSession session) {

        String id = (String) session.getAttribute("usuarioActualId");
        if (id == null)
            return ResponseEntity.status(401).body(Map.of("ok", false));
        String tipo = (String) session.getAttribute("usuarioActualTipo");
        if (!"talento".equals(tipo))
            return ResponseEntity.badRequest().body(Map.of("ok", false));

        com.example.devcoast.model.Usuario usuario = repository.findById(id).orElse(null);
        if (usuario == null)
            return ResponseEntity.status(404).body(Map.of("ok", false));

        if (datos.containsKey("bio") && !str(datos, "bio").isEmpty())
            usuario.setBio(str(datos, "bio"));
        if (datos.containsKey("ubicacion") && !str(datos, "ubicacion").isEmpty())
            usuario.setUbicacion(str(datos, "ubicacion"));
        if (datos.containsKey("rolTitulo") && !str(datos, "rolTitulo").isEmpty())
            usuario.setRolTitulo(str(datos, "rolTitulo"));
        if (datos.containsKey("modalidad") && !str(datos, "modalidad").isEmpty())
            usuario.setModalidad(str(datos, "modalidad"));
        if (datos.containsKey("salarioEsperado") && !str(datos, "salarioEsperado").isEmpty())
            usuario.setSalarioEsperado(str(datos, "salarioEsperado"));
        if (datos.containsKey("disponibilidad") && !str(datos, "disponibilidad").isEmpty())
            usuario.setDisponibilidad(str(datos, "disponibilidad"));

        try {
            List<String> stack = toStringList(datos.get("stack"));
            if (stack != null && !stack.isEmpty())
                usuario.setStack(limitarLista(stack, 50));
        } catch (ClassCastException e) {
            return ResponseEntity.badRequest().body(Map.of("ok", false));
        }

        usuario.setOnboardingCompleto(true);
        usuario.setActualizadoEn(new java.util.Date());
        repository.save(usuario);

        return ResponseEntity.ok(Map.of("ok", true));
    }

}