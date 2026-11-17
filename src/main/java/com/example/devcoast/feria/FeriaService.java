package com.example.devcoast.feria;

import java.nio.file.Path;
import java.security.SecureRandom;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import org.bson.Document;
import org.bson.types.ObjectId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import com.example.devcoast.feria.CsvFeria.Fila;
import com.example.devcoast.model.Empleo;
import com.example.devcoast.model.Empresa;
import com.example.devcoast.model.Proyecto;
import com.example.devcoast.model.Solicitud;
import com.example.devcoast.model.Usuario;
import com.example.devcoast.service.CleanupService;
import com.example.devcoast.service.EtiquetasPerfil;
import com.example.devcoast.service.PlanService;

// la parte de datos del modo feria: importa lo curado (feria/datos) sin duplicar y deja las cuentas demo con su historia lista
@Service
public class FeriaService {

    private static final Logger log = LoggerFactory.getLogger(FeriaService.class);

    public static final String DEMO_TALENTO = "demo@devcoast.co";
    public static final String DEMO_DISENO = "diseno@devcoast.co";
    public static final String DEMO_EMPRESA = "empresa@devcoast.co";
    public static final String DEMO_STARTUP = "startup@devcoast.co";
    public static final List<String> DEMO_CORREOS = List.of(DEMO_TALENTO, DEMO_DISENO, DEMO_EMPRESA, DEMO_STARTUP);
    static final String CLAVE_DEMO = "demo1234";

    private static final long DIA = 86_400_000L;
    private static final String META = "feria_meta";
    private static final String[] COLORES = { "#FF7B4F", "#2DD4BF", "#CDD1DB", "#CC4B27", "#1F8F82", "#8B5CF6", "#F59E0B", "#3B82F6",
            "#EC4899", "#10B981" };

    private final MongoTemplate mongo;
    private final BCryptPasswordEncoder encoder;
    private final CleanupService limpieza;
    private final Path carpeta;
    private final AtomicInteger contador = new AtomicInteger(new SecureRandom().nextInt(0xFFFFFF));

    public FeriaService(MongoTemplate mongo, BCryptPasswordEncoder encoder, CleanupService limpieza,
            @Value("${devcoast.feria.datos:feria/datos}") String carpeta) {
        this.mongo = mongo;
        this.encoder = encoder;
        this.limpieza = limpieza;
        this.carpeta = Path.of(carpeta);
    }

    public Path carpeta() {
        return carpeta;
    }

    public DatosFeria leerDatos() {
        return DatosFeria.cargar(carpeta);
    }

    public boolean hayHistoriaSembrada() {
        return mongo.findById("historias", Document.class, META) != null;
    }

    // ---------------------------------------------------------------- importación

    public record Resultado(int empresas, int ofertas, int talentos, int proyectos) {
        @Override
        public String toString() {
            return empresas + " empresas, " + ofertas + " ofertas, " + talentos + " talentos, " + proyectos + " proyectos";
        }
    }

