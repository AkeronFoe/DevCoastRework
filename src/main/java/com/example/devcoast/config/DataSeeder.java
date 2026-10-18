package com.example.devcoast.config;

import java.text.Normalizer;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.function.Function;

import org.bson.Document;
import org.bson.types.ObjectId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

import com.example.devcoast.config.SeedCatalogo.Arq;
import com.example.devcoast.model.Cobro;
import com.example.devcoast.model.Empleo;
import com.example.devcoast.model.Empresa;
import com.example.devcoast.model.EventoEstado;
import com.example.devcoast.model.Invitacion;
import com.example.devcoast.model.Mensaje;
import com.example.devcoast.model.Notificacion;
import com.example.devcoast.model.Proyecto;
import com.example.devcoast.model.Solicitud;
import com.example.devcoast.model.Usuario;
import com.example.devcoast.service.MatchEmpleo;
import com.example.devcoast.service.PlanService;

/**
 * Siembra datos de ejemplo (miles de talentos, empresas, ofertas, proyectos, solicitudes, chats, notificaciones,
 * invitaciones y cobros) con relaciones coherentes y los mismos valores que guardan los formularios de la app.
 *
 * <ul>
 * <li>Está apagado por defecto: se activa con {@code devcoast.seed.enabled=true} (variable {@code SEED_ENABLED}).</li>
 * <li>Corre en un hilo aparte, por lotes: la app arranca y responde mientras se siembra.</li>
 * <li>Es idempotente: al terminar deja una marca en la colección {@code seed_meta}; si ya existe no vuelve a sembrar.</li>
 * <li>Nunca modifica ni borra datos existentes (salvo que se pida con {@code devcoast.seed.limpiar-antiguos=true},
 * que quita solo lo que sembró el seeder anterior; ver {@link LimpiezaSeederAntiguo}). Si una siembra se interrumpe, no reintenta sola: se reintenta con
 * {@code devcoast.seed.reintentar=true}, que borra solo lo creado después de que empezó esa siembra.</li>
 * <li>No envía correos ni toca servicios externos: las cuentas y direcciones son ficticias.</li>
 * </ul>
 *
 * Cuentas de demo (contraseña {@code demo1234}): demo@devcoast.co, diseno@devcoast.co (talentos),
 * empresa@devcoast.co (plan PRO) y startup@devcoast.co (plan gratis). El resto usa la contraseña {@code devcoast123}.
 */
