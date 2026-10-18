/*
package com.example.devcoast.config;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

import com.example.devcoast.model.Empleo;
import com.example.devcoast.model.Empresa;
import com.example.devcoast.model.Invitacion;
import com.example.devcoast.model.Proyecto;
import com.example.devcoast.model.Solicitud;
import com.example.devcoast.model.Usuario;
import com.example.devcoast.repository.EmpleoRepository;
import com.example.devcoast.repository.EmpresaRepository;
import com.example.devcoast.repository.InvitacionRepository;
import com.example.devcoast.repository.ProyectoRepository;
import com.example.devcoast.repository.SolicitudRepository;
import com.example.devcoast.repository.UsuarioRepository;

@Component
public class DataSeeder implements CommandLineRunner {

        @Autowired
        private UsuarioRepository usuarioRepository;
        @Autowired
        private EmpresaRepository empresaRepository;
        @Autowired
        private EmpleoRepository empleoRepository;
        @Autowired
        private ProyectoRepository proyectoRepository;
        @Autowired
        private SolicitudRepository solicitudRepository;
        @Autowired
        private InvitacionRepository invitacionRepository;
        @Autowired
        private BCryptPasswordEncoder passwordEncoder;

        private static final int TOTAL_USUARIOS = 10000;
        private static final int TOTAL_EMPRESAS = 5500;

        private static final String[] NOMBRES = {
                        "Alejandro", "Andrés", "Camilo", "Carlos", "Daniel", "David", "Diego", "Eduardo",
                        "Felipe", "Fernando", "Gabriel", "Gustavo", "Héctor", "Iván", "Javier", "Jorge",
                        "José", "Juan", "Luis", "Manolo", "Martín", "Miguel", "Nicolás", "Oscar", "Pablo",
                        "Rafael", "Ricardo", "Roberto", "Rodrigo", "Santiago", "Sebastián", "Sergio",
                        "Tomás", "Valentín", "Víctor", "Ana", "Beatriz", "Carolina", "Catalina", "Clara",
                        "Daniela", "Elena", "Fernanda", "Gabriela", "Isabel", "Jessica", "Juliana",
                        "Karen", "Laura", "Lina", "Luisa", "María", "Mariana", "Natalia", "Paola",
                        "Patricia", "Sofía", "Valentina", "Valeria", "Viviana", "Ximena", "Yolanda"
        };

        private static final String[] APELLIDOS = {
                        "García", "Martínez", "López", "Rodríguez", "Hernández", "González", "Pérez",
                        "Torres", "Ramírez", "Flores", "Morales", "Jiménez", "Castro", "Vargas", "Ortega",
                        "Díaz", "Ruiz", "Suárez", "Medina", "Reyes", "Chávez", "Ramos", "Moreno", "Gutiérrez",
                        "Sánchez", "Mendoza", "Vega", "Herrera", "Aguilar", "Cabrera", "Silva", "Romero",
                        "Muñoz", "Alvarado", "Castillo", "Espinoza", "Ríos", "Guerrero", "Salazar", "Rojas"
        };

        private static final String[] ROLES = {
                        "Backend Developer", "Frontend Developer", "Full Stack Developer",
                        "Mobile Developer", "DevOps Engineer", "Data Engineer", "Data Scientist",
                        "ML Engineer", "UI/UX Designer", "Product Designer", "QA Engineer",
                        "Cloud Architect", "Security Engineer", "Tech Lead", "Software Architect",
                        "Android Developer", "iOS Developer", "Game Developer", "Blockchain Developer",
                        "Embedded Systems Engineer"
        };

        private static final String[] ESPECIALIDADES = {
                        "Desarrollo Web", "Desarrollo Móvil", "Inteligencia Artificial",
                        "Ciberseguridad", "Cloud Computing", "Bases de Datos", "DevOps",
                        "Blockchain", "IoT", "Big Data", "Realidad Aumentada", "Videojuegos"
        };

        private static final String[] MODALIDADES = {
                        "Remoto", "Presencial", "Híbrido"
        };

        private static final String[] DISPONIBILIDADES = {
                        "Inmediata", "En 1 mes", "En 3 meses", "Freelance / Por proyectos"
        };

        private static final String[] SALARIOS = {
                        "1.000.000 - 2.000.000 COP", "2.000.000 - 3.500.000 COP",
                        "3.500.000 - 5.000.000 COP", "5.000.000 - 8.000.000 COP",
                        "8.000.000 - 12.000.000 COP", "+ 12.000.000 COP",
                        "1.000 - 2.000 USD", "2.000 - 4.000 USD", "4.000 - 7.000 USD"
        };

        private static final String[][] STACKS = {
                        { "Java", "Spring Boot", "MySQL" },
                        { "Python", "Django", "PostgreSQL" },
                        { "JavaScript", "React", "Node.js" },
                        { "TypeScript", "Angular", "MongoDB" },
                        { "Kotlin", "Android", "Firebase" },
                        { "Swift", "iOS", "Xcode" },
                        { "Go", "Docker", "Kubernetes" },
                        { "C#", ".NET", "SQL Server" },
                        { "PHP", "Laravel", "MySQL" },
                        { "Vue.js", "Nuxt", "Tailwind CSS" },
                        { "Python", "TensorFlow", "Pandas" },
                        { "AWS", "Terraform", "Jenkins" },
                        { "React Native", "Expo", "Firebase" },
                        { "Rust", "WebAssembly", "Linux" },
                        { "Flutter", "Dart", "Firebase" },
                        { "Ruby", "Rails", "PostgreSQL" },
                        { "Scala", "Spark", "Hadoop" },
                        { "Figma", "Adobe XD", "Sketch" },
                        { "Solidity", "Web3.js", "Ethereum" },
                        { "C++", "Qt", "Embedded Linux" }
        };

        private static final String[] CIUDADES_CO = {
                        "Bogotá", "Medellín", "Cali", "Barranquilla", "Cartagena", "Bucaramanga",
                        "Pereira", "Manizales", "Santa Marta", "Cúcuta", "Ibagué", "Pasto",
                        "Villavicencio", "Montería", "Neiva", "Armenia", "Sincelejo", "Popayán",
                        "Valledupar", "Riohacha"
        };

        private static final String[] EMPRESAS_PREFIJOS = {
                        "Tech", "Soft", "Dev", "Digital", "Cloud", "Data", "Net", "Code", "Smart", "Cyber",
                        "Inno", "Nexus", "Apex", "Nova", "Pulse", "Core", "Bit", "Sys", "Pro", "Agile"
        };

        private static final String[] EMPRESAS_SUFIJOS = {
                        "Solutions", "Systems", "Labs", "Group", "Hub", "Works", "Studio", "Tech",
                        "Innovations", "Consulting", "Services", "Digital", "Ventures", "Partners", "Co"
        };

        private static final String[] SECTORES = {
                        "Tecnología", "Fintech", "Salud Digital", "E-commerce", "Educación",
                        "Logística", "Telecomunicaciones", "Entretenimiento", "Ciberseguridad",
                        "Inteligencia Artificial", "Marketing Digital", "Banca", "Seguros",
                        "Construcción Tech", "Agricultura Digital"
        };

        private static final String[] TAMANOS = {
                        "1-10 empleados", "11-50 empleados", "51-200 empleados",
                        "201-500 empleados", "501-1000 empleados", "+ 1000 empleados"
        };

        private static final String[] CARGOS = {
                        "CEO", "CTO", "COO", "Director de Talento", "Gerente de RRHH",
                        "Head of Engineering", "VP de Tecnología", "Director de Operaciones",
                        "Gerente General", "Fundador", "Co-Fundador", "Director Técnico"
        };

        private static final String[] DESCRIPCIONES_EMPRESA = {
                        "Empresa líder en soluciones tecnológicas para el sector financiero con más de 10 años de experiencia.",
                        "Startup de base tecnológica enfocada en transformación digital de PYMEs en Latinoamérica.",
                        "Compañía especializada en desarrollo de software a medida para clientes corporativos.",
                        "Firma consultora en ciberseguridad y gestión de riesgos tecnológicos.",
                        "Plataforma de educación en línea con presencia en más de 15 países de habla hispana.",
                        "Empresa de inteligencia artificial aplicada a la automatización de procesos empresariales.",
                        "Agencia de marketing digital y desarrollo de productos digitales.",
                        "Compañía de logística tecnológica con soluciones de última milla.",
                        "Startup de salud digital que conecta pacientes con profesionales de la salud.",
                        "Empresa de desarrollo de videojuegos y experiencias interactivas.",
                        "Consultora especializada en migración y arquitectura cloud.",
                        "Empresa de analítica de datos y business intelligence para retail.",
                        "Proveedor de soluciones IoT para industria y manufactura.",
                        "Plataforma SaaS para gestión de recursos humanos y nómina.",
                        "Empresa de desarrollo de aplicaciones móviles con enfoque en UX."
        };

        @Override
        public void run(String... args) throws Exception {

                long usuariosExistentes = usuarioRepository.count();
                long empresasExistentes = empresaRepository.count();

                boolean yaHayDatos = usuariosExistentes >= TOTAL_USUARIOS && empresasExistentes >= TOTAL_EMPRESAS;

                if (yaHayDatos && usuarioRepository.findByCorreo("demo@devcoast.co") != null) {
                        System.out.println("[DataSeeder] BD ya tiene datos suficientes. Verificando extras...");
                        seedDemoCompleto();
                        seedProyectosParaUsuarios();
                        seedSolicitudesParaUsuarios();
                        return;
                }

                System.out.println("[DataSeeder] Iniciando seed de datos...");
                long inicio = System.currentTimeMillis();

                String hashComun = passwordEncoder.encode("devcoast123");

                if (usuariosExistentes < TOTAL_USUARIOS) {
                        seedUsuarios(hashComun, (int) usuariosExistentes);
                }

                if (empresasExistentes < TOTAL_EMPRESAS) {
                        seedEmpresas(hashComun, (int) empresasExistentes);
                }

                seedDemoCompleto();
                seedProyectosParaUsuarios();
                seedSolicitudesParaUsuarios();

                long fin = System.currentTimeMillis();
                System.out.printf("[DataSeeder] Seed completado en %.1f segundos.%n",
                                (fin - inicio) / 1000.0);
        }

        private void seedUsuarios(String hash, int yaExisten) {
                int faltan = TOTAL_USUARIOS - yaExisten;
                System.out.printf("[DataSeeder] Insertando %d usuarios...%n", faltan);

                List<Usuario> lote = new ArrayList<>(500);
                Random rnd = ThreadLocalRandom.current();
                Set<String> correosUsados = new HashSet<>();

                for (int i = 0; i < faltan; i++) {
                        String nombre = pick(NOMBRES, rnd);
                        String apellido = pick(APELLIDOS, rnd);
                        String correo = generarCorreo(nombre, apellido, i, correosUsados);
                        correosUsados.add(correo);

                        String[] stack = STACKS[rnd.nextInt(STACKS.length)];
                        String ciudad = pick(CIUDADES_CO, rnd);

                        Usuario u = new Usuario();
                        u.setTipo("talento");
                        u.setNombre(nombre + " " + apellido);
                        u.setRolTitulo(pick(ROLES, rnd));
                        u.setBio(generarBio(nombre, pick(ROLES, rnd), ciudad));
                        u.setStack(Arrays.asList(stack));
                        u.setEspecialidad(pick(ESPECIALIDADES, rnd));
                        u.setModalidad(pick(MODALIDADES, rnd));
                        u.setSalarioEsperado(pick(SALARIOS, rnd));
                        u.setDisponibilidad(pick(DISPONIBILIDADES, rnd));
                        u.setCorreo(correo);
                        u.setEmailContacto(correo);
                        u.setTelefono(generarTelefono(rnd));
                        u.setPortfolioUrl("https://portfolio." + correo.split("@")[0] + ".dev");
                        u.setLinkedinUrl("https://linkedin.com/in/" + correo.split("@")[0]);
                        u.setPassword(hash);
                        u.setActivo(true);
                        u.setUbicacion(ciudad + ", Colombia");
                        u.setNivelEspanol(rnd.nextInt(4) + 1);
                        u.setNivelIngles(rnd.nextInt(4) + 1);
                        u.setPerfilPublico(rnd.nextDouble() > 0.1);
                        u.setCvExperiencias(generarExperiencias(rnd));
                        u.setHabilidadesBlandas(generarHabilidadesBlandas(rnd));
                        u.setCvBullets(generarBullets(rnd));
                        u.setEmpleosFavoritos(new ArrayList<>());

                        Date fecha = fechaAleatoria(rnd, 730);
                        u.setCreadoEn(fecha);
                        u.setActualizadoEn(fecha);

                        lote.add(u);

                        if (lote.size() == 500) {
                                usuarioRepository.saveAll(lote);
                                lote.clear();
                                System.out.printf("[DataSeeder]   %d / %d usuarios insertados%n",
                                                i + 1, faltan);
                        }
                }

                if (!lote.isEmpty()) {
                        usuarioRepository.saveAll(lote);
                }

                System.out.println("[DataSeeder] Usuarios completados.");
        }

        private void seedEmpresas(String hash, int yaExisten) {
                int faltan = TOTAL_EMPRESAS - yaExisten;
                System.out.printf("[DataSeeder] Insertando %d empresas...%n", faltan);

                List<Empresa> lote = new ArrayList<>(500);
                Random rnd = ThreadLocalRandom.current();
                Set<String> correosUsados = new HashSet<>();

                for (int i = 0; i < faltan; i++) {
                        String nombreEmpresa = generarNombreEmpresa(rnd, i);
                        String slug = nombreEmpresa.toLowerCase()
                                        .replaceAll("[^a-z0-9]", "")
                                        .substring(0, Math.min(20,
                                                        nombreEmpresa.replaceAll("[^a-zA-Z0-9]", "").length()));
                        String correo = garantizarUnico("contacto@" + slug + ".com", correosUsados, i);
                        correosUsados.add(correo);

                        String ciudad = pick(CIUDADES_CO, rnd);
                        String repNombre = pick(NOMBRES, rnd) + " " + pick(APELLIDOS, rnd);

                        Empresa e = new Empresa();
                        e.setTipo("empresa");
                        e.setNombreEmpresa(nombreEmpresa);
                        e.setNombreRepresentante(repNombre);
                        e.setCargoRepresentante(pick(CARGOS, rnd));
                        e.setSector(pick(SECTORES, rnd));
                        e.setTamano(pick(TAMANOS, rnd));
                        e.setSitioWeb("https://www." + slug + ".co");
                        e.setCorreo(correo);
                        e.setTelefono(generarTelefono(rnd));
                        e.setDescripcion(pick(DESCRIPCIONES_EMPRESA, rnd));
                        e.setUbicacion(ciudad + ", Colombia");
                        e.setPassword(hash);
                        e.setActivo(true);

                        Date fecha = fechaAleatoria(rnd, 1095);
                        e.setCreadoEn(fecha);
                        e.setActualizadoEn(fecha);

                        lote.add(e);

                        if (lote.size() == 500) {
                                empresaRepository.saveAll(lote);
                                lote.clear();
                                System.out.printf("[DataSeeder]   %d / %d empresas insertadas%n",
                                                i + 1, faltan);
                        }
                }

                if (!lote.isEmpty()) {
                        empresaRepository.saveAll(lote);
                }

                System.out.println("[DataSeeder] Empresas completadas.");
        }

        private <T> T pick(T[] arr, Random rnd) {
                return arr[rnd.nextInt(arr.length)];
        }

        private String generarCorreo(String nombre, String apellido, int idx, Set<String> usados) {
                String base = (nombre + apellido + idx)
                                .toLowerCase()
                                .replaceAll("[áàä]", "a")
                                .replaceAll("[éèë]", "e")
                                .replaceAll("[íìï]", "i")
                                .replaceAll("[óòö]", "o")
                                .replaceAll("[úùü]", "u")
                                .replaceAll("[ñ]", "n")
                                .replaceAll("[^a-z0-9]", "");
                String[] dominios = { "gmail.com", "outlook.com", "hotmail.com", "yahoo.com", "proton.me" };
                String correo = base + "@" + dominios[idx % dominios.length];
                return garantizarUnico(correo, usados, idx);
        }

        private String garantizarUnico(String correo, Set<String> usados, int idx) {
                if (!usados.contains(correo))
                        return correo;
                int partes = correo.indexOf("@");
                return correo.substring(0, partes) + idx + correo.substring(partes);
        }

        private String generarTelefono(Random rnd) {
                return "+57 3" + (rnd.nextInt(9) + 1) + String.format("%08d", rnd.nextInt(100000000));
        }

        private String generarNombreEmpresa(Random rnd, int idx) {
                String prefijo = EMPRESAS_PREFIJOS[rnd.nextInt(EMPRESAS_PREFIJOS.length)];
                String sufijo = EMPRESAS_SUFIJOS[rnd.nextInt(EMPRESAS_SUFIJOS.length)];
                return prefijo + sufijo + (idx % 50 == 0 ? " " + (idx / 50 + 1) : "");
        }

        private String generarBio(String nombre, String rol, String ciudad) {
                String[] plantillas = {
                                "%s es un profesional apasionado por la tecnología con experiencia en %s, basado en %s.",
                                "Desarrollador con enfoque en %s y años de experiencia construyendo productos digitales en %s.",
                                "%s combina habilidades técnicas y creatividad para entregar soluciones robustas como %s.",
                                "Profesional en %s con experiencia trabajando en equipos ágiles y proyectos de alto impacto en %s.",
                                "%s es un %s comprometido con la calidad del código y las buenas prácticas de desarrollo."
                };
                int idx = Math.abs(nombre.hashCode()) % plantillas.length;
                return String.format(plantillas[idx], nombre, rol, ciudad);
        }

        private List<Usuario.CvExperiencia> generarExperiencias(Random rnd) {
                int cantidad = rnd.nextInt(3) + 1;
                List<Usuario.CvExperiencia> lista = new ArrayList<>();
                String[] cargosExp = { "Junior Developer", "Semi-Senior Developer", "Senior Developer",
                                "Tech Lead", "Analista", "Consultor", "Arquitecto de Software", "QA Engineer" };
                String[] empresasExp = { "Bancolombia", "Rappi", "Mercado Libre", "Globant", "Accenture",
                                "Siesa", "Pragma", "PSL", "Intergrupo", "Tigo", "Claro", "EPM", "Grupo Éxito" };
                String[] periodos = { "2019 - 2020", "2020 - 2021", "2021 - 2022", "2022 - 2023", "2023 - Presente" };

                for (int i = 0; i < cantidad; i++) {
                        Usuario.CvExperiencia exp = new Usuario.CvExperiencia();
                        exp.setCargo(pick(cargosExp, rnd));
                        exp.setEmpresa(pick(empresasExp, rnd));
                        exp.setPeriodo(periodos[Math.min(i, periodos.length - 1)]);
                        exp.setModalidad(pick(MODALIDADES, rnd));
                        exp.setDescripcion("Desarrollo y mantenimiento de funcionalidades clave, " +
                                        "trabajo en equipo ágil y entrega de valor continua.");
                        lista.add(exp);
                }
                return lista;
        }

        private List<String> generarHabilidadesBlandas(Random rnd) {
                String[] todas = { "Trabajo en equipo", "Comunicación asertiva", "Liderazgo",
                                "Resolución de problemas", "Adaptabilidad", "Creatividad", "Pensamiento crítico",
                                "Gestión del tiempo", "Empatía", "Proactividad", "Orientación a resultados" };
                int cantidad = rnd.nextInt(3) + 3;
                List<String> shuffled = new ArrayList<>(Arrays.asList(todas));
                Collections.shuffle(shuffled, rnd);
                return shuffled.subList(0, cantidad);
        }

        private List<String> generarBullets(Random rnd) {
                String[] todos = {
                                "Experiencia con metodologías ágiles (Scrum / Kanban)",
                                "Conocimiento en patrones de diseño y arquitectura limpia",
                                "Familiaridad con CI/CD y control de versiones con Git",
                                "Capacidad para trabajar en equipos distribuidos y remotos",
                                "Orientado a la entrega de valor y mejora continua",
                                "Experiencia en code review y documentación técnica"
                };
                int cantidad = rnd.nextInt(3) + 2;
                List<String> shuffled = new ArrayList<>(Arrays.asList(todos));
                Collections.shuffle(shuffled, rnd);
                return shuffled.subList(0, cantidad);
        }

        private Date fechaAleatoria(Random rnd, int diasAtras) {
                long ahora = System.currentTimeMillis();
                long hace = ahora - (long) rnd.nextInt(diasAtras) * 86400000L;
                return new Date(hace);
        }

        private static final String[] NOMBRES_PROYECTOS = {
                        "AppTracker", "FlowBoard", "NexusAPI", "DataPulse", "CloudSync",
                        "ShopKit", "AuthGuard", "MetricsHub", "DeployBot", "UIForge",
                        "InvoiceAI", "LogStream", "TaskFlow", "PayLink", "CodeReview"
        };

        private static final String[] CATEGORIAS_PROYECTO = {
                        "Productividad", "E-commerce", "Fintech", "SaaS", "DevTools",
                        "Educación", "Salud", "Entretenimiento", "Analytics", "Seguridad"
        };

        private static final String[] TIPOS_PROYECTO = {
                        "Aplicación Web", "API REST", "Aplicación Móvil", "Dashboard",
                        "Microservicio", "CLI Tool", "Librería", "Plataforma SaaS"
        };

        private static final String[] ROLES_PROYECTO = {
                        "Desarrollador principal", "Colaborador", "Tech Lead",
                        "Full Stack Developer", "Backend Developer", "Frontend Developer"
        };

        private static final String[] MENSAJES_MOTIVACION = {
                        "Estoy muy interesado en esta posición. Tengo experiencia directa con el stack requerido.",
                        "He trabajado en proyectos similares y creo que puedo aportar mucho valor al equipo.",
                        "Me llama la atención la cultura de la empresa y el impacto del producto.",
                        "Mi experiencia previa en el sector se alinea perfectamente con lo que buscan.",
                        "Llevo 3 años trabajando con este stack y me encantaría aplicarlo en este reto.",
                        "Es exactamente el tipo de proyecto en el que quiero crecer profesionalmente.",
                        "Conozco bien el dominio del negocio y puedo incorporarme rápidamente.",
                        "He liderado equipos con retos similares y tengo mucho que aportar."
        };

        private static final int MAX_PROYECTOS_POR_USUARIO = 500;
        private static final int MAX_SOLICITUDES_POR_EMPLEO = 300;

        private void seedProyectosParaUsuarios() {
                long yaExisten = proyectoRepository.count();
                if (yaExisten >= MAX_PROYECTOS_POR_USUARIO) {
                        System.out.printf("[DataSeeder] Proyectos ya existen (%d). Saltando.%n", yaExisten);
                        return;
                }

                System.out.println("[DataSeeder] Insertando proyectos para usuarios...");
                Random rnd = ThreadLocalRandom.current();

                List<Usuario> usuarios = usuarioRepository.findAll().stream()
                                .filter(u -> u.getCorreo() != null && !u.getCorreo().endsWith("@devcoast.co"))
                                .limit(200)
                                .collect(java.util.stream.Collectors.toList());

                List<Proyecto> lote = new ArrayList<>(500);

                for (Usuario u : usuarios) {
                        int cantidad = rnd.nextInt(3) + 1; // 1 a 3 proyectos por usuario
                        String[] stack = STACKS[rnd.nextInt(STACKS.length)];
                        for (int i = 0; i < cantidad; i++) {
                                Proyecto p = new Proyecto();
                                p.setUsuarioId(u.getId());
                                p.setUsuarioCorreo(u.getCorreo());
                                String nombre = NOMBRES_PROYECTOS[rnd.nextInt(NOMBRES_PROYECTOS.length)]
                                                + " " + (char) ('A' + rnd.nextInt(26)) + rnd.nextInt(99);
                                p.setNombre(nombre);
                                p.setCategoria(CATEGORIAS_PROYECTO[rnd.nextInt(CATEGORIAS_PROYECTO.length)]);
                                p.setTipo(TIPOS_PROYECTO[rnd.nextInt(TIPOS_PROYECTO.length)]);
                                p.setDescripcionCorta("Plataforma " + nombre
                                                + " con enfoque en automatización y experiencia de usuario.");
                                p.setDescripcionExtendida(
                                                "Proyecto desarrollado de forma independiente con foco en rendimiento y escalabilidad.");
                                p.setAnio(String.valueOf(2022 + rnd.nextInt(3)));
                                p.setStack(Arrays.asList(stack));
                                p.setEtiquetas(Arrays.asList("Open Source", "Portfolio"));
                                p.setRol(ROLES_PROYECTO[rnd.nextInt(ROLES_PROYECTO.length)]);
                                p.setDuracion((rnd.nextInt(6) + 1) + " meses");
                                p.setIconoClave(STACKS[rnd.nextInt(STACKS.length)][0].toLowerCase().replaceAll("[^a-z]",
                                                ""));
                                p.setColorIcono(String.format("#%06x", rnd.nextInt(0xffffff + 1)));
                                p.setPublicado(true);
                                p.setIncluirCv(rnd.nextBoolean());
                                p.setDestacar(i == 0);
                                Date fecha = fechaAleatoria(rnd, 500);
                                p.setCreadoEn(fecha);
                                p.setActualizadoEn(fecha);
                                lote.add(p);
                        }
                        if (lote.size() >= 500) {
                                proyectoRepository.saveAll(lote);
                                lote.clear();
                        }
                }
                if (!lote.isEmpty())
                        proyectoRepository.saveAll(lote);
                System.out.printf("[DataSeeder] Proyectos insertados: %d%n", proyectoRepository.count() - yaExisten);
        }

        private void seedSolicitudesParaUsuarios() {
                long yaExisten = solicitudRepository.count();
                if (yaExisten >= MAX_SOLICITUDES_POR_EMPLEO) {
                        System.out.printf("[DataSeeder] Solicitudes ya existen (%d). Saltando.%n", yaExisten);
                        return;
                }

                System.out.println("[DataSeeder] Insertando solicitudes para usuarios...");
                Random rnd = ThreadLocalRandom.current();

                List<Empleo> empleos = empleoRepository.findAll().stream()
                                .filter(e -> e.getEmpresaCorreo() != null
                                                && !e.getEmpresaCorreo().endsWith("@devcoast.co"))
                                .limit(30)
                                .collect(java.util.stream.Collectors.toList());

                List<Usuario> usuarios = usuarioRepository.findAll().stream()
                                .filter(u -> u.getCorreo() != null && !u.getCorreo().endsWith("@devcoast.co"))
                                .limit(150)
                                .collect(java.util.stream.Collectors.toList());

                if (empleos.isEmpty() || usuarios.isEmpty()) {
                        System.out.println("[DataSeeder] No hay empleos o usuarios suficientes para solicitudes.");
                        return;
                }

                String[] estados = { "revision", "revision", "aprobada", "rechazada" };
                List<Solicitud> lote = new ArrayList<>(200);

                for (Usuario u : usuarios) {
                        int cantidad = rnd.nextInt(3) + 1; // 1 a 3 solicitudes por usuario
                        Set<String> empleosUsados = new HashSet<>();
                        for (int i = 0; i < cantidad; i++) {
                                Empleo emp = empleos.get(rnd.nextInt(empleos.size()));
                                if (empleosUsados.contains(emp.getId()))
                                        continue;
                                empleosUsados.add(emp.getId());

                                Solicitud s = new Solicitud();
                                s.setUsuarioId(u.getId());
                                s.setUsuarioCorreo(u.getCorreo());
                                s.setUsuarioNombre(u.getNombre());
                                s.setUsuarioRolTitulo(u.getRolTitulo());
                                s.setEmpleoId(emp.getId());
                                s.setEmpleoTitulo(emp.getTitulo());
                                s.setEmpleoCategoria(emp.getCategoria());
                                s.setEmpleoSeniority(emp.getSeniority());
                                s.setEmpleoModalidad(emp.getModalidad());
                                s.setEmpleoContrato(emp.getContrato());
                                s.setEmpleoEmpresaNombre(emp.getEmpresaNombre());
                                s.setEmpleoEmpresaSector(emp.getEmpresaSector());
                                s.setEmpleoEmpresaTamano(emp.getEmpresaTamano());
                                s.setEmpleoEmpresaWeb(emp.getEmpresaWeb());
                                s.setEmpleoEmpresaDescripcion(emp.getEmpresaDescripcion());
                                s.setEmpleoIcono(emp.getIcono());
                                s.setEmpleoColorIcono(emp.getColorIcono());
                                s.setEmpleoStack(emp.getStack());
                                s.setEmpleoSoftSkills(emp.getSoftSkills());
                                s.setEmpleoDescripcionRol(emp.getDescripcionRol());
                                s.setEmpleoRequisitosDeseables(emp.getRequisitosDeseables());
                                s.setEmpleoMoneda(emp.getMoneda());
                                s.setEmpleoSalarioMin(emp.getSalarioMin());
                                s.setEmpleoSalarioMax(emp.getSalarioMax());
                                s.setEmpleoSalarioPeriodo(emp.getSalarioPeriodo());
                                s.setEmpleoTimezone(emp.getTimezone());
                                s.setEmpleoIngles(emp.getIngles());
                                s.setEmpleoVacantes(emp.getVacantes());
                                s.setEmpleoMostrarSalario(emp.isMostrarSalario());
                                s.setEstado(estados[rnd.nextInt(estados.length)]);
                                s.setFechaSolicitud(fechaAleatoria(rnd, 180));
                                s.setMensajeMotivacion(MENSAJES_MOTIVACION[rnd.nextInt(MENSAJES_MOTIVACION.length)]);
                                s.setProyectosIds(new ArrayList<>());
                                lote.add(s);
                        }
                        if (lote.size() >= 200) {
                                solicitudRepository.saveAll(lote);
                                lote.clear();
                        }
                }
                if (!lote.isEmpty())
                        solicitudRepository.saveAll(lote);
                System.out.printf("[DataSeeder] Solicitudes insertadas: %d%n", solicitudRepository.count() - yaExisten);
        }

        private void seedDemoCompleto() {
                Usuario existente = usuarioRepository.findByCorreo("demo@devcoast.co");
                if (existente != null) {
                        // Asegurar que la cuenta demo siempre sea visible en talentos
                        if (!existente.isPerfilPublico()) {
                                existente.setPerfilPublico(true);
                                usuarioRepository.save(existente);
                                System.out.println("[DataSeeder] Corregido perfilPublico=true en demo@devcoast.co");
                        }
                        return;
                }

                System.out.println("[DataSeeder] Creando usuario y empresa demo...");

                Empresa empresaDemo = new Empresa();
                empresaDemo.setTipo("empresa");
                empresaDemo.setNombreEmpresa("DevCoast Labs");
                empresaDemo.setNombreRepresentante("Mariana Torres");
                empresaDemo.setCargoRepresentante("CTO");
                empresaDemo.setSector("Tecnología");
                empresaDemo.setTamano("51-200 empleados");
                empresaDemo.setSitioWeb("https://www.devcoastlabs.co");
                empresaDemo.setCorreo("empresa@devcoast.co");
                empresaDemo.setTelefono("+57 310 555 0001");
                empresaDemo.setDescripcion(
                                "Empresa de desarrollo de software especializada en plataformas de talento tech, con operaciones en Colombia y México.");
                empresaDemo.setUbicacion("Medellín, Colombia");
                empresaDemo.setPassword(passwordEncoder.encode("demo1234"));
                empresaDemo.setActivo(true);
                empresaDemo.setCreadoEn(new Date());
                empresaDemo.setActualizadoEn(new Date());
                empresaDemo = empresaRepository.save(empresaDemo);

                Usuario usuarioDemo = new Usuario();
                usuarioDemo.setTipo("talento");
                usuarioDemo.setNombre("Carlos Andrés Ramírez");
                usuarioDemo.setRolTitulo("Full Stack Developer");
                usuarioDemo.setBio(
                                "Desarrollador con 4 años de experiencia en Java y React, apasionado por construir productos digitales de alto impacto.");
                usuarioDemo.setStack(Arrays.asList("Java", "Spring Boot", "React", "MongoDB", "Docker", "Git"));
                usuarioDemo.setEspecialidad("Desarrollo Web");
                usuarioDemo.setModalidad("Híbrido");
                usuarioDemo.setSalarioEsperado("5.000.000 - 8.000.000 COP");
                usuarioDemo.setDisponibilidad("Inmediata");
                usuarioDemo.setCorreo("demo@devcoast.co");
                usuarioDemo.setEmailContacto("demo@devcoast.co");
                usuarioDemo.setTelefono("+57 315 555 0001");
                usuarioDemo.setPortfolioUrl("https://carlosramirez.dev");
                usuarioDemo.setLinkedinUrl("https://linkedin.com/in/carlosandresramirez");
                usuarioDemo.setPassword(passwordEncoder.encode("demo1234"));
                usuarioDemo.setActivo(true);
                usuarioDemo.setPerfilPublico(true);
                usuarioDemo.setUbicacion("Bogotá, Colombia");
                usuarioDemo.setNivelEspanol(4);
                usuarioDemo.setNivelIngles(3);
                usuarioDemo.setHabilidadesBlandas(Arrays.asList("Trabajo en equipo", "Comunicación asertiva",
                                "Resolución de problemas", "Liderazgo", "Adaptabilidad", "Proactividad"));
                usuarioDemo.setCvBullets(Arrays.asList("Experiencia con metodologías ágiles (Scrum / Kanban)",
                                "Conocimiento en patrones de diseño y arquitectura limpia",
                                "Familiaridad con CI/CD y control de versiones con Git",
                                "Capacidad para trabajar en equipos distribuidos y remotos"));

                List<Usuario.CvExperiencia> exps = new ArrayList<>();
                Usuario.CvExperiencia exp1 = new Usuario.CvExperiencia();
                exp1.setCargo("Full Stack Developer");
                exp1.setEmpresa("Pragma S.A.");
                exp1.setPeriodo("2022 - Presente");
                exp1.setModalidad("Remoto");
                exp1.setDescripcion(
                                "Desarrollo de microservicios con Spring Boot y frontends en React para clientes del sector financiero.");
                exps.add(exp1);
                Usuario.CvExperiencia exp2 = new Usuario.CvExperiencia();
                exp2.setCargo("Backend Developer");
                exp2.setEmpresa("Bancolombia");
                exp2.setPeriodo("2020 - 2022");
                exp2.setModalidad("Presencial");
                exp2.setDescripcion(
                                "Desarrollo y mantenimiento de APIs REST para plataformas de banca digital con Java 11 y PostgreSQL.");
                exps.add(exp2);
                usuarioDemo.setCvExperiencias(exps);
                usuarioDemo.setEmpleosFavoritos(new ArrayList<>());
                usuarioDemo.setCreadoEn(new Date());
                usuarioDemo.setActualizadoEn(new Date());
                usuarioDemo = usuarioRepository.save(usuarioDemo);

                String[] pNombres = { "DevTrack — Gestión de tareas", "FinBot — Asistente financiero",
                                "ShopLite — E-commerce" };
                String[] pCats = { "Productividad", "Fintech", "E-commerce" };
                String[] pDescs = { "Plataforma de gestión de tareas con tableros Kanban y reportes en tiempo real.",
                                "Chatbot financiero que analiza gastos y genera recomendaciones de ahorro.",
                                "Tienda online ligera con carrito, pasarela de pago y panel admin." };
                String[][] pStacks = { { "Java", "Spring Boot", "React", "MongoDB" },
                                { "Python", "FastAPI", "React Native", "Firebase" },
                                { "Node.js", "Vue.js", "PostgreSQL", "Stripe" } };
                String[] pIconos = { "layers", "trending-up", "shopping-cart" };
                String[] pColores = { "#1d7fd0", "#10b981", "#f59e0b" };

                List<String> proyectosIds = new ArrayList<>();
                for (int i = 0; i < 3; i++) {
                        Proyecto p = new Proyecto();
                        p.setUsuarioId(usuarioDemo.getId());
                        p.setUsuarioCorreo(usuarioDemo.getCorreo());
                        p.setNombre(pNombres[i]);
                        p.setCategoria(pCats[i]);
                        p.setTipo("Aplicación Web");
                        p.setDescripcionCorta(pDescs[i]);
                        p.setDescripcionExtendida(pDescs[i] + " Desarrollado con enfoque en rendimiento y UX.");
                        p.setAnio(String.valueOf(2023 - i));
                        p.setLinkProyecto("https://github.com/carlosramirez/proj" + i);
                        p.setStack(Arrays.asList(pStacks[i]));
                        p.setEtiquetas(Arrays.asList("Open Source", "Portfolio"));
                        p.setRol("Desarrollador principal");
                        p.setDuracion((i + 2) + " meses");
                        p.setIconoClave(pIconos[i]);
                        p.setColorIcono(pColores[i]);
                        p.setPublicado(true);
                        p.setIncluirCv(true);
                        p.setDestacar(i == 0);
                        p.setCreadoEn(new Date());
                        p.setActualizadoEn(new Date());
                        proyectosIds.add(proyectoRepository.save(p).getId());
                }

                // 5 empleos publicados por la empresa demo
                // solicitudes esperadas por empleo: [24, 10, 3, 7, 0]
                String[] eTitulos = {
                                "Senior Backend Developer",
                                "Frontend Developer React",
                                "DevOps Engineer",
                                "Mobile Developer Android",
                                "Data Engineer"
                };
                String[] eCats = { "Backend", "Frontend", "DevOps", "Mobile", "Data" };
                String[] eSens = { "Senior", "Semi-Senior", "Senior", "Semi-Senior", "Senior" };
                String[] eMods = { "Remoto", "Híbrido", "Remoto", "Remoto", "Híbrido" };
                String[] eConts = { "Indefinido", "Término fijo", "Prestación de servicios", "Indefinido",
                                "Término fijo" };
                int[] eSalMin = { 6000000, 4000000, 7000000, 4500000, 8000000 };
                int[] eSalMax = { 9000000, 6000000, 10000000, 7000000, 12000000 };
                String[] eStacks = {
                                "Java, Spring Boot, Kafka, AWS",
                                "React, TypeScript, Tailwind CSS",
                                "Docker, Kubernetes, Terraform, AWS",
                                "Kotlin, Android, Firebase, Retrofit",
                                "Python, Spark, Airflow, BigQuery"
                };
                String[] eDescs = {
                                "Buscamos Senior Backend para liderar desarrollo de microservicios en nuestra plataforma core.",
                                "Frontend con bases sólidas en React y TypeScript para interfaces de alta calidad.",
                                "DevOps para gestionar infraestructura cloud y pipelines CI/CD.",
                                "Desarrollador Android con experiencia en Kotlin y arquitectura MVVM para nuestra app flagship.",
                                "Data Engineer para construir y mantener pipelines de datos a escala en entorno cloud."
                };
                String[] eIconos = { "cpu", "globe", "terminal", "code", "chart" };
                String[] eColores = { "#1d7fd0", "#8b5cf6", "#10b981", "#f59e0b", "#ef4444" };
                // cuántas solicitudes de usuarios random tendrá cada empleo (el índice 4 = 0,
                // sin solicitudes)
                int[] solicitudesPorEmpleo = { 24, 10, 3, 7, 0 };

                List<Empleo> empleosGuardados = new ArrayList<>();
                for (int i = 0; i < 5; i++) {
                        Empleo e = new Empleo();
                        e.setEmpresaId(empresaDemo.getId());
                        e.setEmpresaNombre(empresaDemo.getNombreEmpresa());
                        e.setEmpresaSector(empresaDemo.getSector());
                        e.setEmpresaWeb(empresaDemo.getSitioWeb());
                        e.setEmpresaTamano(empresaDemo.getTamano());
                        e.setEmpresaDescripcion(empresaDemo.getDescripcion());
                        e.setEmpresaCorreo(empresaDemo.getCorreo());
                        e.setTitulo(eTitulos[i]);
                        e.setCategoria(eCats[i]);
                        e.setSeniority(eSens[i]);
                        e.setModalidad(eMods[i]);
                        e.setContrato(eConts[i]);
                        e.setVacantes(i + 1);
                        e.setMoneda("COP");
                        e.setSalarioMin(eSalMin[i]);
                        e.setSalarioMax(eSalMax[i]);
                        e.setSalarioPeriodo("Mensual");
                        e.setTimezone("América/Bogotá");
                        e.setIngles("Intermedio");
                        e.setDescripcionRol(eDescs[i]);
                        e.setRequisitosDeseables(
                                        "Experiencia ágil, comunicación remota, certificaciones cloud son un plus.");
                        e.setStack(eStacks[i]);
                        e.setSoftSkills("Proactividad, trabajo en equipo, orientación a resultados");
                        e.setMostrarSalario(true);
                        e.setMatchAutomatico(false);
                        e.setDestacar(i == 0);
                        e.setNotificacionesEmail(true);
                        e.setEstado("ACTIVO");
                        // fechas repartidas en los últimos 30 días para que "hace N días" y NUEVA se vean variados
                        e.setFechaPublicacion(new java.util.Date(System.currentTimeMillis() - (long) ((i * 7 + 1) % 30) * 86_400_000L));
                        e.setIcono(eIconos[i]);
                        e.setColorIcono(eColores[i]);
                        empleosGuardados.add(empleoRepository.save(e));
                }

                // ── Solicitudes del usuario DEMO en empleos de la empresa demo (primeros 3) ──
                String[] estadosDemo = { "aprobada", "revision", "rechazada" };
                String[] mensajesDemo = {
                                "Estoy muy interesado en esta posición. Tengo experiencia directa con el stack requerido.",
                                "He trabajado en proyectos similares en el sector financiero, es una oportunidad alineada con mi trayectoria.",
                                "Me llama la atención la cultura remota y el enfoque en automatización de DevCoast Labs."
                };
                for (int i = 0; i < 3; i++) {
                        Empleo emp = empleosGuardados.get(i);
                        Solicitud s = new Solicitud();
                        s.setUsuarioId(usuarioDemo.getId());
                        s.setUsuarioCorreo(usuarioDemo.getCorreo());
                        s.setUsuarioNombre(usuarioDemo.getNombre());
                        s.setUsuarioRolTitulo(usuarioDemo.getRolTitulo());
                        s.setEmpleoId(emp.getId());
                        s.setEmpleoTitulo(emp.getTitulo());
                        s.setEmpleoCategoria(emp.getCategoria());
                        s.setEmpleoSeniority(emp.getSeniority());
                        s.setEmpleoModalidad(emp.getModalidad());
                        s.setEmpleoContrato(emp.getContrato());
                        s.setEmpleoEmpresaNombre(emp.getEmpresaNombre());
                        s.setEmpleoEmpresaSector(emp.getEmpresaSector());
                        s.setEmpleoEmpresaTamano(emp.getEmpresaTamano());
                        s.setEmpleoEmpresaWeb(emp.getEmpresaWeb());
                        s.setEmpleoEmpresaDescripcion(emp.getEmpresaDescripcion());
                        s.setEmpleoIcono(emp.getIcono());
                        s.setEmpleoColorIcono(emp.getColorIcono());
                        s.setEmpleoStack(emp.getStack());
                        s.setEmpleoSoftSkills(emp.getSoftSkills());
                        s.setEmpleoDescripcionRol(emp.getDescripcionRol());
                        s.setEmpleoRequisitosDeseables(emp.getRequisitosDeseables());
                        s.setEmpleoMoneda(emp.getMoneda());
                        s.setEmpleoSalarioMin(emp.getSalarioMin());
                        s.setEmpleoSalarioMax(emp.getSalarioMax());
                        s.setEmpleoSalarioPeriodo(emp.getSalarioPeriodo());
                        s.setEmpleoTimezone(emp.getTimezone());
                        s.setEmpleoIngles(emp.getIngles());
                        s.setEmpleoVacantes(emp.getVacantes());
                        s.setEmpleoMostrarSalario(emp.isMostrarSalario());
                        s.setEstado(estadosDemo[i]);
                        s.setFechaSolicitud(new Date());
                        s.setMensajeMotivacion(mensajesDemo[i]);
                        s.setProyectosIds(proyectosIds.subList(0, i + 1));
                        solicitudRepository.save(s);
                }

                // ── Solicitudes de usuarios ALEATORIOS en los empleos de la empresa demo ──
                {
                        Random rnd = ThreadLocalRandom.current();
                        List<Usuario> candidatos = usuarioRepository.findAll().stream()
                                        .filter(u -> u.getCorreo() != null && !u.getCorreo().endsWith("@devcoast.co"))
                                        .limit(200)
                                        .collect(java.util.stream.Collectors.toList());

                        String[] estadosRnd = { "revision", "revision", "aprobada", "rechazada" };

                        for (int ei = 0; ei < empleosGuardados.size(); ei++) {
                                int cantidad = solicitudesPorEmpleo[ei];
                                if (cantidad == 0 || candidatos.isEmpty())
                                        continue;

                                Empleo emp = empleosGuardados.get(ei);
                                Collections.shuffle(candidatos, rnd);
                                List<Usuario> seleccionados = candidatos.subList(0,
                                                Math.min(cantidad, candidatos.size()));

                                for (Usuario u : seleccionados) {
                                        Solicitud s = new Solicitud();
                                        s.setUsuarioId(u.getId());
                                        s.setUsuarioCorreo(u.getCorreo());
                                        s.setUsuarioNombre(u.getNombre());
                                        s.setUsuarioRolTitulo(u.getRolTitulo());
                                        s.setEmpleoId(emp.getId());
                                        s.setEmpleoTitulo(emp.getTitulo());
                                        s.setEmpleoCategoria(emp.getCategoria());
                                        s.setEmpleoSeniority(emp.getSeniority());
                                        s.setEmpleoModalidad(emp.getModalidad());
                                        s.setEmpleoContrato(emp.getContrato());
                                        s.setEmpleoEmpresaNombre(emp.getEmpresaNombre());
                                        s.setEmpleoEmpresaSector(emp.getEmpresaSector());
                                        s.setEmpleoEmpresaTamano(emp.getEmpresaTamano());
                                        s.setEmpleoEmpresaWeb(emp.getEmpresaWeb());
                                        s.setEmpleoEmpresaDescripcion(emp.getEmpresaDescripcion());
                                        s.setEmpleoIcono(emp.getIcono());
                                        s.setEmpleoColorIcono(emp.getColorIcono());
                                        s.setEmpleoStack(emp.getStack());
                                        s.setEmpleoSoftSkills(emp.getSoftSkills());
                                        s.setEmpleoDescripcionRol(emp.getDescripcionRol());
                                        s.setEmpleoRequisitosDeseables(emp.getRequisitosDeseables());
                                        s.setEmpleoMoneda(emp.getMoneda());
                                        s.setEmpleoSalarioMin(emp.getSalarioMin());
                                        s.setEmpleoSalarioMax(emp.getSalarioMax());
                                        s.setEmpleoSalarioPeriodo(emp.getSalarioPeriodo());
                                        s.setEmpleoTimezone(emp.getTimezone());
                                        s.setEmpleoIngles(emp.getIngles());
                                        s.setEmpleoVacantes(emp.getVacantes());
                                        s.setEmpleoMostrarSalario(emp.isMostrarSalario());
                                        s.setEstado(estadosRnd[rnd.nextInt(estadosRnd.length)]);
                                        s.setFechaSolicitud(fechaAleatoria(rnd, 90));
                                        s.setMensajeMotivacion(
                                                        MENSAJES_MOTIVACION[rnd.nextInt(MENSAJES_MOTIVACION.length)]);
                                        s.setProyectosIds(new ArrayList<>());
                                        solicitudRepository.save(s);
                                }
                        }
                }

                // ── Postulaciones extra del usuario DEMO en empleos de OTRAS empresas ──
                {
                        Random rnd = ThreadLocalRandom.current();
                        List<Empleo> empleosExternos = empleoRepository.findAll().stream()
                                        .filter(e -> e.getEmpresaCorreo() != null
                                                        && !e.getEmpresaCorreo().endsWith("@devcoast.co"))
                                        .limit(100)
                                        .collect(java.util.stream.Collectors.toList());

                        if (!empleosExternos.isEmpty()) {
                                Collections.shuffle(empleosExternos, rnd);
                                // El usuario demo postula a 8 empleos externos con distintos estados
                                String[] estadosExtra = { "revision", "aprobada", "revision", "rechazada", "revision",
                                                "aprobada", "revision", "rechazada" };
                                String[] mensajesExtra = {
                                                "Busco un nuevo reto donde pueda aplicar mis conocimientos en Java y React a gran escala.",
                                                "Tengo experiencia sólida con el stack que mencionan y me motiva el producto que están construyendo.",
                                                "He liderado proyectos end-to-end y me interesa seguir creciendo en un equipo como el de ustedes.",
                                                "Su empresa apareció en mi radar hace tiempo, y esta vacante encaja perfectamente con mi perfil.",
                                                "Llevo 4 años trabajando con Spring Boot y me gustaría aplicar esa experiencia aquí.",
                                                "El enfoque de producto y la tecnología que usan me generan mucho entusiasmo.",
                                                "Creo que puedo aportar valor desde el primer sprint gracias a mi experiencia previa.",
                                                "Me atrae la cultura de trabajo remoto y los desafíos técnicos que implica este rol."
                                };
                                int totalExtra = Math.min(8, empleosExternos.size());
                                for (int i = 0; i < totalExtra; i++) {
                                        Empleo emp = empleosExternos.get(i);
                                        Solicitud s = new Solicitud();
                                        s.setUsuarioId(usuarioDemo.getId());
                                        s.setUsuarioCorreo(usuarioDemo.getCorreo());
                                        s.setUsuarioNombre(usuarioDemo.getNombre());
                                        s.setUsuarioRolTitulo(usuarioDemo.getRolTitulo());
                                        s.setEmpleoId(emp.getId());
                                        s.setEmpleoTitulo(emp.getTitulo());
                                        s.setEmpleoCategoria(emp.getCategoria());
                                        s.setEmpleoSeniority(emp.getSeniority());
                                        s.setEmpleoModalidad(emp.getModalidad());
                                        s.setEmpleoContrato(emp.getContrato());
                                        s.setEmpleoEmpresaNombre(emp.getEmpresaNombre());
                                        s.setEmpleoEmpresaSector(emp.getEmpresaSector());
                                        s.setEmpleoEmpresaTamano(emp.getEmpresaTamano());
                                        s.setEmpleoEmpresaWeb(emp.getEmpresaWeb());
                                        s.setEmpleoEmpresaDescripcion(emp.getEmpresaDescripcion());
                                        s.setEmpleoIcono(emp.getIcono());
                                        s.setEmpleoColorIcono(emp.getColorIcono());
                                        s.setEmpleoStack(emp.getStack());
                                        s.setEmpleoSoftSkills(emp.getSoftSkills());
                                        s.setEmpleoDescripcionRol(emp.getDescripcionRol());
                                        s.setEmpleoRequisitosDeseables(emp.getRequisitosDeseables());
                                        s.setEmpleoMoneda(emp.getMoneda());
                                        s.setEmpleoSalarioMin(emp.getSalarioMin());
                                        s.setEmpleoSalarioMax(emp.getSalarioMax());
                                        s.setEmpleoSalarioPeriodo(emp.getSalarioPeriodo());
                                        s.setEmpleoTimezone(emp.getTimezone());
                                        s.setEmpleoIngles(emp.getIngles());
                                        s.setEmpleoVacantes(emp.getVacantes());
                                        s.setEmpleoMostrarSalario(emp.isMostrarSalario());
                                        s.setEstado(estadosExtra[i]);
                                        s.setFechaSolicitud(fechaAleatoria(rnd, 120));
                                        s.setMensajeMotivacion(mensajesExtra[i]);
                                        s.setProyectosIds(
                                                        proyectosIds.subList(0, rnd.nextInt(proyectosIds.size()) + 1));
                                        solicitudRepository.save(s);
                                }
                        }
                }

                Empleo empInv = empleosGuardados.get(0);
                Invitacion inv = new Invitacion();
                inv.setEmpresaId(empresaDemo.getId());
                inv.setEmpresaNombre(empresaDemo.getNombreEmpresa());
                inv.setEmpresaSector(empresaDemo.getSector());
                inv.setEmpresaWeb(empresaDemo.getSitioWeb());
                inv.setEmpresaDescripcion(empresaDemo.getDescripcion());
                inv.setEmpresaTamano(empresaDemo.getTamano());
                inv.setTalentoId(usuarioDemo.getId());
                inv.setTalentoNombre(usuarioDemo.getNombre());
                inv.setTalentoCorreo(usuarioDemo.getCorreo());
                inv.setEmpleoId(empInv.getId());
                inv.setEmpleoTitulo(empInv.getTitulo());
                inv.setEmpleoModalidad(empInv.getModalidad());
                inv.setEmpleoContrato(empInv.getContrato());
                inv.setEmpleoCategoria(empInv.getCategoria());
                inv.setEmpleoSalarioMin(empInv.getSalarioMin());
                inv.setEmpleoSalarioMax(empInv.getSalarioMax());
                inv.setEmpleoMoneda(empInv.getMoneda());
                inv.setEmpleoMostrarSalario(empInv.isMostrarSalario());
                inv.setMensaje(
                                "Hola Carlos, hemos revisado tu perfil y creemos que encajas perfectamente con el rol de Senior Backend. ¿Tienes disponibilidad esta semana?");
                inv.setEstado("pendiente");
                inv.setFechaEnvio(new Date());
                invitacionRepository.save(inv);

                System.out.println(
                                "[DataSeeder] Demo listo — usuario: demo@devcoast.co / empresa: empresa@devcoast.co (password: demo1234)"
                                                +
                                                " | 5 empleos publicados, solicitudes por empleo: [24, 10, 3, 7, 0] + 8 postulaciones externas del usuario demo");
        }
}
        */