    // vuelve a aplicar lo curado: crea lo que falta y actualiza lo que ya existe (por correo / empresa+título / talento+nombre).
    // Lee y escribe por lotes (unas 12 consultas en total) para que no dependa de la distancia a la base de datos.
    public synchronized Resultado importar(DatosFeria datos) {
        if (!datos.valido())
            throw new IllegalStateException("Los datos de la feria tienen errores: " + String.join(" | ", datos.errores));
        Date ahora = new Date();

        List<String> correosEmpresa = new ArrayList<>();
        datos.empresas.forEach(f -> correosEmpresa.add(f.get("correo").toLowerCase(Locale.ROOT)));
        List<String> correosTalento = new ArrayList<>();
        datos.talentos.forEach(f -> correosTalento.add(f.get("correo").toLowerCase(Locale.ROOT)));
        List<String> todosLosCorreos = new ArrayList<>(correosEmpresa);
        todosLosCorreos.addAll(correosTalento);

        Map<String, Empresa> empresasExistentes = new HashMap<>();
        Map<String, Usuario> usuariosExistentes = new HashMap<>();
        if (!todosLosCorreos.isEmpty()) {
            Query porCorreo = Query.query(Criteria.where("correo").in(todosLosCorreos));
            mongo.find(porCorreo, Empresa.class).forEach(e -> empresasExistentes.put(e.getCorreo(), e));
            mongo.find(Query.query(Criteria.where("correo").in(todosLosCorreos)), Usuario.class).forEach(u -> usuariosExistentes.put(u.getCorreo(), u));
        }

        Map<String, Empresa> empresas = new HashMap<>();
        int n = 0;
        for (Fila f : datos.empresas) {
            String correo = f.get("correo").toLowerCase(Locale.ROOT);
            Empresa e = importarEmpresa(f, ahora, n++, empresasExistentes.get(correo), usuariosExistentes.containsKey(correo));
            empresas.put(correo, e);
        }
        guardarPorLotes(new ArrayList<>(empresas.values()), Empresa.class, Empresa::getId);

        Map<String, Empleo> ofertasExistentes = new HashMap<>();
        if (!empresas.isEmpty())
            mongo.find(Query.query(Criteria.where("empresaId").in(empresas.values().stream().map(Empresa::getId).toList())), Empleo.class)
                    .forEach(o -> ofertasExistentes.put(o.getEmpresaId() + "|" + o.getTitulo(), o));
        List<Empleo> ofertas = new ArrayList<>();
        Map<String, Integer> porEmpresa = new HashMap<>();
        for (Fila f : datos.ofertas) {
            Empresa em = empresas.get(f.get("empresa_correo").toLowerCase(Locale.ROOT));
            ofertas.add(importarOferta(f, em, ahora, porEmpresa.merge(em.getCorreo(), 1, Integer::sum), ofertasExistentes.get(em.getId() + "|" + f.get("titulo"))));
        }
        guardarPorLotes(ofertas, Empleo.class, Empleo::getId);

        Map<String, Usuario> talentos = new HashMap<>();
        n = 0;
        for (Fila f : datos.talentos) {
            String correo = f.get("correo").toLowerCase(Locale.ROOT);
            talentos.put(correo, importarTalento(f, ahora, n++, usuariosExistentes.get(correo), empresasExistentes.containsKey(correo)));
        }
        guardarPorLotes(new ArrayList<>(talentos.values()), Usuario.class, Usuario::getId);

        Map<String, Proyecto> proyectosExistentes = new HashMap<>();
        if (!talentos.isEmpty())
            mongo.find(Query.query(Criteria.where("usuarioId").in(talentos.values().stream().map(Usuario::getId).toList())), Proyecto.class)
                    .forEach(p -> proyectosExistentes.put(p.getUsuarioId() + "|" + p.getNombre(), p));
        List<Proyecto> proyectos = new ArrayList<>();
        Map<String, Integer> proyPorTalento = new HashMap<>();
        for (Fila f : datos.proyectos) {
            Usuario u = talentos.get(f.get("talento_correo").toLowerCase(Locale.ROOT));
            if (u == null)
                continue;
            proyectos.add(importarProyecto(f, u, ahora, proyPorTalento.merge(u.getCorreo(), 1, Integer::sum), proyectosExistentes.get(u.getId() + "|" + f.get("nombre"))));
        }
        guardarPorLotes(proyectos, Proyecto.class, Proyecto::getId);

        if (mongo.findById("base", Document.class, META) == null)
            mongo.save(new Document("_id", "base").append("fecha", ahora), META);
        Resultado r = new Resultado(empresas.size(), ofertas.size(), talentos.size(), proyectos.size());
        log.info("[Feria] Datos curados aplicados: {}", r);
        return r;
    }

    // una sola ida a la base por tipo: reemplaza (o crea) cada documento por su _id
    private <T> void guardarPorLotes(List<T> docs, Class<T> tipo, java.util.function.Function<T, String> id) {
        if (docs.isEmpty())
            return;
        org.springframework.data.mongodb.core.BulkOperations ops = mongo.bulkOps(org.springframework.data.mongodb.core.BulkOperations.BulkMode.UNORDERED, tipo);
        for (T d : docs)
            ops.replaceOne(Query.query(Criteria.where("_id").is(id.apply(d))), d, org.springframework.data.mongodb.core.FindAndReplaceOptions.options().upsert());
        ops.execute();
    }