@Component
@ConditionalOnProperty(name = "devcoast.seed.enabled", havingValue = "true")
public class DataSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);

    static final int VERSION = 2;
    private static final String MARCA_ID = "devcoast-seed";
    private static final String COLECCION_MARCA = "seed_meta";
    private static final long SEMILLA = 20260101L;
    private static final long DIA = 86_400_000L;
    private static final int LOTE = 1000;
    private static final String[] COLECCIONES = { "usuarios", "empresas", "empleos", "proyectos", "solicitudes", "invitaciones",
            "mensajes", "notificaciones", "cobros" };

    private static final String DEMO_TALENTO = "demo@devcoast.co";
    private static final String DEMO_DISENO = "diseno@devcoast.co";
    private static final String DEMO_EMPRESA = "empresa@devcoast.co";
    private static final String DEMO_STARTUP = "startup@devcoast.co";

    private final MongoTemplate mongo;
    private final BCryptPasswordEncoder encoder;
    private final int nTalentos;
    private final int nEmpresas;
    private final boolean asincrono;
    private final boolean reintentar;
    private final boolean limpiarAntiguos;

    public DataSeeder(MongoTemplate mongo, BCryptPasswordEncoder encoder,
            @Value("${devcoast.seed.talentos:8000}") int nTalentos,
            @Value("${devcoast.seed.empresas:2000}") int nEmpresas,
            @Value("${devcoast.seed.async:true}") boolean asincrono,
            @Value("${devcoast.seed.reintentar:false}") boolean reintentar,
            @Value("${devcoast.seed.limpiar-antiguos:false}") boolean limpiarAntiguos) {
        this.mongo = mongo;
        this.encoder = encoder;
        this.nTalentos = Math.max(0, nTalentos);
        this.nEmpresas = Math.max(0, nEmpresas);
        this.asincrono = asincrono;
        this.reintentar = reintentar;
        this.limpiarAntiguos = limpiarAntiguos;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!asincrono) {
            sembrar();
            return;
        }
        Thread hilo = new Thread(this::sembrar, "devcoast-seeder");
        hilo.setDaemon(true);
        hilo.start();
    }

    /** Siembra de forma síncrona. Lo usan {@link #run} y las pruebas. */
    public void sembrar() {
        try {
            if (limpiarAntiguos)
                new LimpiezaSeederAntiguo(mongo).limpiar();
            Document marca = mongo.findById(MARCA_ID, Document.class, COLECCION_MARCA);
            if (marca != null && "completo".equals(marca.getString("estado")) && marca.getInteger("version", 0) >= VERSION) {
                log.info("[DataSeeder] Los datos de ejemplo ya están sembrados (versión {}). No se hace nada.", marca.getInteger("version", 0));
                return;
            }
            if (marca != null && "en_progreso".equals(marca.getString("estado"))) {
                if (!reintentar) {
                    log.warn("[DataSeeder] Una siembra anterior no terminó. No se toca nada. Para reintentar (borra solo lo creado "
                            + "desde que esa siembra empezó) arranca con devcoast.seed.reintentar=true.");
                    return;
                }
                deshacerParcial();
            }
            long t0 = System.currentTimeMillis();
            Document nueva = new Document("_id", MARCA_ID).append("estado", "en_progreso").append("version", VERSION)
                    .append("inicio", new Date());
            mongo.save(nueva, COLECCION_MARCA);

            Generacion g = new Generacion();
            g.ejecutar();

            nueva.put("estado", "completo");
            nueva.put("fin", new Date());
            nueva.put("resumen", g.resumen());
            mongo.save(nueva, COLECCION_MARCA);
            log.info("[DataSeeder] Siembra completa en {} s. {}", (System.currentTimeMillis() - t0) / 1000, g.resumen().toJson());
            log.info("[DataSeeder] Cuentas de demo (contraseña demo1234): {}, {}, {}, {}", DEMO_TALENTO, DEMO_DISENO, DEMO_EMPRESA,
                    DEMO_STARTUP);
        } catch (Exception e) {
            log.error("[DataSeeder] La siembra falló y quedó incompleta: {}", e.toString(), e);
        }
    }

    // ───────────────────────── marca y reintento ─────────────────────────

    private static String coleccionDeIds(String coleccion) {
        return "ids-" + coleccion;
    }

    /** Borra exactamente los documentos que la siembra interrumpida había registrado (nada más). */
    private void deshacerParcial() {
        log.warn("[DataSeeder] Reintento: borrando lo que creó la siembra interrumpida...");
        for (String c : COLECCIONES) {
            Document reg = mongo.findById(coleccionDeIds(c), Document.class, COLECCION_MARCA);
            if (reg == null)
                continue;
            List<ObjectId> ids = reg.getList("ids", ObjectId.class);
            long borrados = 0;
            for (int i = 0; i < ids.size(); i += 5000)
                borrados += mongo.remove(new Query(Criteria.where("_id").in(ids.subList(i, Math.min(ids.size(), i + 5000)))), c)
                        .getDeletedCount();
            mongo.remove(new Query(Criteria.where("_id").is(coleccionDeIds(c))), COLECCION_MARCA);
            log.warn("[DataSeeder]   {}: {} documentos borrados", c, borrados);
        }
        mongo.remove(new Query(Criteria.where("_id").is(MARCA_ID)), COLECCION_MARCA);
    }

    // ───────────────────────── generación ─────────────────────────

    private record EmpleoGen(Empleo e, String grupo) {
    }

    private record TalentoGen(Usuario u, String grupo, int tier, Arq arq) {
    }

    private final class Generacion {
        final Random rnd = new Random(SEMILLA);
        final Date ahora = new Date();
        final int anioActual = LocalDate.now().getYear();
        final String hash = encoder.encode("devcoast123");
        final String hashDemo = encoder.encode("demo1234");

        final Set<String> correos = new HashSet<>();
        final Map<String, Integer> usadosBase = new HashMap<>();
        final Set<String> nombresEmpresa = new HashSet<>();

        final List<Empresa> empresas = new ArrayList<>();
        final List<EmpleoGen> empleosGen = new ArrayList<>();
        final List<TalentoGen> talentosGen = new ArrayList<>();
        final List<Proyecto> proyectos = new ArrayList<>();
        final List<Solicitud> solicitudes = new ArrayList<>();
        final List<Invitacion> invitaciones = new ArrayList<>();
        final List<Mensaje> mensajes = new ArrayList<>();
        final List<Notificacion> notificaciones = new ArrayList<>();
        final List<Cobro> cobros = new ArrayList<>();

        final Map<String, Empresa> empresaPorId = new HashMap<>();
        final Map<String, Empleo> empleoPorId = new HashMap<>();
        final Map<String, Usuario> usuarioPorId = new HashMap<>();
        final Map<String, TalentoGen> talentoGenPorId = new HashMap<>();
        final Map<String, List<Empleo>> empleosPorGrupo = new LinkedHashMap<>();
        final Map<String, List<Empleo>> activosPorGrupo = new LinkedHashMap<>();
        final Map<String, List<Empleo>> activosPorEmpresa = new HashMap<>();
        final Map<String, String> grupoDeEmpleo = new HashMap<>();
        final Map<String, List<Proyecto>> proyectosPorUsuario = new HashMap<>();
        final Map<String, List<TalentoGen>> talentosPorGrupo = new LinkedHashMap<>();
        final Map<String, Integer> aprobadasPorEmpleo = new HashMap<>();
        final Set<String> paresSolicitud = new HashSet<>();

        boolean demoTalento;
        boolean demoDiseno;
        boolean demoEmpresa;
        boolean demoStartup;
        Usuario uDemoTalento;
        Usuario uDemoDiseno;
        Empresa eDemoEmpresa;
        Empresa eDemoStartup;

        void ejecutar() {
            long t = System.currentTimeMillis();
            for (String correo : mongo.findDistinct(new Query(), "correo", Usuario.class, String.class))
                if (correo != null)
                    correos.add(correo.toLowerCase(Locale.ROOT));
            for (String correo : mongo.findDistinct(new Query(), "correo", Empresa.class, String.class))
                if (correo != null)
                    correos.add(correo.toLowerCase(Locale.ROOT));
            demoTalento = !correos.contains(DEMO_TALENTO);
            demoDiseno = !correos.contains(DEMO_DISENO);
            demoEmpresa = !correos.contains(DEMO_EMPRESA);
            demoStartup = !correos.contains(DEMO_STARTUP);
            log.info("[DataSeeder] Iniciando: {} talentos y {} empresas (más cuentas de demo).", nTalentos, nEmpresas);

            generarEmpresas();
            insertar(empresas, Empresa.class, "empresas", Empresa::getId);
            empresas.forEach(e -> empresaPorId.put(e.getId(), e));

            generarEmpleos();
            insertar(empleosGen.stream().map(EmpleoGen::e).toList(), Empleo.class, "empleos", Empleo::getId);
            indexarEmpleos();

            generarTalentos();
            insertar(talentosGen.stream().map(TalentoGen::u).toList(), Usuario.class, "usuarios", Usuario::getId);
            for (TalentoGen tg : talentosGen) {
                usuarioPorId.put(tg.u().getId(), tg.u());
                talentoGenPorId.put(tg.u().getId(), tg);
                talentosPorGrupo.computeIfAbsent(tg.grupo(), k -> new ArrayList<>()).add(tg);
            }

            generarProyectos();
            insertar(proyectos, Proyecto.class, "proyectos", Proyecto::getId);
            for (Proyecto p : proyectos)
                proyectosPorUsuario.computeIfAbsent(p.getUsuarioId(), k -> new ArrayList<>()).add(p);

            generarSolicitudes();
            insertar(solicitudes, Solicitud.class, "solicitudes", Solicitud::getId);

            generarInvitaciones();
            insertar(invitaciones, Invitacion.class, "invitaciones", Invitacion::getId);

            generarMensajes();
            insertar(mensajes, Mensaje.class, "mensajes", Mensaje::getId);

            generarCobros();
            insertar(cobros, Cobro.class, "cobros", Cobro::getId);

            generarNotificaciones();
            insertar(notificaciones, Notificacion.class, "notificaciones", Notificacion::getId);
            log.info("[DataSeeder] Generación e inserción: {} s", (System.currentTimeMillis() - t) / 1000);
        }

        Document resumen() {
            return new Document("empresas", empresas.size()).append("ofertas", empleosGen.size()).append("talentos", talentosGen.size())
                    .append("proyectos", proyectos.size()).append("solicitudes", solicitudes.size())
                    .append("invitaciones", invitaciones.size()).append("mensajes", mensajes.size())
                    .append("notificaciones", notificaciones.size()).append("cobros", cobros.size());
        }

        /** Registra los ids antes de insertar (para poder deshacer si se interrumpe) y luego inserta por lotes. */
        <T> void insertar(List<T> lista, Class<T> tipo, String coleccion, Function<T, String> id) {
            List<ObjectId> ids = new ArrayList<>(lista.size());
            for (T t : lista)
                ids.add(new ObjectId(id.apply(t)));
            mongo.save(new Document("_id", coleccionDeIds(coleccion)).append("ids", ids), COLECCION_MARCA);
            for (int i = 0; i < lista.size(); i += LOTE)
                mongo.insert(lista.subList(i, Math.min(lista.size(), i + LOTE)), tipo);
            log.info("[DataSeeder]   {}: {} insertados", coleccion, lista.size());
        }

        /** Un ObjectId cuya fecha embebida es la del documento: así "reciente", "nuevo esta semana" y el orden por _id son reales. */
        String idEn(Date fecha) {
            return new ObjectId(fecha).toHexString();
        }

        // ───────── utilidades ─────────

        int peso(int... pesos) {
            int total = 0;
            for (int p : pesos)
                total += p;
            int r = rnd.nextInt(total);
            for (int i = 0; i < pesos.length; i++) {
                r -= pesos[i];
                if (r < 0)
                    return i;
            }
            return pesos.length - 1;
        }

        <T> T pick(T[] arr) {
            return arr[rnd.nextInt(arr.length)];
        }

        <T> T pick(List<T> l) {
            return l.get(rnd.nextInt(l.size()));
        }

        boolean prob(double p) {
            return rnd.nextDouble() < p;
        }

        Date atras(double dias) {
            return new Date(ahora.getTime() - (long) (dias * DIA));
        }

        Date mas(Date base, double dias) {
            return new Date(base.getTime() + (long) (dias * DIA));
        }

        Date limitar(Date f) {
            return f.after(ahora) ? new Date(ahora.getTime() - 3_600_000L) : f;
        }

        List<Date> fechasOrdenadas(int n, double minDias, double maxDias) {
            List<Date> l = new ArrayList<>(n);
            for (int i = 0; i < n; i++)
                l.add(atras(minDias + rnd.nextDouble() * (maxDias - minDias)));
            l.sort(Comparator.naturalOrder());
            return l;
        }

        String sinAcentos(String s) {
            return Normalizer.normalize(s, Normalizer.Form.NFD).replaceAll("\\p{M}+", "").replace('ñ', 'n').replace('Ñ', 'N');
        }

        String slug(String s) {
            return sinAcentos(s).toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]", "");
        }

        String miles(long n) {
            return java.text.NumberFormat.getIntegerInstance(new Locale("es", "CO")).format(n);
        }

        long redondear(long v, long paso) {
            return Math.round(v / (double) paso) * paso;
        }

        String telefono() {
            return "+57 3" + rnd.nextInt(10) + rnd.nextInt(10) + " 555 " + String.format("%04d", rnd.nextInt(10000));
        }

        String anios(int n) {
            return n == 1 ? "1 año" : n + " años";
        }

        Arq elegirArq(String grupo) {
            List<Arq> candidatos = new ArrayList<>();
            for (Arq a : SeedCatalogo.ARQUETIPOS)
                if (a.grupo().equals(grupo))
                    candidatos.add(a);
            for (Arq a : SeedCatalogo.SOLO_OFERTAS)
                if (a.grupo().equals(grupo))
                    candidatos.add(a);
            return pick(candidatos);
        }

        // ───────── empresas ─────────

        void generarEmpresas() {
            List<Date> fechas = fechasOrdenadas(nEmpresas + 2, 20, 900);
            int f = 0;
            if (demoEmpresa) {
                eDemoEmpresa = empresa("DevCoast Labs", "SaaS / Software", "51 – 200 personas", DEMO_EMPRESA, true, "Mariana Torres Cardona",
                        "CTO", "Medellín, Colombia", atras(900), hashDemo,
                        "DevCoast Labs desarrolla plataformas de talento tech con operación en Colombia y México: producto, datos y comunidad en un solo equipo.");
                eDemoEmpresa.setPlanVenceEn(mas(ahora, 21));
            }
            if (demoStartup) {
                eDemoStartup = empresa("Marea Studio", "Startup", "11 – 50 personas", DEMO_STARTUP, false, "Andrés Felipe Rueda Ibarra",
                        "Cofundador", "Cartagena, Colombia", atras(430), hashDemo,
                        "Marea Studio es una startup de turismo digital en el Caribe: reservas, experiencias locales y una app que ya tiene usuarios activos.");
            }
            for (int i = 0; i < nEmpresas; i++) {
                String sector = SeedCatalogo.SECTORES[peso(SeedCatalogo.PESO_SECTORES)];
                String nombre = nombreEmpresa();
                String correoBase = pick(SeedCatalogo.CORREOS_EMPRESA) + "@" + slug(nombre);
                String correo = correoBase + pick(SeedCatalogo.DOMINIOS_EMPRESA);
                for (int n = 2; correos.contains(correo); n++)
                    correo = correoBase + n + SeedCatalogo.DOMINIOS_EMPRESA[0];
                String desc = String.format(pick(SeedCatalogo.DESCRIPCION_EMPRESA.get(sector)), nombre);
                String rep = nombrePersona(prob(0.5)).completo();
                Empresa e = empresa(nombre, sector, SeedCatalogo.TAMANOS[peso(SeedCatalogo.PESO_TAMANOS)], correo, prob(0.18), rep,
                        pick(SeedCatalogo.CARGOS_REP), pick(SeedCatalogo.CIUDADES), fechas.get(f++), hash, desc);
                if (PlanService.PLAN_PRO.equals(e.getPlan()))
                    e.setPlanVenceEn(mas(ahora, 3 + rnd.nextInt(26)));
            }
            empresas.sort(Comparator.comparing(Empresa::getCreadoEn));
        }

        Empresa empresa(String nombre, String sector, String tamano, String correo, boolean pro, String representante, String cargo,
                String ubicacion, Date creado, String password, String descripcion) {
            Empresa e = new Empresa();
            e.setTipo("empresa");
            e.setNombreEmpresa(nombre);
            e.setNombreRepresentante(representante);
            e.setCargoRepresentante(cargo);
            e.setSector(sector);
            e.setTamano(tamano);
            e.setSitioWeb("https://www." + slug(nombre) + ".example.com");
            e.setCorreo(correo);
            e.setTelefono(telefono());
            e.setDescripcion(descripcion);
            e.setUbicacion(ubicacion);
            e.setPassword(password);
            e.setActivo(true);
            e.setCreadoEn(creado);
            e.setActualizadoEn(creado);
            e.setPlan(pro ? PlanService.PLAN_PRO : PlanService.PLAN_GRATIS);
            e.setId(idEn(creado));
            correos.add(correo.toLowerCase(Locale.ROOT));
            empresas.add(e);
            return e;
        }

        String nombreEmpresa() {
            for (int intento = 0; intento < 20; intento++) {
                String n = pick(SeedCatalogo.EMPRESA_A) + " " + pick(SeedCatalogo.EMPRESA_B);
                if (nombresEmpresa.add(n))
                    return n;
            }
            String n;
            do {
                n = pick(SeedCatalogo.EMPRESA_A) + " " + pick(SeedCatalogo.EMPRESA_B) + " " + pick(SeedCatalogo.EMPRESA_C);
            } while (!nombresEmpresa.add(n));
            return n;
        }

        // ───────── ofertas ─────────

        void generarEmpleos() {
            for (Empresa em : empresas) {
                boolean pro = PlanService.PLAN_PRO.equals(em.getPlan());
                boolean esDemoPro = DEMO_EMPRESA.equals(em.getCorreo());
                int activos = esDemoPro ? 9 : pro ? 3 + rnd.nextInt(9) : peso(20, 45, 35);
                int cerrados = esDemoPro ? 3 : pro ? rnd.nextInt(5) : rnd.nextInt(4);
                if (DEMO_STARTUP.equals(em.getCorreo()))
                    activos = 2;
                int destacados = 0;
                for (int k = 0; k < activos + cerrados; k++) {
                    boolean activo = k < activos;
                    boolean destacar = activo && pro && destacados < 3 && prob(esDemoPro ? 0.5 : 0.35);
                    if (destacar)
                        destacados++;
                    empleosGen.add(crearEmpleo(em, activo, destacar));
                }
            }
            empleosGen.sort(Comparator.comparing(g -> g.e().getFechaPublicacion()));
        }

        EmpleoGen crearEmpleo(Empresa em, boolean activo, boolean destacar) {
            String sector = em.getSector();
            String grupo = pick(SeedCatalogo.GRUPOS_POR_SECTOR.get(sector));
            Arq arq = elegirArq(grupo);
            int tier = peso(26, 34, 28, 8, 4);
            String rol = pick(arq.roles());
            String titulo = switch (tier) {
                case 0 -> "Junior " + rol;
                case 2 -> "Senior " + rol;
                case 3 -> "Lead " + rol;
                default -> rol;
            };
            int len = arq.stack().length;
            int k = 3 + rnd.nextInt(Math.min(4, len - 2));
            List<String> skills = new ArrayList<>(Arrays.asList(arq.stack()).subList(0, k));
            if (k < len && prob(0.35))
                skills.set(k - 1, arq.stack()[k + rnd.nextInt(len - k)]);
            String stack = String.join(", ", skills);

            String modalidad = SeedCatalogo.MODALIDADES_JOB[peso(50, 32, 18)];
            String contrato = tier == 0 && prob(0.08) ? "Practicas"
                    : SeedCatalogo.CONTRATOS[peso(78, 4, 10, 8, 0)];
            boolean usd = "Remoto".equals(modalidad) && prob(0.4);
            boolean blando = !grupo.equals("frontend") && !grupo.equals("backend") && !grupo.equals("fullstack") && !grupo.equals("mobile")
                    && !grupo.equals("data") && !grupo.equals("devops");
            double factor = blando ? 0.85 : 1.0;

            Empleo e = new Empleo();
            e.setEmpresaId(em.getId());
            e.setEmpresaNombre(em.getNombreEmpresa());
            e.setEmpresaSector(em.getSector());
            e.setEmpresaWeb(em.getSitioWeb());
            e.setEmpresaTamano(em.getTamano());
            e.setEmpresaDescripcion(em.getDescripcion());
            e.setEmpresaCorreo(em.getCorreo());
            e.setIcono(arq.icono());
            e.setColorIcono(pick(SeedCatalogo.COLORES));
            e.setTitulo(titulo);
            e.setCategoria(arq.categoria());
            e.setSeniority(SeedCatalogo.SENIORITY[tier]);
            e.setModalidad(modalidad);
            e.setContrato(contrato);
            e.setVacantes(peso(70, 20, 7, 3) + 1);
            if ("Freelance / Proyecto".equals(contrato)) {
                e.setMoneda("USD");
                long lo = redondear(15 + rnd.nextInt(45), 5);
                e.setSalarioMin((int) lo);
                e.setSalarioMax((int) (lo + 10 + rnd.nextInt(25)));
                e.setSalarioPeriodo("/ hora");
            } else if (usd) {
                long[][] b = { { 1100, 2000 }, { 2000, 3600 }, { 3600, 6500 }, { 6000, 10000 }, { 1500, 4000 } };
                long lo = redondear(b[tier][0] + (long) (rnd.nextDouble() * (b[tier][1] - b[tier][0]) * 0.7), 100);
                e.setMoneda("USD");
                e.setSalarioMin((int) lo);
                e.setSalarioMax((int) redondear((long) (lo * (1.2 + rnd.nextDouble() * 0.3)), 100));
                e.setSalarioPeriodo("/ mes");
            } else {
                long[][] b = { { 2_400_000, 4_500_000 }, { 4_500_000, 8_000_000 }, { 8_000_000, 14_000_000 },
                        { 13_000_000, 22_000_000 }, { 3_000_000, 8_000_000 } };
                long base = (long) (b[tier][0] * factor + rnd.nextDouble() * (b[tier][1] - b[tier][0]) * 0.7 * factor);
                long lo = redondear(base, 100_000);
                e.setMoneda("COP");
                e.setSalarioMin((int) lo);
                e.setSalarioMax((int) redondear((long) (lo * (1.2 + rnd.nextDouble() * 0.3)), 100_000));
                e.setSalarioPeriodo("/ mes");
            }
            e.setMostrarSalario(prob(0.78));
            e.setTimezone(prob(0.55) ? "Latam · UTC-5" : prob(0.4) ? pick(SeedCatalogo.TIMEZONES) : null);
            e.setIngles(tier == 0 && prob(0.5) ? null : SeedCatalogo.INGLES[peso(25, 50, 25)]);
            String producto = pick(SeedCatalogo.PRODUCTO_POR_SECTOR.get(sector));
            e.setDescripcionRol("En " + em.getNombreEmpresa() + " buscamos sumar a nuestro equipo el perfil de " + titulo
                    + ". Participarás en la construcción de " + producto + ", trabajando con " + stack
                    + " en ciclos cortos, con revisiones de código y contacto directo con producto. Ofrecemos "
                    + pick(SeedCatalogo.BENEFICIOS) + " y " + pick(SeedCatalogo.BENEFICIOS) + ".");
            e.setRequisitosDeseables(pick(SeedCatalogo.REQUISITOS_DESEABLES));
            e.setStack(stack);
            e.setSoftSkills(String.join(", ", pickVarios(SeedCatalogo.SOFT_SKILLS, 3 + rnd.nextInt(2))));
            e.setMatchAutomatico(prob(0.3));
            e.setDestacar(destacar);
            e.setNotificacionesEmail(true);
            boolean screening = prob(0.15);
            e.setScreeningActivo(screening);
            if (screening)
                e.setScreeningKeywords(skills.get(0) + "," + skills.get(1));

            Date pub;
            if (activo) {
                double dias = switch (peso(30, 40, 30)) {
                    case 0 -> rnd.nextDouble() * 7;
                    case 1 -> 7 + rnd.nextDouble() * 23;
                    default -> 30 + rnd.nextDouble() * 90;
                };
                pub = atras(dias);
                e.setEstado("ACTIVO");
                e.setFaseReclutamiento(new String[] { "ABIERTO", "SCREENING", "ENTREVISTAS", "OFERTA" }[peso(55, 20, 17, 8)]);
            } else {
                pub = atras(40 + rnd.nextDouble() * 220);
                e.setEstado("CERRADO");
                e.setFaseReclutamiento("CERRADO_PROCESO");
            }
            Date minimo = mas(em.getCreadoEn(), 1);
            if (pub.before(minimo))
                pub = minimo;
            e.setFechaPublicacion(limitar(pub));
            e.setId(idEn(e.getFechaPublicacion()));
            return new EmpleoGen(e, grupo);
        }

        List<String> pickVarios(String[] fuente, int n) {
            List<String> copia = new ArrayList<>(Arrays.asList(fuente));
            Collections.shuffle(copia, rnd);
            return new ArrayList<>(copia.subList(0, Math.min(n, copia.size())));
        }

        void indexarEmpleos() {
            for (EmpleoGen g : empleosGen) {
                Empleo e = g.e();
                empleoPorId.put(e.getId(), e);
                grupoDeEmpleo.put(e.getId(), g.grupo());
                empleosPorGrupo.computeIfAbsent(g.grupo(), k -> new ArrayList<>()).add(e);
                if ("ACTIVO".equals(e.getEstado())) {
                    activosPorGrupo.computeIfAbsent(g.grupo(), k -> new ArrayList<>()).add(e);
                    activosPorEmpresa.computeIfAbsent(e.getEmpresaId(), k -> new ArrayList<>()).add(e);
                }
            }
        }

        // ───────── talentos ─────────

        record Nombre(String pila, String completo, String apellido) {
        }

        Nombre nombrePersona(boolean mujer) {
            String pila = pick(mujer ? SeedCatalogo.NOMBRES_F : SeedCatalogo.NOMBRES_M);
            String completo = pila;
            if (prob(0.22))
                completo += " " + pick(mujer ? SeedCatalogo.NOMBRES_F : SeedCatalogo.NOMBRES_M);
            String ap1 = pick(SeedCatalogo.APELLIDOS);
            String ap2 = pick(SeedCatalogo.APELLIDOS);
            return new Nombre(pila, completo + " " + ap1 + " " + ap2, ap1);
        }

        String correoUnico(String pila, String apellido) {
            String base = slug(pila) + "." + slug(apellido);
            if (prob(0.3))
                base += String.format("%02d", rnd.nextInt(100));
            String dominio = SeedCatalogo.DOMINIOS_CORREO[peso(SeedCatalogo.PESO_DOMINIOS)];
            String correo = base + "@" + dominio;
            while (correos.contains(correo)) {
                int n = usadosBase.merge(base, 1, Integer::sum);
                correo = base + n + "@" + dominio;
            }
            correos.add(correo);
            return correo;
        }

        void generarTalentos() {
            List<Date> fechas = fechasOrdenadas(nTalentos, 2, 800);
            List<Usuario> demos = new ArrayList<>();
            if (demoTalento)
                demos.add(uDemoTalento = talentoDemoPrincipal());
            if (demoDiseno)
                demos.add(uDemoDiseno = talentoDemoDiseno());
            for (Usuario u : demos)
                talentosGen.add(new TalentoGen(u, demoGrupo(u), 1, null));

            for (int i = 0; i < nTalentos; i++) {
                String grupo = SeedCatalogo.GRUPOS_TALENTO[peso(SeedCatalogo.PESO_GRUPOS_TALENTO)];
                List<Arq> candidatos = SeedCatalogo.ARQUETIPOS.stream().filter(a -> a.grupo().equals(grupo)).toList();
                Arq arq = pick(candidatos);
                int tier = peso(28, 34, 28, 10);
                talentosGen.add(new TalentoGen(talento(arq, grupo, tier, fechas.get(i)), grupo, tier, arq));
            }
            // favoritos: ofertas activas del mismo grupo
            for (TalentoGen tg : talentosGen) {
                if (tg.arq() == null)
                    continue;
                List<Empleo> pool = activosPorGrupo.getOrDefault(tg.grupo(), List.of());
                int k = pool.isEmpty() ? 0 : peso(45, 20, 15, 10, 6, 4);
                Set<String> ids = new java.util.LinkedHashSet<>();
                for (int j = 0; j < k; j++)
                    ids.add(pick(pool).getId());
                tg.u().setEmpleosFavoritos(new ArrayList<>(ids));
            }
            for (TalentoGen tg : talentosGen) {
                if (tg.arq() != null)
                    continue;
                List<Empleo> pool = activosPorGrupo.getOrDefault(tg.grupo(), List.of());
                tg.u().setEmpleosFavoritos(new ArrayList<>(pool.stream().limit(3).map(Empleo::getId).toList()));
            }
            talentosGen.sort(Comparator.comparing(tg -> tg.u().getCreadoEn()));
        }

        String demoGrupo(Usuario u) {
            return "ui-ux".equals(u.getEspecialidad()) ? "ui-ux" : "fullstack";
        }

        Usuario talento(Arq arq, String grupo, int tier, Date creado) {
            boolean mujer = prob(0.47);
            Nombre n = nombrePersona(mujer);
            String correo = correoUnico(n.pila(), n.apellido());
            String handle = correo.substring(0, correo.indexOf('@')).replace(".", "-");
            int anios = switch (tier) {
                case 0 -> 1 + rnd.nextInt(2);
                case 1 -> 2 + rnd.nextInt(3);
                case 2 -> 5 + rnd.nextInt(4);
                default -> 9 + rnd.nextInt(6);
            };
            String rolBase = pick(arq.roles());
            String rol = switch (tier) {
                case 0 -> "Junior " + rolBase;
                case 2 -> "Senior " + rolBase;
                case 3 -> "Lead " + rolBase;
                default -> rolBase;
            };
            List<String> stack = new ArrayList<>(Arrays.asList(arq.stack()));
            Collections.shuffle(stack, rnd);
            stack = new ArrayList<>(stack.subList(0, Math.min(stack.size(), 3 + rnd.nextInt(3))));
            // el stack principal siempre incluye la primera herramienta del arquetipo (la que define el perfil)
            if (!stack.contains(arq.stack()[0]))
                stack.set(0, arq.stack()[0]);
            for (int x = rnd.nextInt(3); x > 0; x--) {
                String extra = pick(SeedCatalogo.EXTRAS_STACK);
                if (!stack.contains(extra))
                    stack.add(extra);
            }
            String ciudad = SeedCatalogo.CIUDADES[peso(SeedCatalogo.PESO_CIUDADES)];
            String enfoque = pick(SeedCatalogo.ENFOQUE.get(grupo));

            Usuario u = new Usuario();
            u.setTipo("talento");
            u.setNombre(n.completo());
            u.setRolTitulo(rol);
            u.setBio(String.format(pick(SeedCatalogo.BIOS), rol, anios(anios), enfoque, stack.get(0), stack.get(Math.min(1, stack.size() - 1)),
                    ciudad.substring(0, ciudad.indexOf(',')), stack.get(Math.min(2, stack.size() - 1))));
            u.setStack(stack);
            u.setEspecialidad(arq.clave());
            String modalidad = SeedCatalogo.MODALIDADES_TALENTO[peso(45, 35, 20)];
            u.setModalidad(modalidad);
            u.setDisponibilidad(tier == 0 && prob(0.25) ? "practicas" : SeedCatalogo.DISPONIBILIDADES[peso(58, 12, 30, 0)]);
            u.setSalarioEsperado(salarioEsperado(tier, modalidad, u.getDisponibilidad(), grupo));
            u.setCorreo(correo);
            u.setEmailContacto(correo);
            u.setTelefono(telefono());
            u.setPortfolioUrl(prob(0.6) ? "https://" + handle + ".example.com" : null);
            u.setLinkedinUrl(prob(0.85) ? "https://linkedin.example.com/in/" + handle : null);
            boolean programa = !grupo.equals("ui-ux") && !grupo.equals("diseno-grafico");
            u.setGithubUrl(programa && prob(0.75) ? "https://github.example.com/" + handle : null);
            u.setPassword(hash);
            u.setActivo(true);
            u.setCreadoEn(creado);
            u.setActualizadoEn(limitar(mas(creado, rnd.nextDouble() * 60)));
            u.setUbicacion(ciudad);
            u.setCvBullets(pickVarios(SeedCatalogo.CV_BULLETS, 2 + rnd.nextInt(3)));
            u.setHabilidadesBlandas(pickVarios(SeedCatalogo.HABILIDADES_BLANDAS, 3 + rnd.nextInt(3)));
            u.setNivelEspanol(prob(0.85) ? 5 : 4);
            u.setNivelIngles(1 + peso(5, 20, 30, 30, 15));
            u.setCvExperiencias(experiencias(rol, rolBase, grupo, tier, anios));
            u.setCvEducaciones(educaciones(grupo, anios, tier));
            u.setPerfilPublico(prob(0.92));
            u.setMostrarCorreo(prob(0.2));
            u.setPermitirMensajes(prob(0.9));
            u.setOnboardingCompleto(true);
            u.setDestacar(prob(0.03));
            u.setEmpleosFavoritos(new ArrayList<>());
            u.setId(idEn(creado));
            return u;
        }

        String salarioEsperado(int tier, String modalidad, String disponibilidad, String grupo) {
            if ("freelance".equals(disponibilidad)) {
                long lo = redondear(15 + tier * 10 + rnd.nextInt(20), 5);
                return "USD " + lo + " – " + (lo + 10 + rnd.nextInt(20)) + " / hora";
            }
            if ("practicas".equals(disponibilidad))
                return "COP 1.500.000 – 2.200.000 / mes";
            if ("remoto".equals(modalidad) && prob(0.3)) {
                long[][] b = { { 1100, 2000 }, { 2000, 3600 }, { 3600, 6500 }, { 6000, 10000 } };
                long lo = redondear(b[tier][0] + (long) (rnd.nextDouble() * (b[tier][1] - b[tier][0]) * 0.6), 100);
                return "USD " + miles(lo) + " – " + miles(redondear((long) (lo * 1.3), 100)) + " / mes";
            }
            long[][] b = { { 2_400_000, 4_500_000 }, { 4_500_000, 8_000_000 }, { 8_000_000, 14_000_000 }, { 13_000_000, 22_000_000 } };
            double f = grupo.equals("ui-ux") || grupo.equals("diseno-grafico") ? 0.85 : 1.0;
            long lo = redondear((long) ((b[tier][0] + rnd.nextDouble() * (b[tier][1] - b[tier][0]) * 0.6) * f), 100_000);
            return "COP " + miles(lo) + " – " + miles(redondear((long) (lo * 1.3), 100_000)) + " / mes";
        }

        List<Usuario.CvExperiencia> experiencias(String rol, String rolBase, String grupo, int tier, int anios) {
            int n = tier == 0 ? 1 : tier == 1 ? 2 : tier == 2 ? 3 : 3 + rnd.nextInt(2);
            List<Usuario.CvExperiencia> lista = new ArrayList<>();
            int fin = anioActual;
            for (int i = 0; i < n; i++) {
                int dur = i == n - 1 ? Math.max(1, anios - (anioActual - fin)) : Math.max(1, Math.round(anios / (float) n));
                int inicio = fin - dur;
                Usuario.CvExperiencia x = new Usuario.CvExperiencia();
                x.setCargo(i == 0 ? rol : (i == n - 1 && tier > 0 ? "Junior " + rolBase : rolBase));
                x.setEmpresa(pick(SeedCatalogo.EMPRESAS_CV));
                x.setPeriodo(i == 0 ? inicio + " – Presente" : inicio + " – " + fin);
                x.setModalidad(pick(new String[] { "Remoto", "Híbrido", "Presencial" }));
                x.setDescripcion(pick(SeedCatalogo.LOGROS.get(grupo)));
                lista.add(x);
                fin = inicio;
            }
            return lista;
        }

        List<Usuario.CvEducacion> educaciones(String grupo, int anios, int tier) {
            Usuario.CvEducacion e = new Usuario.CvEducacion();
            e.setInstitucion(pick(SeedCatalogo.UNIVERSIDADES));
            e.setTitulo(pick(SeedCatalogo.TITULOS_ESTUDIO.get(grupo)));
            int fin = Math.max(2008, anioActual - anios);
            e.setPeriodo(tier == 0 && prob(0.4) ? (anioActual - 3) + " – Presente" : (fin - 5) + " – " + fin);
            return new ArrayList<>(List.of(e));
        }

        // ───────── proyectos ─────────

        void generarProyectos() {
            for (TalentoGen tg : talentosGen) {
                Usuario u = tg.u();
                if (tg.arq() == null) {
                    proyectosDemo(tg);
                    continue;
                }
                int n = peso(30, 25, 22, 15, 8);
                for (int i = 0; i < n; i++)
                    proyectos.add(proyecto(tg, i == 0 && prob(0.5), null, null, null));
            }
        }

        Proyecto proyecto(TalentoGen tg, boolean destacar, String nombre, String categoria, String descripcion) {
            Usuario u = tg.u();
            String grupo = tg.grupo();
            boolean diseno = grupo.equals("ui-ux") || grupo.equals("diseno-grafico");
            if (categoria == null)
                categoria = pick(SeedCatalogo.CATEGORIAS_PROYECTO.get(grupo));
            String dominio = pick(SeedCatalogo.DOMINIOS_PROYECTO);
            String marca = pick(SeedCatalogo.NOMBRES_MARCA);
            if (nombre == null)
                nombre = marca + " · " + (diseno ? pick(SeedCatalogo.PRODUCTOS_PROYECTO_DISENO) : pick(SeedCatalogo.PRODUCTOS_PROYECTO_PROG));
            List<String> herramientas = new ArrayList<>(u.getStack().subList(0, Math.min(u.getStack().size(), 2 + rnd.nextInt(3))));
            String corta = descripcion != null ? descripcion
                    : diseno ? "Propuesta de diseño para " + dominio + ": investigación, exploración visual y entrega final lista para desarrollo."
                            : "Solución para " + dominio + " construida con " + String.join(", ", herramientas) + ", con foco en rendimiento y mantenibilidad.";
            Proyecto p = new Proyecto();
            p.setUsuarioId(u.getId());
            p.setUsuarioCorreo(u.getCorreo());
            p.setNombre(nombre);
            p.setCategoria(categoria);
            p.setTipo(SeedCatalogo.TIPOS_PROYECTO[peso(SeedCatalogo.PESO_TIPOS_PROYECTO)]);
            p.setDescripcionCorta(corta.length() > 200 ? corta.substring(0, 200) : corta);
            p.setDescripcionExtendida(corta + " Trabajé de punta a punta en el proceso: definición del problema, iteraciones con usuarios y entrega documentada.");
            p.setAnio(String.valueOf(Math.max(2022, anioActual - peso(40, 30, 15, 10, 5))));
            p.setLinkProyecto(prob(0.65) ? "https://example.com/proyectos/" + slug(nombre) : null);
            p.setBannerClase(diseno ? pick(SeedCatalogo.COVERS_DISENO) : "cover-terminal");
            p.setIconoClave(pick(SeedCatalogo.ICONOS_PROYECTO));
            p.setColorIcono(pick(SeedCatalogo.COLORES));
            p.setStack(herramientas);
            p.setEtiquetas(pickVarios(SeedCatalogo.ETIQUETAS_PROYECTO, 2 + rnd.nextInt(2)));
            p.setRol(pick(diseno ? SeedCatalogo.ROLES_PROYECTO_DISENO : SeedCatalogo.ROLES_PROYECTO_PROG));
            p.setDuracion(pick(SeedCatalogo.DURACIONES));
            p.setPublicado(prob(0.88));
            p.setIncluirCv(prob(0.7));
            p.setDestacar(destacar);
            Date c = limitar(mas(u.getCreadoEn(), rnd.nextDouble() * Math.max(1, (ahora.getTime() - u.getCreadoEn().getTime()) / (double) DIA)));
            p.setCreadoEn(c);
            p.setActualizadoEn(c);
            p.setId(idEn(c));
            return p;
        }

        // ───────── solicitudes ─────────

        void generarSolicitudes() {
            for (TalentoGen tg : talentosGen) {
                if (tg.arq() == null)
                    continue;
                int k = peso(18, 17, 17, 15, 12, 9, 6, 4, 2);
                for (int j = 0; j < k; j++) {
                    boolean mismoGrupo = prob(0.72);
                    List<Empleo> pool = mismoGrupo ? empleosPorGrupo.getOrDefault(tg.grupo(), List.of()) : null;
                    Empleo e = pool != null && !pool.isEmpty() ? pick(pool) : pick(empleosGen).e();
                    Solicitud s = solicitudAutomatica(tg, e, null);
                    if (s != null)
                        solicitudes.add(s);
                }
            }
            solicitudesDemo();
        }

        /** Crea la solicitud con el estado que corresponde a la fase de la oferta; null si ya existe o no es posible. */
        Solicitud solicitudAutomatica(TalentoGen tg, Empleo e, String estadoForzado) {
            Usuario u = tg.u();
            String par = u.getId() + "|" + e.getId();
            if (paresSolicitud.contains(par))
                return null;
            Date base = e.getFechaPublicacion().after(u.getCreadoEn()) ? e.getFechaPublicacion() : u.getCreadoEn();
            if (!base.before(ahora))
                return null;
            double span = (ahora.getTime() - base.getTime()) / (double) DIA;
            Date fecha = limitar(mas(base, rnd.nextDouble() * Math.min(span, 40)));
            boolean cerrada = "CERRADO".equals(e.getEstado());
            String fase = e.getFaseReclutamiento() == null ? "ABIERTO" : e.getFaseReclutamiento();
            double pDecision = cerrada ? 0.97 : switch (fase) {
                case "SCREENING" -> 0.45;
                case "ENTREVISTAS" -> 0.6;
                case "OFERTA" -> 0.75;
                default -> 0.25;
            };
            String estado = "revision";
            Date fechaDecision = null;
            if (estadoForzado != null) {
                estado = estadoForzado;
                fechaDecision = "revision".equals(estado) ? null : limitar(mas(fecha, 1 + rnd.nextInt(6)));
            } else if (prob(pDecision)) {
                Date dec = mas(fecha, 1 + rnd.nextInt(14));
                if (dec.after(ahora))
                    dec = cerrada ? limitar(dec) : null;
                if (dec != null && dec.before(fecha))
                    dec = fecha;
                if (dec != null) {
                    double pAprob = cerrada ? 0.3 : "OFERTA".equals(fase) ? 0.5 : "ENTREVISTAS".equals(fase) ? 0.35 : 0.2;
                    int cupo = e.getVacantes() == null ? 1 : e.getVacantes();
                    if (prob(pAprob) && aprobadasPorEmpleo.getOrDefault(e.getId(), 0) < cupo) {
                        estado = "aprobada";
                        aprobadasPorEmpleo.merge(e.getId(), 1, Integer::sum);
                    } else if (e.isScreeningActivo() && prob(0.15)) {
                        estado = "descartado";
                        dec = fecha;
                    } else {
                        estado = "rechazada";
                    }
                    fechaDecision = dec;
                }
            }
            return construirSolicitud(tg, e, estado, fecha, fechaDecision);
        }

        Solicitud construirSolicitud(TalentoGen tg, Empleo e, String estado, Date fecha, Date fechaDecision) {
            Usuario u = tg.u();
            paresSolicitud.add(u.getId() + "|" + e.getId());
            Solicitud s = new Solicitud();
            s.setUsuarioId(u.getId());
            s.setUsuarioCorreo(u.getCorreo());
            s.setUsuarioNombre(u.getNombre());
            s.setUsuarioRolTitulo(u.getRolTitulo());
            s.setEmpleoId(e.getId());
            s.setEmpleoTitulo(e.getTitulo());
            s.setEmpleoCategoria(e.getCategoria());
            s.setEmpleoSeniority(e.getSeniority());
            s.setEmpleoModalidad(e.getModalidad());
            s.setEmpleoContrato(e.getContrato());
            s.setEmpleoEmpresaNombre(e.getEmpresaNombre());
            s.setEmpleoEmpresaSector(e.getEmpresaSector());
            s.setEmpleoEmpresaTamano(e.getEmpresaTamano());
            s.setEmpleoEmpresaWeb(e.getEmpresaWeb());
            s.setEmpleoEmpresaDescripcion(e.getEmpresaDescripcion());
            s.setEmpleoIcono(e.getIcono());
            s.setEmpleoColorIcono(e.getColorIcono());
            s.setEmpleoStack(e.getStack());
            s.setEmpleoSoftSkills(e.getSoftSkills());
            s.setEmpleoDescripcionRol(e.getDescripcionRol());
            s.setEmpleoRequisitosDeseables(e.getRequisitosDeseables());
            s.setEmpleoMoneda(e.getMoneda());
            s.setEmpleoSalarioMin(e.getSalarioMin());
            s.setEmpleoSalarioMax(e.getSalarioMax());
            s.setEmpleoSalarioPeriodo(e.getSalarioPeriodo());
            s.setEmpleoTimezone(e.getTimezone());
            s.setEmpleoIngles(e.getIngles());
            s.setEmpleoVacantes(e.getVacantes());
            s.setEmpleoMostrarSalario(e.isMostrarSalario());
            s.setEstado(estado);
            s.setFechaSolicitud(fecha);
            String stackOferta = e.getStack() == null ? "mi stack" : e.getStack().split(",")[0].trim();
            s.setMensajeMotivacion(String.format(pick(SeedCatalogo.MENSAJES_MOTIVACION), e.getTitulo(), e.getEmpresaNombre(), stackOferta));
            List<String> ids = new ArrayList<>();
            List<Proyecto> propios = proyectosPorUsuario.getOrDefault(u.getId(), List.of());
            if (!propios.isEmpty() && prob(0.55)) {
                int cuantos = 1 + rnd.nextInt(Math.min(2, propios.size()));
                for (int i = 0; i < cuantos; i++)
                    ids.add(propios.get(i).getId());
            }
            s.setProyectosIds(ids);
            List<EventoEstado> historial = new ArrayList<>();
            historial.add(new EventoEstado("revision", fecha, "Postulación enviada"));
            if ("aprobada".equals(estado))
                historial.add(new EventoEstado("aprobada", fechaDecision, "Aceptada por la empresa"));
            else if ("rechazada".equals(estado))
                historial.add(new EventoEstado("rechazada", fechaDecision, "Rechazada por la empresa"));
            else if ("descartado".equals(estado))
                historial.add(new EventoEstado("descartado", fecha,
                        "Descartado automáticamente: stack no cumple los requisitos mínimos del empleo"));
            s.setHistorial(historial);
            s.setId(idEn(fecha));
            return s;
        }

        Date fechaDe(Solicitud s, String estado) {
            if (s.getHistorial() != null)
                for (int i = s.getHistorial().size() - 1; i >= 0; i--)
                    if (estado.equals(s.getHistorial().get(i).getEstado()))
                        return s.getHistorial().get(i).getFecha();
            return s.getFechaSolicitud();
        }

        // ───────── invitaciones ─────────

        void generarInvitaciones() {
            int objetivo = Math.max(0, nTalentos / 2);
            List<Empresa> conOfertas = empresas.stream().filter(e -> activosPorEmpresa.containsKey(e.getId())).toList();
            List<Empresa> pro = conOfertas.stream().filter(e -> PlanService.PLAN_PRO.equals(e.getPlan())).toList();
            Set<String> usadas = new HashSet<>();
            if (!conOfertas.isEmpty())
                for (int i = 0; i < objetivo; i++) {
                    Empresa em = !pro.isEmpty() && prob(0.6) ? pick(pro) : pick(conOfertas);
                    Empleo emp = prob(0.75) ? pick(activosPorEmpresa.get(em.getId())) : null;
                    List<TalentoGen> pool = emp != null ? talentosPorGrupo.get(grupoDeEmpleo.get(emp.getId())) : talentosGen;
                    if (pool == null || pool.isEmpty())
                        continue;
                    TalentoGen tg = pick(pool);
                    Usuario t = tg.u();
                    if (!t.isPerfilPublico() || Boolean.FALSE.equals(t.getPermitirMensajes()))
                        continue;
                    if (!usadas.add(em.getId() + "|" + t.getId() + "|" + (emp == null ? "" : emp.getId())))
                        continue;
                    String estado = new String[] { "pendiente", "aceptada", "rechazada" }[peso(55, 20, 25)];
                    invitaciones.add(invitacion(em, t, emp, estado, atras(rnd.nextDouble() * 60)));
                }
            if (uDemoTalento != null && eDemoEmpresa != null && activosPorEmpresa.containsKey(eDemoEmpresa.getId())) {
                invitaciones.add(invitacion(eDemoEmpresa, uDemoTalento, activosPorEmpresa.get(eDemoEmpresa.getId()).get(0), "pendiente", atras(1.5)));
                if (eDemoStartup != null)
                    invitaciones.add(invitacion(eDemoStartup, uDemoTalento, null, "pendiente", atras(4)));
            }
            if (uDemoDiseno != null && eDemoStartup != null)
                invitaciones.add(invitacion(eDemoStartup, uDemoDiseno, null, "pendiente", atras(2)));
        }

        Invitacion invitacion(Empresa em, Usuario t, Empleo emp, String estado, Date fecha) {
            Invitacion i = new Invitacion();
            i.setEmpresaId(em.getId());
            i.setEmpresaNombre(em.getNombreEmpresa());
            i.setEmpresaSector(em.getSector());
            i.setEmpresaWeb(em.getSitioWeb());
            i.setEmpresaDescripcion(em.getDescripcion());
            i.setEmpresaTamano(em.getTamano());
            i.setTalentoId(t.getId());
            i.setTalentoNombre(t.getNombre());
            i.setTalentoCorreo(t.getCorreo());
            String pila = t.getNombre().split(" ")[0];
            if (emp != null) {
                i.setEmpleoId(emp.getId());
                i.setEmpleoTitulo(emp.getTitulo());
                i.setEmpleoModalidad(emp.getModalidad());
                i.setEmpleoContrato(emp.getContrato());
                i.setEmpleoCategoria(emp.getCategoria());
                i.setEmpleoSalarioMin(emp.getSalarioMin());
                i.setEmpleoSalarioMax(emp.getSalarioMax());
                i.setEmpleoMoneda(emp.getMoneda());
                i.setEmpleoMostrarSalario(emp.isMostrarSalario());
                i.setMensaje("Hola " + pila + ", revisamos tu perfil y creemos que encajas con el rol de " + emp.getTitulo()
                        + " en " + em.getNombreEmpresa() + ". ¿Tienes disponibilidad esta semana para conversar?");
            } else {
                i.setMensaje("Hola " + pila + ", nos gustó tu portafolio y queremos tenerte en cuenta para próximas vacantes en "
                        + em.getNombreEmpresa() + ". ¿Te interesa que hablemos?");
            }
            i.setEstado(estado);
            i.setFechaEnvio(fecha);
            i.setId(idEn(fecha));
            return i;
        }

        // ───────── mensajes ─────────

        final Map<String, List<Mensaje>> hilos = new LinkedHashMap<>();

        void generarMensajes() {
            for (Solicitud s : solicitudes) {
                boolean aprobada = "aprobada".equals(s.getEstado());
                if (!(aprobada ? prob(0.55) : "revision".equals(s.getEstado()) && prob(0.02)))
                    continue;
                Empleo e = empleoPorId.get(s.getEmpleoId());
                Empresa em = empresaPorId.get(e.getEmpresaId());
                Date t = mas(fechaDe(s, aprobada ? "aprobada" : "revision"), (1 + rnd.nextInt(6)) / 24.0);
                int n = 2 + rnd.nextInt(4);
                List<Mensaje> hilo = new ArrayList<>();
                for (int i = 0; i < n; i++) {
                    if (t.after(ahora))
                        break;
                    boolean deEmpresa = i % 2 == 0;
                    Mensaje m = new Mensaje();
                    m.setSolicitudId(s.getId());
                    m.setRemitenteId(deEmpresa ? em.getId() : s.getUsuarioId());
                    m.setRemitenteNombre(deEmpresa ? em.getNombreEmpresa() : s.getUsuarioNombre());
                    m.setDestinatarioId(deEmpresa ? s.getUsuarioId() : em.getId());
                    String texto = deEmpresa ? SeedCatalogo.MENSAJES_CHAT_EMPRESA[Math.min(i / 2, SeedCatalogo.MENSAJES_CHAT_EMPRESA.length - 1)]
                            : SeedCatalogo.MENSAJES_CHAT_TALENTO[Math.min(i / 2, SeedCatalogo.MENSAJES_CHAT_TALENTO.length - 1)];
                    m.setTexto(String.format(texto, s.getUsuarioNombre().split(" ")[0], s.getEmpleoTitulo()));
                    m.setFechaEnvio(t);
                    m.setLeido(true);
                    m.setId(idEn(t));
                    hilo.add(m);
                    t = mas(t, (1 + rnd.nextInt(20)) / 24.0);
                }
                if (hilo.isEmpty())
                    continue;
                Mensaje ultimo = hilo.get(hilo.size() - 1);
                if (ultimo.getFechaEnvio().after(atras(14)) && prob(0.4))
                    ultimo.setLeido(false);
                mensajes.addAll(hilo);
                hilos.put(s.getId(), hilo);
            }
        }

        // ───────── cobros ─────────

        void generarCobros() {
            for (Solicitud s : solicitudes) {
                if (!"aprobada".equals(s.getEstado()))
                    continue;
                Empresa em = empresaPorId.get(empleoPorId.get(s.getEmpleoId()).getEmpresaId());
                if (PlanService.PLAN_PRO.equals(em.getPlan()))
                    continue;
                Cobro c = new Cobro();
                c.setEmpresaId(em.getId());
                c.setSolicitudId(s.getId());
                c.setEmpleoTitulo(s.getEmpleoTitulo());
                c.setMonto(PlanService.TARIFA_COLOCACION);
                c.setMotivo("Tarifa de colocación exitosa");
                c.setFecha(fechaDe(s, "aprobada"));
                c.setId(idEn(c.getFecha()));
                cobros.add(c);
            }
        }

        // ───────── notificaciones ─────────

        void generarNotificaciones() {
            Date corte = atras(60);
            for (Solicitud s : solicitudes) {
                Empleo e = empleoPorId.get(s.getEmpleoId());
                if (s.getFechaSolicitud().after(corte))
                    notificaciones.add(notif(e.getEmpresaId(), "NUEVA_SOLICITUD", "Nueva postulación",
                            s.getUsuarioNombre() + " se postuló a " + s.getEmpleoTitulo(), s.getFechaSolicitud(),
                            "/empleos/" + e.getId() + "/postulaciones/" + s.getId()));
                String est = s.getEstado();
                if ("aprobada".equals(est) || "rechazada".equals(est)) {
                    Date f = fechaDe(s, est);
                    if (f.after(corte)) {
                        boolean ok = "aprobada".equals(est);
                        notificaciones.add(notif(s.getUsuarioId(), "ESTADO_SOLICITUD",
                                ok ? "¡Tu postulación fue aceptada! 🎉" : "Actualización de tu postulación",
                                s.getEmpleoEmpresaNombre() + (ok ? " aceptó tu postulación a " : " actualizó tu postulación a ") + s.getEmpleoTitulo(),
                                f, "/solicitudes/" + s.getId()));
                    }
                }
            }
            for (Invitacion i : invitaciones) {
                if ("pendiente".equals(i.getEstado()) && i.getFechaEnvio().after(atras(30)))
                    notificaciones.add(notif(i.getTalentoId(), "NUEVA_INVITACION", "Una empresa quiere conocerte",
                            i.getEmpresaNombre() + " te envió una invitación", i.getFechaEnvio(), "/solicitudes?estado=invitaciones"));
                else if (!"pendiente".equals(i.getEstado()) && i.getFechaEnvio().after(atras(30)))
                    notificaciones.add(notif(i.getEmpresaId(), "ESTADO_INVITACION",
                            "aceptada".equals(i.getEstado()) ? "Invitación aceptada" : "Invitación rechazada",
                            i.getTalentoNombre() + ("aceptada".equals(i.getEstado()) ? " aceptó" : " rechazó") + " tu invitación",
                            mas(i.getFechaEnvio(), 1 + rnd.nextInt(3)), "/talentos/" + i.getTalentoId()));
            }
            for (Map.Entry<String, List<Mensaje>> h : hilos.entrySet()) {
                Mensaje ultimo = h.getValue().get(h.getValue().size() - 1);
                if (!ultimo.isLeido())
                    notificaciones.add(notif(ultimo.getDestinatarioId(), "NUEVO_MENSAJE", "Nuevo mensaje",
                            ultimo.getRemitenteNombre() + " te escribió", ultimo.getFechaEnvio(), "/chat/" + h.getKey()));
            }
            // las notificaciones más antiguas de 10 días ya están leídas; el resto, a veces
            for (Notificacion n : notificaciones)
                n.setLeida(n.getFechaCreacion().before(atras(10)) || prob(0.35));
            // las que justifican un badge (mensajes sin leer) se quedan sin leer
            for (Notificacion n : notificaciones)
                if ("NUEVO_MENSAJE".equals(n.getTipo()))
                    n.setLeida(false);
            notificaciones.sort(Comparator.comparing(Notificacion::getFechaCreacion));
        }

        Notificacion notif(String destinatario, String tipo, String titulo, String mensaje, Date fecha, String url) {
            Notificacion n = new Notificacion();
            n.setDestinatarioId(destinatario);
            n.setTipo(tipo);
            n.setTitulo(titulo);
            n.setMensaje(mensaje);
            n.setFechaCreacion(fecha);
            n.setUrl(url);
            n.setId(idEn(fecha));
            return n;
        }

        // ───────── cuentas de demo ─────────

        Usuario talentoDemoPrincipal() {
            Usuario u = new Usuario();
            u.setTipo("talento");
            u.setNombre("Carlos Andrés Ramírez");
            u.setRolTitulo("Full Stack Developer");
            u.setBio("Desarrollador con 4 años de experiencia en Java y React, apasionado por construir productos digitales de alto impacto. "
                    + "Me gustan los equipos pequeños, el código bien probado y compartir lo que aprendo.");
            u.setStack(new ArrayList<>(List.of("Java", "Spring Boot", "React", "TypeScript", "PostgreSQL", "Docker", "Git")));
            u.setEspecialidad("fullstack");
            u.setModalidad("hibrido");
            u.setSalarioEsperado("COP 7.000.000 – 9.000.000 / mes");
            u.setDisponibilidad("full-time");
            u.setCorreo(DEMO_TALENTO);
            u.setEmailContacto(DEMO_TALENTO);
            u.setTelefono("+57 315 555 0001");
            u.setPortfolioUrl("https://carlos.example.com");
            u.setLinkedinUrl("https://linkedin.example.com/in/carlos-ramirez");
            u.setGithubUrl("https://github.example.com/carlos-ramirez");
            u.setPassword(hashDemo);
            u.setActivo(true);
            u.setPerfilPublico(true);
            u.setMostrarCorreo(true);
            u.setPermitirMensajes(true);
            u.setOnboardingCompleto(true);
            u.setDestacar(true);
            u.setUbicacion("Bogotá, Colombia");
            u.setNivelEspanol(5);
            u.setNivelIngles(4);
            u.setHabilidadesBlandas(new ArrayList<>(List.of("Trabajo en equipo", "Comunicación asertiva", "Resolución de problemas",
                    "Liderazgo", "Adaptabilidad", "Proactividad")));
            u.setCvBullets(new ArrayList<>(List.of(SeedCatalogo.CV_BULLETS[0], SeedCatalogo.CV_BULLETS[1], SeedCatalogo.CV_BULLETS[2],
                    SeedCatalogo.CV_BULLETS[3])));
            Usuario.CvExperiencia e1 = new Usuario.CvExperiencia();
            e1.setCargo("Full Stack Developer");
            e1.setEmpresa("Andina Software");
            e1.setPeriodo((anioActual - 3) + " – Presente");
            e1.setModalidad("Remoto");
            e1.setDescripcion("Desarrollo de microservicios con Spring Boot y frontends en React para clientes del sector financiero.");
            Usuario.CvExperiencia e2 = new Usuario.CvExperiencia();
            e2.setCargo("Backend Developer");
            e2.setEmpresa("Costa Pay");
            e2.setPeriodo((anioActual - 5) + " – " + (anioActual - 3));
            e2.setModalidad("Presencial");
            e2.setDescripcion("APIs REST para banca digital con Java y PostgreSQL, con pruebas automáticas y despliegue continuo.");
            u.setCvExperiencias(new ArrayList<>(List.of(e1, e2)));
            Usuario.CvEducacion ed = new Usuario.CvEducacion();
            ed.setInstitucion("Universidad Nacional");
            ed.setTitulo("Ingeniería de Sistemas");
            ed.setPeriodo((anioActual - 10) + " – " + (anioActual - 5));
            u.setCvEducaciones(new ArrayList<>(List.of(ed)));
            u.setCreadoEn(atras(420));
            u.setActualizadoEn(atras(2));
            u.setEmpleosFavoritos(new ArrayList<>());
            u.setId(idEn(u.getCreadoEn()));
            correos.add(DEMO_TALENTO);
            return u;
        }

        Usuario talentoDemoDiseno() {
            Usuario u = new Usuario();
            u.setTipo("talento");
            u.setNombre("Laura Camila Ospina");
            u.setRolTitulo("Product Designer");
            u.setBio("Diseñadora de producto con 5 años creando interfaces y sistemas de diseño para equipos de software. "
                    + "Trabajo de la investigación al handoff, con foco en accesibilidad.");
            u.setStack(new ArrayList<>(List.of("Figma", "Design Systems", "Prototipado", "User Research", "Accesibilidad", "Framer")));
            u.setEspecialidad("ui-ux");
            u.setModalidad("remoto");
            u.setSalarioEsperado("COP 6.000.000 – 8.000.000 / mes");
            u.setDisponibilidad("full-time");
            u.setCorreo(DEMO_DISENO);
            u.setEmailContacto(DEMO_DISENO);
            u.setTelefono("+57 316 555 0002");
            u.setPortfolioUrl("https://laura.example.com");
            u.setLinkedinUrl("https://linkedin.example.com/in/laura-ospina");
            u.setPassword(hashDemo);
            u.setActivo(true);
            u.setPerfilPublico(true);
            u.setPermitirMensajes(true);
            u.setOnboardingCompleto(true);
            u.setUbicacion("Medellín, Colombia");
            u.setNivelEspanol(5);
            u.setNivelIngles(4);
            u.setHabilidadesBlandas(new ArrayList<>(List.of("Empatía", "Comunicación asertiva", "Creatividad", "Pensamiento crítico")));
            u.setCvBullets(new ArrayList<>(List.of(SeedCatalogo.CV_BULLETS[6], SeedCatalogo.CV_BULLETS[4])));
            Usuario.CvExperiencia e1 = new Usuario.CvExperiencia();
            e1.setCargo("Product Designer");
            e1.setEmpresa("Estudio Faro");
            e1.setPeriodo((anioActual - 3) + " – Presente");
            e1.setModalidad("Remoto");
            e1.setDescripcion("Diseño de un sistema de diseño adoptado por tres equipos de producto y pruebas de usabilidad quincenales.");
            u.setCvExperiencias(new ArrayList<>(List.of(e1)));
            Usuario.CvEducacion ed = new Usuario.CvEducacion();
            ed.setInstitucion("Universidad Pontificia Bolivariana");
            ed.setTitulo("Diseño Gráfico");
            ed.setPeriodo((anioActual - 10) + " – " + (anioActual - 6));
            u.setCvEducaciones(new ArrayList<>(List.of(ed)));
            u.setCreadoEn(atras(300));
            u.setActualizadoEn(atras(3));
            u.setEmpleosFavoritos(new ArrayList<>());
            u.setId(idEn(u.getCreadoEn()));
            correos.add(DEMO_DISENO);
            return u;
        }

        void proyectosDemo(TalentoGen tg) {
            if (tg.u() == uDemoTalento) {
                proyectos.add(proyecto(tg, true, "DevTrack · Gestión de tareas", "Full Stack",
                        "Plataforma de gestión de tareas con tableros Kanban, comentarios y reportes en tiempo real."));
                proyectos.add(proyecto(tg, false, "FinBot · Asistente financiero", "Backend",
                        "Asistente que analiza gastos y recomienda metas de ahorro con alertas semanales."));
                proyectos.add(proyecto(tg, false, "ShopLite · E-commerce", "Frontend",
                        "Tienda en línea ligera con carrito, pasarela de pago de pruebas y panel de administración."));
                proyectos.add(proyecto(tg, false, "Cauca · API de eventos", "Backend",
                        "API de eventos y boletería con colas, caché y documentación OpenAPI."));
            } else {
                proyectos.add(proyecto(tg, true, "Sistema de diseño Brisa", "Design System",
                        "Biblioteca de componentes y tokens en Figma con guías de accesibilidad y documentación para desarrollo."));
                proyectos.add(proyecto(tg, false, "Rediseño app de reservas", "UI/UX Design",
                        "Rediseño del flujo de reserva con pruebas de usabilidad: la conversión subió un 18 %."));
                proyectos.add(proyecto(tg, false, "Panel de analítica para clínica", "Dashboard SaaS",
                        "Tablero de indicadores clínicos pensado para lectura rápida en pantalla compartida."));
            }
            for (Proyecto p : proyectos.subList(Math.max(0, proyectos.size() - 4), proyectos.size()))
                if (p.getUsuarioId() != null && p.getUsuarioId().equals(tg.u().getId())) {
                    p.setPublicado(true);
                    p.setIncluirCv(true);
                }
        }

        void solicitudesDemo() {
            // ofertas de las empresas de demo
            List<Empleo> deLabs = eDemoEmpresa == null ? List.of() : empleosGen.stream().map(EmpleoGen::e)
                    .filter(e -> eDemoEmpresa.getId().equals(e.getEmpresaId())).toList();
            // 1) el talento de demo en las primeras ofertas de DevCoast Labs, en cada estado
            if (uDemoTalento != null) {
                TalentoGen tg = talentoGenPorId.get(uDemoTalento.getId());
                String[] estados = { "aprobada", "revision", "rechazada" };
                List<Empleo> activos = deLabs.stream().filter(e -> "ACTIVO".equals(e.getEstado())).toList();
                for (int i = 0; i < Math.min(3, activos.size()); i++) {
                    Empleo e = activos.get(i);
                    Date f = atras(18 - i * 4);
                    solicitudes.add(construirSolicitud(tg, e, estados[i], f, "revision".equals(estados[i]) ? null : mas(f, 3)));
                }
                // 2) postulaciones del demo a ofertas de otras empresas
                String[] ext = { "revision", "aprobada", "revision", "rechazada", "revision", "aprobada", "revision", "revision" };
                List<Empleo> afines = new ArrayList<>(activosPorGrupo.getOrDefault("fullstack", List.of()));
                afines.addAll(activosPorGrupo.getOrDefault("backend", List.of()));
                afines.sort(Comparator.comparingInt((Empleo e) -> -MatchEmpleo.porcentaje(uDemoTalento, e)));
                int hechos = 0;
                for (Empleo e : afines) {
                    if (hechos >= ext.length)
                        break;
                    if (eDemoEmpresa != null && eDemoEmpresa.getId().equals(e.getEmpresaId()) || paresSolicitud.contains(uDemoTalento.getId() + "|" + e.getId()))
                        continue;
                    Date f = atras(3 + hechos * 6 + rnd.nextInt(4));
                    solicitudes.add(construirSolicitud(tg, e, ext[hechos], f, "revision".equals(ext[hechos]) ? null : limitar(mas(f, 2 + rnd.nextInt(5)))));
                    hechos++;
                }
            }
            // 3) la diseñadora de demo
            if (uDemoDiseno != null) {
                TalentoGen tg = talentoGenPorId.get(uDemoDiseno.getId());
                List<Empleo> afines = new ArrayList<>(activosPorGrupo.getOrDefault("ui-ux", List.of()));
                afines.addAll(activosPorGrupo.getOrDefault("diseno-grafico", List.of()));
                afines.sort(Comparator.comparingInt((Empleo e) -> -MatchEmpleo.porcentaje(uDemoDiseno, e)));
                String[] est = { "revision", "aprobada", "revision", "rechazada", "revision" };
                int hechos = 0;
                for (Empleo e : afines) {
                    if (hechos >= est.length)
                        break;
                    if (paresSolicitud.contains(uDemoDiseno.getId() + "|" + e.getId()))
                        continue;
                    Date f = atras(2 + hechos * 5 + rnd.nextInt(3));
                    solicitudes.add(construirSolicitud(tg, e, est[hechos], f, "revision".equals(est[hechos]) ? null : limitar(mas(f, 2 + rnd.nextInt(4)))));
                    hechos++;
                }
            }
            // 4) candidatos de talentos al azar en las ofertas de DevCoast Labs y Marea Studio
            int[] porOferta = { 24, 10, 3, 7, 0, 14, 5, 2, 9 };
            List<Empleo> objetivo = new ArrayList<>(deLabs);
            if (eDemoStartup != null)
                empleosGen.stream().map(EmpleoGen::e).filter(e -> eDemoStartup.getId().equals(e.getEmpresaId())).forEach(objetivo::add);
            for (int i = 0; i < objetivo.size(); i++) {
                Empleo e = objetivo.get(i);
                int cuantos = i < porOferta.length ? porOferta[i] : 4;
                List<TalentoGen> pool = talentosPorGrupo.getOrDefault(grupoDeEmpleo.get(e.getId()), List.of());
                for (int c = 0; c < cuantos && !pool.isEmpty(); c++) {
                    TalentoGen tg = pick(pool);
                    if (tg.arq() == null)
                        continue;
                    Solicitud s = solicitudAutomatica(tg, e, null);
                    if (s != null)
                        solicitudes.add(s);
                }
            }
        }
    }
}