    private Empresa importarEmpresa(Fila f, Date ahora, int orden, Empresa e, boolean existeComoTalento) {
        String correo = f.get("correo").toLowerCase(Locale.ROOT);
        if (existeComoTalento)
            throw new IllegalStateException("El correo " + correo + " ya existe como talento; no puede ser también una empresa.");
        boolean nueva = e == null;
        if (nueva) {
            e = new Empresa();
            e.setTipo("empresa");
            e.setCorreo(correo);
            e.setCreadoEn(atras(ahora, 120 + orden * 9));
            e.setId(nuevoId(e.getCreadoEn()));
            e.setPassword(claveInicial(f, correo));
        } else if (!f.vacio("password")) {
            e.setPassword(encoder.encode(f.get("password")));
        }
        e.setNombreEmpresa(f.get("nombre"));
        e.setSector(catalogo(f.get("sector")));
        e.setTamano(vacioANull(f.get("tamano")));
        e.setUbicacion(vacioANull(f.get("ubicacion")));
        e.setSitioWeb(vacioANull(f.get("sitio_web")));
        e.setDescripcion(vacioANull(f.get("descripcion")));
        e.setNombreRepresentante(vacioANull(f.get("representante")));
        e.setCargoRepresentante(vacioANull(f.get("cargo")));
        e.setTelefono(vacioANull(f.get("telefono")));
        e.setActivo(true);
        e.setActualizadoEn(ahora);
        if (!f.vacio("logo"))
            e.setFotoUrl(urlImagen(f.get("logo")));
        boolean pro = "PRO".equalsIgnoreCase(f.get("plan"));
        e.setPlan(pro ? PlanService.PLAN_PRO : PlanService.PLAN_GRATIS);
        e.setPlanVenceEn(pro ? new Date(ahora.getTime() + 45 * DIA) : null);
        return e;
    }

    private Empleo importarOferta(Fila f, Empresa em, Date ahora, int orden, Empleo e) {
        String titulo = f.get("titulo");
        if (e == null) {
            e = new Empleo();
            Integer dias = f.entero("dias_publicado");
            e.setFechaPublicacion(atras(ahora, dias != null ? dias : 2 + orden * 5));
            e.setId(nuevoId(e.getFechaPublicacion()));
        }
        e.setEmpresaId(em.getId());
        e.setEmpresaNombre(em.getNombreEmpresa());
        e.setEmpresaSector(em.getSector());
        e.setEmpresaWeb(em.getSitioWeb());
        e.setEmpresaTamano(em.getTamano());
        e.setEmpresaDescripcion(em.getDescripcion());
        e.setEmpresaCorreo(em.getCorreo());
        e.setLogoUrl(em.getFotoUrl());
        e.setTitulo(titulo);
        String categoria = catalogo(f.get("categoria"));
        e.setCategoria(categoria);
        e.setIcono(iconoDe(categoria));
        if (e.getColorIcono() == null)
            e.setColorIcono(COLORES[Math.floorMod(titulo.hashCode(), COLORES.length)]);
        e.setSeniority(seniority(f.get("seniority")));
        e.setModalidad(switch (EtiquetasPerfil.modalidadClave(f.get("modalidad"))) {
            case "remoto" -> "Remoto";
            case "presencial" -> "Presencial";
            default -> "Hibrido";
        });
        e.setContrato(f.vacio("contrato") ? "Tiempo completo" : catalogo(f.get("contrato")));
        Integer vacantes = f.entero("vacantes");
        e.setVacantes(vacantes == null || vacantes < 1 ? 1 : vacantes);
        e.setMoneda(f.vacio("moneda") ? "COP" : f.get("moneda").toUpperCase(Locale.ROOT));
        e.setSalarioMin(f.entero("salario_min"));
        e.setSalarioMax(f.entero("salario_max"));
        e.setSalarioPeriodo(f.vacio("periodo") ? "/ mes" : f.get("periodo"));
        e.setMostrarSalario(f.si("mostrar_salario") && e.getSalarioMin() != null);
        e.setTimezone(vacioANull(f.get("timezone")));
        e.setIngles(vacioANull(catalogo(f.get("ingles"))));
        e.setDescripcionRol(f.get("descripcion"));
        e.setRequisitosDeseables(vacioANull(f.get("requisitos")));
        e.setStack(String.join(", ", f.lista("stack")));
        e.setSoftSkills(f.vacio("soft_skills") ? null : String.join(", ", f.lista("soft_skills")));
        e.setDestacar(f.si("destacar") && PlanService.PLAN_PRO.equals(em.getPlan()));
        e.setMatchAutomatico(false);
        e.setNotificacionesEmail(false);
        boolean cerrado = "CERRADO".equalsIgnoreCase(f.get("estado"));
        e.setEstado(cerrado ? "CERRADO" : "ACTIVO");
        String fase = f.get("fase").toUpperCase(Locale.ROOT);
        e.setFaseReclutamiento(cerrado ? "CERRADO_PROCESO"
                : Set.of("ABIERTO", "SCREENING", "ENTREVISTAS", "OFERTA").contains(fase) ? fase : "ABIERTO");
        return e;
    }

    private Usuario importarTalento(Fila f, Date ahora, int orden, Usuario u, boolean existeComoEmpresa) {
        String correo = f.get("correo").toLowerCase(Locale.ROOT);
        if (existeComoEmpresa)
            throw new IllegalStateException("El correo " + correo + " ya existe como empresa; no puede ser también un talento.");
        if (u == null) {
            u = new Usuario();
            u.setTipo("talento");
            u.setCorreo(correo);
            u.setCreadoEn(atras(ahora, 30 + orden * 7));
            u.setId(nuevoId(u.getCreadoEn()));
            u.setPassword(claveInicial(f, correo));
            u.setEmpleosFavoritos(new ArrayList<>());
        } else if (!f.vacio("password")) {
            u.setPassword(encoder.encode(f.get("password")));
        }
        u.setNombre(f.get("nombre"));
        u.setRolTitulo(f.get("rol"));
        u.setBio(vacioANull(f.get("bio")));
        u.setStack(new ArrayList<>(f.lista("stack")));
        u.setEspecialidad(EtiquetasPerfil.especialidadClave(f.get("especialidad")));
        String modalidad = EtiquetasPerfil.modalidadClave(f.get("modalidad"));
        u.setModalidad(modalidad.isEmpty() ? "remoto" : modalidad);
        String disp = EtiquetasPerfil.disponibilidadClave(f.get("disponibilidad"));
        u.setDisponibilidad(disp.isEmpty() ? "full-time" : disp);
        u.setSalarioEsperado(vacioANull(f.get("salario")));
        u.setUbicacion(vacioANull(f.get("ubicacion")));
        u.setGithubUrl(vacioANull(f.get("github")));
        u.setLinkedinUrl(vacioANull(f.get("linkedin")));
        u.setPortfolioUrl(vacioANull(f.get("portfolio")));
        u.setTelefono(null);
        boolean mostrar = f.si("mostrar_correo");
        u.setEmailContacto(mostrar ? correo : null);
        u.setMostrarCorreo(mostrar);
        u.setActivo(true);
        u.setPerfilPublico(true);
        u.setPermitirMensajes(true);
        u.setOnboardingCompleto(true);
        u.setDestacar(f.si("destacar"));
        u.setNivelEspanol(5);
        Integer ingles = f.entero("nivel_ingles");
        u.setNivelIngles(ingles == null ? 3 : Math.min(5, ingles));
        u.setHabilidadesBlandas(f.vacio("habilidades_blandas") ? new ArrayList<>() : new ArrayList<>(f.lista("habilidades_blandas")));
        u.setCvBullets(f.vacio("cv_bullets") ? new ArrayList<>() : new ArrayList<>(f.lista("cv_bullets")));
        u.setCvExperiencias(experiencias(f.get("experiencias")));
        u.setCvEducaciones(educaciones(f.get("educacion")));
        u.setActualizadoEn(ahora);
        if (!f.vacio("foto"))
            u.setFotoUrl(urlImagen(f.get("foto")));
        return u;
    }

    private Proyecto importarProyecto(Fila f, Usuario u, Date ahora, int orden, Proyecto p) {
        String nombre = f.get("nombre");
        if (p == null) {
            p = new Proyecto();
            p.setCreadoEn(atras(ahora, 3 + orden * 6));
            p.setId(nuevoId(p.getCreadoEn()));
        }
        p.setUsuarioId(u.getId());
        p.setUsuarioCorreo(u.getCorreo());
        p.setNombre(nombre);
        String categoria = catalogo(f.get("categoria"));
        p.setCategoria(categoria);
        p.setTipo(f.vacio("tipo") ? "Personal" : f.get("tipo"));
        p.setDescripcionCorta(recortar(f.get("descripcion_corta"), 200));
        p.setDescripcionExtendida(f.vacio("descripcion") ? f.get("descripcion_corta") : f.get("descripcion"));
        p.setAnio(f.vacio("anio") ? String.valueOf(java.time.LocalDate.now().getYear()) : f.get("anio"));
        p.setLinkProyecto(vacioANull(f.get("link")));
        boolean diseno = categoria.toLowerCase(Locale.ROOT).matches("^(ui|ux|diseno|design|dashboard|branding).*");
        p.setBannerClase(diseno ? "cover-artboard" : "cover-terminal");
        p.setIconoClave(diseno ? "layers" : "code");
        p.setColorIcono(COLORES[Math.floorMod(nombre.hashCode(), COLORES.length)]);
        p.setStack(new ArrayList<>(f.lista("stack")));
        p.setEtiquetas(new ArrayList<>(f.lista("etiquetas")));
        p.setRol(vacioANull(f.get("rol")));
        p.setDuracion(vacioANull(f.get("duracion")));
        p.setPublicado(true);
        p.setIncluirCv(true);
        p.setDestacar(f.si("destacar"));
        p.setActualizadoEn(ahora);
        if (!f.vacio("imagen"))
            p.setImagenUrl(urlImagen(f.get("imagen")));
        return p;
    }

    // ---------------------------------------------------------------- reinicio de la demo

    // deja las cuentas demo como el primer día: perfil y ofertas según los archivos, historial rehecho con fechas de hoy.
    // purgarRegistros = true además borra las cuentas creadas por visitantes después de la primera importación
    public synchronized String reiniciar(DatosFeria datos, boolean purgarRegistros) {
        long t0 = System.currentTimeMillis();
        importar(datos);
        long tImportar = System.currentTimeMillis() - t0;
        int purgadas = 0;
        if (purgarRegistros)
            purgadas = purgarRegistros(datos);
        long t1 = System.currentTimeMillis();
        podarExtrasDemo(datos);
        long tPodar = System.currentTimeMillis() - t1;
        long t2 = System.currentTimeMillis();
        String h = new HistoriasDemo(mongo, datos).reconstruir();
        long tHistorias = System.currentTimeMillis() - t2;
        mongo.save(new Document("_id", "historias").append("fecha", new Date()), META);
        String msg = "Demo reiniciada en " + (System.currentTimeMillis() - t0) + " ms. " + h
                + (purgarRegistros ? " Cuentas de visitantes borradas: " + purgadas + "." : "");
        log.info("[Feria] {} (importar {} ms, limpiar {} ms, historias {} ms)", msg, tImportar, tPodar, tHistorias);
        return msg;
    }

    // ofertas y proyectos que un visitante creó en una cuenta demo se van; los de los archivos se quedan
    private void podarExtrasDemo(DatosFeria datos) {
        Set<String> tituloOfertas = new HashSet<>();
        for (Fila f : datos.ofertas)
            tituloOfertas.add(f.get("empresa_correo").toLowerCase(Locale.ROOT) + "|" + f.get("titulo"));
        for (String correo : List.of(DEMO_EMPRESA, DEMO_STARTUP)) {
            Empresa em = mongo.findOne(Query.query(Criteria.where("correo").is(correo)), Empresa.class);
            if (em == null)
                continue;
            for (Empleo e : mongo.find(Query.query(Criteria.where("empresaId").is(em.getId())), Empleo.class))
                if (!tituloOfertas.contains(correo + "|" + e.getTitulo())) {
                    limpieza.limpiarDatosDeEmpleo(e.getId());
                    mongo.remove(e);
                }
        }
        Set<String> proyectos = new HashSet<>();
        for (Fila f : datos.proyectos)
            proyectos.add(f.get("talento_correo").toLowerCase(Locale.ROOT) + "|" + f.get("nombre"));
        for (String correo : List.of(DEMO_TALENTO, DEMO_DISENO)) {
            Usuario u = mongo.findOne(Query.query(Criteria.where("correo").is(correo)), Usuario.class);
            if (u == null)
                continue;
            for (Proyecto p : mongo.find(Query.query(Criteria.where("usuarioId").is(u.getId())), Proyecto.class))
                if (!proyectos.contains(correo + "|" + p.getNombre()))
                    mongo.remove(p);
            u.setEmpleosFavoritos(new ArrayList<>());
            mongo.save(u);
        }
    }

    private int purgarRegistros(DatosFeria datos) {
        Document base = mongo.findById("base", Document.class, META);
        if (base == null)
            return 0;
        Date desde = base.getDate("fecha");
        Set<String> curados = new HashSet<>();
        datos.empresas.forEach(f -> curados.add(f.get("correo").toLowerCase(Locale.ROOT)));
        datos.talentos.forEach(f -> curados.add(f.get("correo").toLowerCase(Locale.ROOT)));
        int n = 0;
        for (Usuario u : mongo.find(Query.query(Criteria.where("creadoEn").gt(desde)), Usuario.class))
            if (!curados.contains(u.getCorreo())) {
                limpieza.limpiarDatosDeTalento(u.getId());
                mongo.remove(u);
                n++;
            }
        for (Empresa e : mongo.find(Query.query(Criteria.where("creadoEn").gt(desde)), Empresa.class))
            if (!curados.contains(e.getCorreo())) {
                limpieza.limpiarDatosDeEmpresa(e.getId());
                mongo.remove(e);
                n++;
            }
        return n;
    }

    // ---------------------------------------------------------------- utilidades

    String nuevoId(Date fecha) {
        return new ObjectId(fecha, contador.updateAndGet(c -> (c + 1) & 0xFFFFFF)).toHexString();
    }

    private String claveInicial(Fila f, String correo) {
        if (DEMO_CORREOS.contains(correo))
            return encoder.encode(CLAVE_DEMO);
        if (!f.vacio("password"))
            return encoder.encode(f.get("password"));
        byte[] azar = new byte[24];
        new SecureRandom().nextBytes(azar);
        return encoder.encode(java.util.Base64.getEncoder().encodeToString(azar));
    }

    private static Date atras(Date desde, double dias) {
        return new Date(desde.getTime() - (long) (dias * DIA));
    }

    private static String urlImagen(String ruta) {
        return "/feria-img/" + ruta.replace('\\', '/');
    }

    private static String vacioANull(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }

    private static String recortar(String s, int max) {
        return s.length() > max ? s.substring(0, max) : s;
    }

    // los formularios de la app guardan algunas palabras sin tilde; los datos curados se escriben bien y aquí se igualan
    static String catalogo(String s) {
        if (s == null)
            return "";
        return s.trim().replace("Programación", "Programacion").replace("Educación", "Educacion").replace("Prácticas", "Practicas")
                .replace("Práctica", "Practica").replace("Gráfico", "Grafico").replace("Ilustración", "Ilustracion")
                .replace("restricción", "restriccion").replace("Básico", "Basico");
    }

    static String seniority(String v) {
        String b = Normalizer.normalize(v == null ? "" : v, Normalizer.Form.NFD).replaceAll("\\p{M}+", "").toLowerCase(Locale.ROOT);
        if (b.startsWith("junior") || b.startsWith("jr"))
            return "Junior (0 – 2 años)";
        if (b.startsWith("mid") || b.startsWith("semi"))
            return "Mid (2 – 4 años)";
        if (b.startsWith("senior") || b.startsWith("sr"))
            return "Senior (4+ años)";
        if (b.startsWith("lead") || b.startsWith("principal"))
            return "Lead / Principal";
        return "Sin restriccion";
    }

    static String iconoDe(String categoria) {
        String c = categoria.toLowerCase(Locale.ROOT);
        if (c.contains("frontend"))
            return "globe";
        if (c.contains("backend") || c.contains("devops"))
            return "cpu";
        if (c.contains("full"))
            return "code";
        if (c.contains("mobile"))
            return "terminal";
        if (c.contains("data"))
            return "chart";
        if (c.startsWith("dise"))
            return "layers";
        return "trending-up";
    }

    private static List<Usuario.CvExperiencia> experiencias(String texto) {
        List<Usuario.CvExperiencia> r = new ArrayList<>();
        for (String item : texto.split(";;")) {
            String[] p = item.split("\\|", -1);
            if (item.isBlank() || p.length < 3)
                continue;
            Usuario.CvExperiencia e = new Usuario.CvExperiencia();
            e.setCargo(p[0].trim());
            e.setEmpresa(p[1].trim());
            e.setPeriodo(p[2].trim());
            e.setModalidad("Remoto");
            e.setDescripcion(p.length > 3 ? p[3].trim() : null);
            r.add(e);
        }
        return r;
    }

    private static List<Usuario.CvEducacion> educaciones(String texto) {
        List<Usuario.CvEducacion> r = new ArrayList<>();
        for (String item : texto.split(";;")) {
            String[] p = item.split("\\|", -1);
            if (item.isBlank() || p.length < 3)
                continue;
            Usuario.CvEducacion e = new Usuario.CvEducacion();
            e.setInstitucion(p[0].trim());
            e.setTitulo(p[1].trim());
            e.setPeriodo(p[2].trim());
            r.add(e);
        }
        return r;
    }

    // solicitudes existentes de los demos, para el simulador y los chequeos
    public long contarSolicitudes() {
        return mongo.count(new Query(), Solicitud.class);
    }
}
