package com.example.devcoast.config;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

// aquí están las listas de nombres, ciudades, stacks y textos que usa el seeder para inventar gente y ofertas creíbles
final class SeedCatalogo {

        private SeedCatalogo() {
        }

        record Arq(String grupo, String clave, String[] roles, String[] stack, String categoria, String icono) {
        }

        private static String[] a(String... v) {
                return v;
        }

        private static Arq arq(String grupo, String clave, String[] roles, String[] stack, String categoria,
                        String icono) {
                return new Arq(grupo, clave, roles, stack, categoria, icono);
        }

        static final List<Arq> ARQUETIPOS = List.of(
                        arq("frontend", "frontend", a("Frontend Developer", "Desarrollador Frontend", "UI Engineer"),
                                        a("React", "TypeScript", "Next.js", "Tailwind CSS", "Vite", "Jest",
                                                        "Storybook"),
                                        "Programacion · Frontend", "globe"),
                        arq("frontend", "frontend", a("Frontend Developer", "Desarrollador Frontend"),
                                        a("Vue.js", "Nuxt", "TypeScript", "Pinia", "Vitest", "Tailwind CSS"),
                                        "Programacion · Frontend", "globe"),
                        arq("frontend", "frontend", a("Frontend Developer", "Angular Developer"),
                                        a("Angular", "TypeScript", "RxJS", "NgRx", "Cypress", "Sass"),
                                        "Programacion · Frontend", "globe"),
                        arq("frontend", "frontend", a("Frontend Engineer", "Web Developer"),
                                        a("Svelte", "SvelteKit", "TypeScript", "Vite", "Playwright", "CSS"),
                                        "Programacion · Frontend", "globe"),
                        arq("backend", "backend", a("Backend Developer", "Desarrollador Backend", "Java Developer"),
                                        a("Java", "Spring Boot", "PostgreSQL", "Kafka", "Docker", "JUnit"),
                                        "Programacion · Backend", "cpu"),
                        arq("backend", "backend", a("Backend Developer", "Node.js Developer"),
                                        a("Node.js", "NestJS", "TypeScript", "PostgreSQL", "Redis", "Docker"),
                                        "Programacion · Backend", "cpu"),
                        arq("backend", "backend", a("Backend Developer", "Python Developer"),
                                        a("Python", "FastAPI", "PostgreSQL", "Redis", "Celery", "Pytest"),
                                        "Programacion · Backend", "cpu"),
                        arq("backend", "backend", a("Backend Engineer", "Go Developer"),
                                        a("Go", "gRPC", "PostgreSQL", "Kubernetes", "Prometheus", "Docker"),
                                        "Programacion · Backend", "cpu"),
                        arq("backend", "backend", a("Backend Developer", ".NET Developer"),
                                        a("C#", ".NET", "SQL Server", "Azure", "Entity Framework", "xUnit"),
                                        "Programacion · Backend", "cpu"),
                        arq("backend", "backend", a("Backend Developer", "Kotlin Developer"),
                                        a("Kotlin", "Spring Boot", "MongoDB", "RabbitMQ", "Docker", "Gradle"),
                                        "Programacion · Backend", "cpu"),
                        arq("backend", "backend", a("Backend Developer", "PHP Developer"),
                                        a("PHP", "Laravel", "MySQL", "Redis", "Docker", "PHPUnit"),
                                        "Programacion · Backend", "cpu"),
                        arq("backend", "backend", a("Backend Engineer", "Rust Developer"),
                                        a("Rust", "Axum", "PostgreSQL", "Docker", "Tokio", "gRPC"),
                                        "Programacion · Backend", "cpu"),
                        arq("fullstack", "fullstack", a("Full Stack Developer", "Desarrollador Full Stack"),
                                        a("React", "Node.js", "TypeScript", "MongoDB", "Docker", "Git"),
                                        "Programacion · Full Stack", "code"),
                        arq("fullstack", "fullstack", a("Full Stack Developer", "Full Stack Engineer"),
                                        a("Next.js", "Prisma", "PostgreSQL", "Tailwind CSS", "TypeScript", "Vercel"),
                                        "Programacion · Full Stack", "code"),
                        arq("fullstack", "fullstack", a("Full Stack Developer", "Desarrollador Full Stack"),
                                        a("Java", "Spring Boot", "React", "MySQL", "Docker", "Git"),
                                        "Programacion · Full Stack", "code"),
                        arq("fullstack", "fullstack", a("Full Stack Developer", "Python Full Stack Developer"),
                                        a("Python", "Django", "React", "PostgreSQL", "Docker", "Redis"),
                                        "Programacion · Full Stack", "code"),
                        arq("fullstack", "fullstack", a("Full Stack Developer", "Laravel Developer"),
                                        a("Vue.js", "Laravel", "MySQL", "Docker", "Tailwind CSS", "Git"),
                                        "Programacion · Full Stack", "code"),
                        arq("mobile", "mobile", a("Mobile Developer", "Flutter Developer"),
                                        a("Flutter", "Dart", "Firebase", "Riverpod", "Git", "Figma"),
                                        "Programacion · Mobile", "terminal"),
                        arq("mobile", "mobile", a("Android Developer", "Mobile Engineer"),
                                        a("Kotlin", "Jetpack Compose", "Android", "Room", "Retrofit", "Firebase"),
                                        "Programacion · Mobile", "terminal"),
                        arq("mobile", "mobile", a("iOS Developer", "Mobile Engineer"),
                                        a("Swift", "SwiftUI", "iOS", "Combine", "Xcode", "Firebase"),
                                        "Programacion · Mobile", "terminal"),
                        arq("mobile", "mobile", a("React Native Developer", "Mobile Developer"),
                                        a("React Native", "TypeScript", "Expo", "Redux", "Firebase", "Jest"),
                                        "Programacion · Mobile", "terminal"),
                        arq("data", "data", a("Data Engineer", "Ingeniero de Datos"),
                                        a("Python", "SQL", "Airflow", "dbt", "Spark", "BigQuery"), "Data / Analytics",
                                        "chart"),
                        arq("data", "data", a("Data Scientist", "Machine Learning Engineer"),
                                        a("Python", "PyTorch", "scikit-learn", "MLflow", "Pandas", "SQL"),
                                        "Data / Analytics", "chart"),
                        arq("data", "data", a("Data Analyst", "Analista de Datos"),
                                        a("SQL", "Power BI", "Excel", "Python", "Looker Studio", "dbt"),
                                        "Data / Analytics", "chart"),
                        arq("data", "data", a("Data Engineer", "Analytics Engineer"),
                                        a("Python", "Databricks", "Delta Lake", "Spark", "Snowflake", "Terraform"),
                                        "Data / Analytics", "chart"),
                        arq("data", "data", a("AI Engineer", "Ingeniero de IA"),
                                        a("Python", "LangChain", "OpenAI API", "FastAPI", "Pinecone", "Docker"),
                                        "Data / Analytics", "chart"),
                        arq("ui-ux", "ui-ux", a("UX/UI Designer", "Diseñador UX/UI"),
                                        a("Figma", "Design Systems", "Prototipado", "User Research", "Accesibilidad",
                                                        "Maze"),
                                        "Diseño · UI/UX", "layers"),
                        arq("ui-ux", "ui-ux", a("Product Designer", "Diseñador de Producto"),
                                        a("Figma", "FigJam", "Design Systems", "UX Writing", "Pruebas de usabilidad",
                                                        "Notion"),
                                        "Diseño · UI/UX", "layers"),
                        arq("ui-ux", "ui-ux", a("UI Designer", "Diseñador de Interfaces"),
                                        a("Figma", "Framer", "Principle", "Lottie", "Tipografía", "Sistemas de color"),
                                        "Diseño · UI/UX", "layers"),
                        arq("diseno-grafico", "diseno-grafico", a("Diseñador Gráfico", "Brand Designer"),
                                        a("Illustrator", "Photoshop", "InDesign", "Branding", "Tipografía", "Figma"),
                                        "Diseño · Grafico", "layers"),
                        arq("diseno-grafico", "diseno-grafico", a("3D Artist", "Artista 3D"),
                                        a("Blender", "Cinema 4D", "Substance Painter", "ZBrush", "After Effects",
                                                        "Unreal Engine"),
                                        "Diseño · 3D", "layers"),
                        arq("diseno-grafico", "diseno-grafico", a("Motion Designer", "Diseñador Motion"),
                                        a("After Effects", "Premiere Pro", "Lottie", "Rive", "Cinema 4D",
                                                        "Illustrator"),
                                        "Diseño · Motion", "layers"),
                        arq("diseno-grafico", "diseno-grafico", a("Ilustrador Digital", "Illustrator"),
                                        a("Procreate", "Illustrator", "Photoshop", "Ilustración", "Color",
                                                        "Storyboard"),
                                        "Diseño · Grafico", "layers"),
                        arq("devops", "backend", a("DevOps Engineer", "Cloud Engineer", "SRE"),
                                        a("AWS", "Terraform", "Kubernetes", "GitHub Actions", "Docker", "Prometheus"),
                                        "DevOps / Infraestructura", "terminal"),
                        arq("devops", "backend", a("DevOps Engineer", "Platform Engineer"),
                                        a("Azure", "Docker", "Kubernetes", "Helm", "ArgoCD", "Grafana"),
                                        "DevOps / Infraestructura", "terminal"),
                        arq("devops", "backend", a("Cloud Engineer", "DevSecOps Engineer"),
                                        a("GCP", "Terraform", "Linux", "GitLab CI", "Docker", "Vault"),
                                        "DevOps / Infraestructura", "terminal"));

        static final List<Arq> SOLO_OFERTAS = List.of(
                        arq("producto", null, a("Product Manager", "Product Owner", "Product Analyst"),
                                        a("Discovery", "Roadmapping", "Jira", "Scrum", "Analítica de producto", "SQL"),
                                        "Producto / UX", "layers"),
                        arq("marketing", null, a("Especialista en Marketing Digital", "Growth Marketer"),
                                        a("Meta Ads", "Google Analytics", "HubSpot", "Email Marketing", "A/B Testing",
                                                        "Looker Studio"),
                                        "Marketing de crecimiento", "chart"),
                        arq("marketing", null, a("Especialista SEO/SEM", "SEO Specialist"),
                                        a("SEO", "Google Ads", "Search Console", "Semrush", "Google Analytics",
                                                        "Copywriting"),
                                        "Marketing SEO/SEM", "chart"),
                        arq("marketing", null, a("Content Manager", "Estratega de Contenido"),
                                        a("Estrategia de contenido", "Redes sociales", "Canva", "Notion", "Analítica",
                                                        "Copywriting"),
                                        "Marketing de contenido", "chart"),
                        arq("escritura", null, a("Redactor Creativo", "UX Writer", "Copywriter"),
                                        a("Copywriting", "UX Writing", "SEO", "Storytelling", "Edición", "Notion"),
                                        "Escritura / Contenido", "layers"));

        static final String[] GRUPOS_TALENTO = { "frontend", "backend", "fullstack", "mobile", "data", "ui-ux",
                        "diseno-grafico", "devops" };
        static final int[] PESO_GRUPOS_TALENTO = { 19, 21, 16, 9, 12, 10, 7, 6 };

        static final Map<String, String[]> GRUPOS_POR_SECTOR = Map.ofEntries(
                        Map.entry("SaaS / Software",
                                        a("frontend", "backend", "backend", "fullstack", "mobile", "data", "devops",
                                                        "ui-ux", "producto")),
                        Map.entry("Fintech",
                                        a("backend", "backend", "fullstack", "mobile", "data", "devops", "frontend",
                                                        "producto")),
                        Map.entry("E-commerce",
                                        a("frontend", "fullstack", "backend", "ui-ux", "data", "marketing", "marketing",
                                                        "mobile")),
                        Map.entry("Agencia creativa",
                                        a("ui-ux", "diseno-grafico", "diseno-grafico", "frontend", "marketing",
                                                        "escritura", "fullstack")),
                        Map.entry("Startup",
                                        a("fullstack", "fullstack", "frontend", "backend", "mobile", "ui-ux", "data",
                                                        "producto", "marketing")),
                        Map.entry("Educacion",
                                        a("fullstack", "frontend", "ui-ux", "data", "escritura", "backend", "mobile")),
                        Map.entry("Salud", a("backend", "mobile", "data", "fullstack", "ui-ux", "devops")),
                        Map.entry("Videojuegos",
                                        a("diseno-grafico", "diseno-grafico", "frontend", "mobile", "backend",
                                                        "ui-ux")),
                        Map.entry("Diseño",
                                        a("ui-ux", "ui-ux", "diseno-grafico", "diseno-grafico", "frontend",
                                                        "producto")),
                        Map.entry("Marketing",
                                        a("marketing", "marketing", "escritura", "data", "diseno-grafico", "frontend")),
                        Map.entry("Otro", a("fullstack", "backend", "data", "frontend", "mobile", "ui-ux")));

        static final String[] SECTORES = { "SaaS / Software", "Fintech", "E-commerce", "Agencia creativa", "Startup",
                        "Educacion", "Salud",
                        "Videojuegos", "Diseño", "Marketing", "Otro" };
        static final int[] PESO_SECTORES = { 24, 14, 12, 9, 17, 6, 5, 4, 3, 3, 3 };

        static final String[] TAMANOS = { "1 – 10 personas", "11 – 50 personas", "51 – 200 personas",
                        "201 – 500 personas", "500+ personas" };
        static final int[] PESO_TAMANOS = { 28, 38, 22, 8, 4 };

        static final String[] SENIORITY = { "Junior (0 – 2 años)", "Mid (2 – 4 años)", "Senior (4+ años)",
                        "Lead / Principal", "Sin restriccion" };
        static final String[] CONTRATOS = { "Tiempo completo", "Medio tiempo", "Freelance / Proyecto",
                        "Contrato por periodo", "Practicas" };
        static final String[] TIMEZONES = { "Latam · UTC-5", "USA · EST / PST", "Europa · CET", "Flexible · +/- 3h" };
        static final String[] INGLES = { "Basico (A1/A2)", "Intermedio (B1/B2)", "Avanzado (C1/C2)" };

        static final String[] EMPRESA_A = { "Andina", "Costa", "Pacífico", "Cumbre", "Tayrona", "Nexo", "Aurora",
                        "Faro", "Marea", "Brújula",
                        "Delta", "Horizonte", "Zafiro", "Cóndor", "Ceiba", "Quimbaya", "Sierra", "Caribe", "Muisca",
                        "Orinoco", "Magdalena", "Cauca",
                        "Atrato", "Nevado", "Páramo", "Colibrí", "Jaguar", "Manglar", "Arrecife", "Bahía", "Isla",
                        "Puerto", "Brisa", "Velero",
                        "Ancla", "Timón", "Coral", "Delfín", "Boreal", "Lumen", "Vértice", "Pulso", "Trama", "Órbita",
                        "Cobalto", "Ámbar", "Alba", "Siena" };
        static final String[] EMPRESA_B = { "Software", "Labs", "Digital", "Studio", "Tech", "Systems", "Cloud", "Data",
                        "Works", "Soluciones",
                        "Interactive", "Creative", "Health", "Pay", "Commerce", "Learning", "Games", "Media",
                        "Analytics", "Ventures" };
        static final String[] EMPRESA_C = { "Colombia", "LATAM", "Andes", "Caribe", "Pacífico", "Group" };
        static final String[] CORREOS_EMPRESA = { "contacto", "talento", "rrhh", "people", "hola", "empleos" };
        static final String[] DOMINIOS_EMPRESA = { ".co", ".com", ".io", ".com.co" };
        static final String[] CARGOS_REP = { "CEO", "CTO", "COO", "Directora de Talento", "Gerente de RRHH",
                        "Head of Engineering",
                        "VP de Tecnología", "Directora de Operaciones", "Fundador", "Cofundadora", "Head of People",
                        "Talent Acquisition Lead" };

        static final Map<String, String[]> DESCRIPCION_EMPRESA = Map.ofEntries(
                        Map.entry("SaaS / Software", a(
                                        "%s desarrolla software como servicio para pymes de Latinoamérica: gestión, facturación y automatización de procesos.",
                                        "%s construye plataformas SaaS con foco en experiencia de usuario y despliegues continuos para clientes en cinco países.",
                                        "En %s creamos productos de software a la medida y herramientas internas que ayudan a equipos a trabajar mejor.")),
                        Map.entry("Fintech", a(
                                        "%s ofrece pagos, cuentas digitales y crédito para personas y pequeños negocios, con operación 100 %% digital.",
                                        "%s es una fintech que simplifica el acceso a servicios financieros con tecnología segura y auditable.",
                                        "En %s construimos infraestructura de pagos y herramientas de educación financiera para millones de usuarios.")),
                        Map.entry("E-commerce", a(
                                        "%s opera un marketplace que conecta a productores y marcas locales con compradores en toda la región.",
                                        "%s es una tienda en línea con logística propia y una plataforma de datos para decidir inventario en tiempo real.",
                                        "En %s ayudamos a marcas a vender en línea: catálogo, pagos, envíos y analítica en un solo lugar.")),
                        Map.entry("Agencia creativa", a(
                                        "%s es una agencia creativa que diseña marcas, sitios y campañas digitales para empresas que quieren destacar.",
                                        "%s combina estrategia, diseño y desarrollo para lanzar productos digitales memorables.",
                                        "En %s hacemos identidad visual, motion y experiencias web para marcas de turismo, gastronomía y tecnología.")),
                        Map.entry("Startup", a(
                                        "%s es una startup en etapa de crecimiento que está construyendo su primer producto con un equipo pequeño y muy autónomo.",
                                        "%s acaba de cerrar su ronda semilla y busca perfiles que quieran moldear el producto desde cero.",
                                        "En %s probamos rápido, medimos todo y construimos en público; aquí tu trabajo llega a usuarios cada semana.")),
                        Map.entry("Educacion", a(
                                        "%s ofrece cursos en línea y rutas de aprendizaje en tecnología para estudiantes y empresas de habla hispana.",
                                        "%s desarrolla plataformas de educación digital con contenidos interactivos y evaluación continua.",
                                        "En %s hacemos que aprender sea más fácil: aulas virtuales, mentorías y analítica del progreso de cada estudiante.")),
                        Map.entry("Salud", a(
                                        "%s conecta pacientes con profesionales de la salud mediante telemedicina, agenda y seguimiento digital.",
                                        "%s desarrolla software clínico y herramientas de monitoreo para clínicas y laboratorios.",
                                        "En %s usamos datos y tecnología para que la atención médica llegue a más personas, también en zonas rurales.")),
                        Map.entry("Videojuegos", a(
                                        "%s es un estudio de videojuegos que crea experiencias móviles y de PC con identidad latinoamericana.",
                                        "%s desarrolla juegos y herramientas de realidad aumentada para entretenimiento y educación.",
                                        "En %s hacemos arte, código y sonido para mundos interactivos que se juegan en todo el planeta.")),
                        Map.entry("Diseño", a(
                                        "%s es un estudio de diseño de producto especializado en sistemas de diseño e interfaces para equipos de software.",
                                        "%s ayuda a empresas a ordenar su experiencia digital con investigación, prototipos y diseño de interfaces.",
                                        "En %s diseñamos productos digitales claros, accesibles y bonitos, de la investigación al handoff.")),
                        Map.entry("Marketing", a(
                                        "%s es una agencia de marketing digital orientada a resultados: performance, contenido y analítica.",
                                        "%s gestiona campañas, SEO y estrategia de contenido para marcas de comercio electrónico.",
                                        "En %s medimos cada peso invertido: pauta, crecimiento y automatización de marketing para negocios en expansión.")),
                        Map.entry("Otro", a(
                                        "%s digitaliza procesos de empresas tradicionales con software a la medida y analítica de datos.",
                                        "%s ofrece soluciones tecnológicas para logística, agroindustria y servicios con operación en la región.",
                                        "En %s trabajamos con equipos multidisciplinarios para transformar operaciones con tecnología.")));

        static final Map<String, String[]> PRODUCTO_POR_SECTOR = Map.ofEntries(
                        Map.entry("SaaS / Software",
                                        a("una plataforma de gestión para pymes",
                                                        "un CRM sencillo para equipos comerciales",
                                                        "herramientas de automatización de procesos")),
                        Map.entry("Fintech",
                                        a("una app de pagos y cuentas digitales", "un motor de crédito en línea",
                                                        "una billetera para pequeños comercios")),
                        Map.entry("E-commerce",
                                        a("un marketplace de productos locales", "la tienda en línea y su logística",
                                                        "una plataforma de ventas para marcas")),
                        Map.entry("Agencia creativa", a("campañas y piezas para marcas de la región",
                                        "sitios web y experiencias interactivas", "identidades visuales completas")),
                        Map.entry("Startup",
                                        a("nuestro primer producto, en etapa de validación",
                                                        "una app que ya tiene usuarios activos",
                                                        "el MVP de una idea con tracción inicial")),
                        Map.entry("Educacion", a("un aula virtual con contenidos interactivos",
                                        "rutas de aprendizaje personalizadas", "una plataforma de cursos en línea")),
                        Map.entry("Salud",
                                        a("una app de telemedicina", "un sistema de historias clínicas digitales",
                                                        "una plataforma de seguimiento de pacientes")),
                        Map.entry("Videojuegos",
                                        a("un juego móvil con millones de descargas", "un título para PC en producción",
                                                        "herramientas de arte y animación")),
                        Map.entry("Diseño",
                                        a("sistemas de diseño para varios clientes", "interfaces de productos SaaS",
                                                        "prototipos y pruebas de usabilidad")),
                        Map.entry("Marketing",
                                        a("campañas de performance para e-commerce", "estrategias de contenido y SEO",
                                                        "paneles de analítica para clientes")),
                        Map.entry("Otro",
                                        a("herramientas internas para operaciones",
                                                        "software de logística y trazabilidad",
                                                        "tableros de analítica para la gerencia")));

        static final String[] REQUISITOS_DESEABLES = {
                        "Experiencia previa en equipos ágiles, buena comunicación escrita y ganas de aprender. Un plus: haber trabajado con integración y despliegue continuos.",
                        "Valoramos que hayas participado en code reviews, que documentes lo que construyes y que tengas portafolio o proyectos propios para mostrar.",
                        "Es un plus tener experiencia en productos con usuarios reales, pruebas automatizadas y conocimientos básicos de seguridad.",
                        "Nos ayuda que hables inglés técnico, que hayas trabajado en remoto y que te guste compartir lo que sabes con el equipo.",
                        "Se valora experiencia en startups o equipos pequeños, autonomía para priorizar y criterio para proponer mejoras al producto." };

        static final String[] SOFT_SKILLS = { "Comunicación asertiva", "Trabajo en equipo", "Autonomía",
                        "Resolución de problemas",
                        "Orientación a resultados", "Adaptabilidad", "Pensamiento crítico", "Gestión del tiempo",
                        "Empatía", "Proactividad",
                        "Liderazgo", "Creatividad", "Colaboración remota" };

        static final String[] BENEFICIOS = { "horario flexible", "bono por desempeño", "días libres adicionales",
                        "presupuesto anual de formación", "equipo de trabajo incluido", "medicina prepagada",
                        "día libre de cumpleaños",
                        "trabajo remoto desde cualquier ciudad" };

        static final String[] COLORES = { "#FF7B4F", "#2DD4BF", "#CDD1DB", "#CC4B27", "#1F8F82", "#8B5CF6", "#F59E0B",
                        "#3B82F6", "#EC4899", "#10B981" };

        static final String[] NOMBRES_F = { "Camila", "Valentina", "Sofía", "Mariana", "Daniela", "Laura", "Natalia",
                        "Juliana", "Paula", "Andrea",
                        "Carolina", "Isabela", "Luisa", "Manuela", "Sara", "Gabriela", "Alejandra", "Catalina", "María",
                        "Ana", "Lucía", "Valeria",
                        "Verónica", "Karen", "Lina", "Diana", "Tatiana", "Sandra", "Ximena", "Estefanía", "Jimena",
                        "Renata", "Antonia", "Fernanda" };
        static final String[] NOMBRES_M = { "Santiago", "Sebastián", "Mateo", "Juan", "Andrés", "Daniel", "David",
                        "Nicolás", "Camilo",
                        "Felipe", "Alejandro", "Carlos", "Diego", "Jorge", "Luis", "Miguel", "Julián", "Esteban",
                        "Samuel", "Tomás", "Emilio",
                        "Gabriel", "Mauricio", "Cristian", "Johan", "Iván", "Óscar", "Ricardo", "Rafael", "Sergio",
                        "Brayan", "Kevin", "Leonardo", "Martín" };
        static final String[] APELLIDOS = { "García", "Rodríguez", "Martínez", "López", "González", "Hernández",
                        "Pérez", "Sánchez", "Ramírez",
                        "Torres", "Flores", "Díaz", "Vargas", "Castro", "Rojas", "Moreno", "Jiménez", "Ruiz",
                        "Gutiérrez", "Ortiz", "Mendoza",
                        "Silva", "Cardona", "Restrepo", "Ospina", "Gómez", "Rincón", "Osorio", "Zapata", "Arango",
                        "Bedoya", "Valencia", "Cortés",
                        "Montoya", "Quintero", "Ríos", "Salazar", "Acosta", "Mejía", "Pineda", "Navarro", "Herrera",
                        "Cano", "Bermúdez", "Guerrero",
                        "Rueda", "Duarte", "Ibarra", "Parra", "Agudelo", "Londoño", "Lozano", "Pardo", "Cárdenas",
                        "Beltrán", "Cifuentes", "Barrios", "Medina" };
        static final String[] DOMINIOS_CORREO = { "gmail.com", "outlook.com", "hotmail.com", "yahoo.com", "proton.me",
                        "icloud.com" };
        static final int[] PESO_DOMINIOS = { 55, 15, 10, 8, 5, 7 };

        static final String[] CIUDADES = { "Bogotá, Colombia", "Medellín, Colombia", "Cali, Colombia",
                        "Barranquilla, Colombia",
                        "Cartagena, Colombia", "Bucaramanga, Colombia", "Pereira, Colombia", "Manizales, Colombia",
                        "Santa Marta, Colombia",
                        "Cúcuta, Colombia", "Ibagué, Colombia", "Armenia, Colombia", "Montería, Colombia",
                        "Villavicencio, Colombia",
                        "Neiva, Colombia", "Pasto, Colombia", "Valledupar, Colombia", "Sincelejo, Colombia",
                        "Ciudad de México, México",
                        "Lima, Perú", "Buenos Aires, Argentina", "Santiago, Chile", "Montevideo, Uruguay",
                        "Quito, Ecuador", "Panamá, Panamá" };
        static final int[] PESO_CIUDADES = { 22, 16, 9, 8, 7, 4, 3, 2, 3, 2, 2, 2, 2, 2, 1, 1, 1, 1, 3, 3, 2, 2, 1, 1,
                        1 };

        static final String[] EXTRAS_STACK = { "Git", "GitHub", "Docker", "Jira", "Scrum", "REST", "GraphQL", "Linux",
                        "CI/CD", "Testing", "Figma", "Notion" };

        static final String[] HABILIDADES_BLANDAS = { "Trabajo en equipo", "Comunicación asertiva", "Liderazgo",
                        "Resolución de problemas",
                        "Adaptabilidad", "Creatividad", "Pensamiento crítico", "Gestión del tiempo", "Empatía",
                        "Proactividad",
                        "Orientación a resultados", "Aprendizaje continuo", "Colaboración remota", "Mentoría" };

        static final String[] CV_BULLETS = {
                        "Experiencia con metodologías ágiles (Scrum / Kanban)",
                        "Conocimiento en patrones de diseño y arquitectura limpia",
                        "Familiaridad con CI/CD y control de versiones con Git",
                        "Capacidad para trabajar en equipos distribuidos y remotos",
                        "Orientado a la entrega de valor y mejora continua",
                        "Experiencia en code review y documentación técnica",
                        "Comunicación clara con perfiles técnicos y de negocio",
                        "Autonomía para estimar, priorizar y entregar" };

        static final String[] EMPRESAS_CV = { "Andina Software", "Mercatto", "Pixelcoop", "Nube Sur", "Cobalto Labs",
                        "Tienda Local",
                        "Bahía Digital", "Alianza Tech", "Grupo Cumbre", "Estudio Faro", "Datos del Valle",
                        "Plataforma Ágora", "Costa Pay",
                        "Orbita Studio", "Ruta Verde", "Kiwi Apps", "Taller Creativo", "Lumen Health" };

        static final Map<String, String[]> TITULOS_ESTUDIO = Map.of(
                        "frontend",
                        a("Ingeniería de Sistemas", "Ingeniería de Software", "Tecnología en Desarrollo de Software"),
                        "backend", a("Ingeniería de Sistemas", "Ingeniería de Software", "Ingeniería Informática"),
                        "fullstack",
                        a("Ingeniería de Sistemas", "Ingeniería de Software",
                                        "Tecnología en Análisis y Desarrollo de Software"),
                        "mobile", a("Ingeniería de Sistemas", "Ingeniería de Software", "Ingeniería Electrónica"),
                        "data", a("Estadística", "Ingeniería de Sistemas", "Ciencia de Datos", "Matemáticas Aplicadas"),
                        "ui-ux",
                        a("Diseño Gráfico", "Diseño Industrial", "Diseño de Medios Interactivos",
                                        "Comunicación Social"),
                        "diseno-grafico", a("Diseño Gráfico", "Diseño Digital", "Artes Visuales", "Animación Digital"),
                        "devops",
                        a("Ingeniería de Sistemas", "Ingeniería de Telecomunicaciones", "Ingeniería Informática"));
        static final String[] UNIVERSIDADES = { "Universidad Nacional", "Universidad de Antioquia",
                        "Universidad del Norte", "Universidad del Valle",
                        "Universidad Javeriana", "Universidad Industrial de Santander", "Universidad EAFIT",
                        "Politécnico Grancolombiano", "SENA",
                        "Universidad Tecnológica de Pereira", "Universidad del Magdalena", "Universidad de Cartagena" };

        static final Map<String, String[]> ENFOQUE = Map.of(
                        "frontend",
                        a("interfaces rápidas y accesibles", "aplicaciones web con muy buena experiencia de usuario",
                                        "productos web escalables y bien probados"),
                        "backend",
                        a("APIs y servicios confiables", "sistemas de alto tráfico y baja latencia",
                                        "backends mantenibles y bien documentados"),
                        "fullstack",
                        a("productos web de punta a punta", "MVPs que después escalan",
                                        "aplicaciones completas, de la base de datos a la interfaz"),
                        "mobile",
                        a("apps móviles fluidas y estables", "experiencias móviles nativas y multiplataforma",
                                        "aplicaciones que la gente usa todos los días"),
                        "data",
                        a("pipelines de datos que llegan a producción", "analítica que ayuda a decidir mejor",
                                        "soluciones de IA aplicadas a problemas reales de negocio"),
                        "ui-ux",
                        a("experiencias digitales centradas en las personas",
                                        "sistemas de diseño y prototipos funcionales",
                                        "interfaces claras con investigación detrás"),
                        "diseno-grafico",
                        a("identidades visuales con carácter", "ilustración, motion y piezas 3D",
                                        "diseño que cuenta historias y vende"),
                        "devops",
                        a("infraestructura como código y pipelines confiables",
                                        "plataformas cloud seguras y observables",
                                        "automatización que ahorra horas al equipo"));

        static final Map<String, String[]> LOGROS = Map.of(
                        "frontend",
                        a("Migré una aplicación a un diseño responsive y mejoré los tiempos de carga en más de un 40 %.",
                                        "Lideré la creación de una librería de componentes reutilizables usada por tres equipos.",
                                        "Implementé pruebas end-to-end que redujeron los errores en producción."),
                        "backend",
                        a("Diseñé APIs REST documentadas con OpenAPI y cobertura de pruebas superior al 80 %.",
                                        "Reduje la latencia de los endpoints críticos optimizando consultas e índices.",
                                        "Migré un monolito a servicios independientes con despliegue automatizado."),
                        "fullstack",
                        a("Construí desde cero un producto con su frontend, API y base de datos, hoy con usuarios activos.",
                                        "Automaticé el despliegue continuo y acorté el ciclo de entrega de semanas a días.",
                                        "Acompañé al equipo de producto en la definición y entrega de nuevas funcionalidades."),
                        "mobile",
                        a("Publiqué y mantuve aplicaciones en Google Play y App Store con calificación superior a 4,5.",
                                        "Reduje los cierres inesperados de la app con monitoreo y pruebas automáticas.",
                                        "Implementé notificaciones push y modo sin conexión para mejorar la retención."),
                        "data",
                        a("Construí pipelines de datos que consolidan varias fuentes y alimentan los tableros de la gerencia.",
                                        "Entrené y desplegué modelos predictivos que mejoraron la conversión de campañas.",
                                        "Documenté métricas y definiciones para que todos los equipos hablen el mismo idioma."),
                        "ui-ux", a("Diseñé y documenté un sistema de diseño adoptado por varios equipos de producto.",
                                        "Hice pruebas de usabilidad que mejoraron la conversión del flujo de registro.",
                                        "Lideré el rediseño de una aplicación con foco en accesibilidad."),
                        "diseno-grafico",
                        a("Desarrollé identidades visuales completas para marcas de turismo y gastronomía.",
                                        "Produje piezas de motion y animaciones 3D para campañas digitales.",
                                        "Coordiné con desarrollo la entrega de recursos gráficos optimizados."),
                        "devops",
                        a("Automaticé la infraestructura con Terraform y reduje el tiempo de aprovisionamiento a minutos.",
                                        "Implementé observabilidad con métricas, logs y alertas que bajaron el tiempo de respuesta a incidentes.",
                                        "Construí pipelines de CI/CD con despliegues sin tiempo de inactividad."));

        static final String[] BIOS = {
                        "%1$s con %2$s de experiencia enfocado en %3$s. Trabajo sobre todo con %4$s y %5$s, y me importa entregar código claro, probado y fácil de mantener.",
                        "Soy %1$s en %6$s. Me dedico a %3$s y disfruto los equipos pequeños donde el producto se decide rápido y se mide con usuarios reales.",
                        "Llevo %2$s trabajando en %3$s. Mi stack de cabecera: %4$s, %5$s y %7$s. Abierto a retos con impacto real y equipos que compartan lo que saben.",
                        "%1$s con pasión por %3$s. He trabajado con equipos ágiles en Latinoamérica y busco un lugar donde seguir creciendo con %4$s." };

        static final String[] MENSAJES_MOTIVACION = {
                        "Me interesa mucho el rol de %1$s en %2$s. He trabajado con %3$s y creo que puedo aportar desde el primer sprint.",
                        "Hola, vi la vacante de %1$s y encaja con lo que vengo haciendo con %3$s. Me encantaría conversar con el equipo de %2$s.",
                        "Llevo tiempo siguiendo a %2$s y esta posición de %1$s es justo el reto que busco. Mi experiencia con %3$s se alinea con lo que piden.",
                        "Quiero postularme a %1$s. Tengo proyectos con %3$s en mi portafolio y me motiva el producto que construyen en %2$s.",
                        "Me motiva la cultura de %2$s y el enfoque de este rol de %1$s. Trabajo a diario con %3$s y aprendo rápido lo que falta.",
                        "Mi experiencia reciente con %3$s se ajusta bien a la vacante de %1$s. Quedo atento a una conversación con %2$s." };

        static final String[] MODALIDADES_JOB = { "Remoto", "Hibrido", "Presencial" };
        static final String[] MODALIDADES_TALENTO = { "remoto", "hibrido", "presencial" };
        static final String[] DISPONIBILIDADES = { "full-time", "part-time", "freelance", "practicas" };

        static final String[] CATEGORIAS_PROYECTO_PROG = { "Frontend", "Backend", "Full Stack", "App movil",
                        "Dashboard SaaS" };
        static final Map<String, String[]> CATEGORIAS_PROYECTO = Map.of(
                        "frontend", a("Frontend", "Full Stack", "Dashboard SaaS", "Frontend"),
                        "backend", a("Backend", "Full Stack", "Otro", "Backend"),
                        "fullstack", a("Full Stack", "Frontend", "Backend", "Full Stack"),
                        "mobile", a("App movil", "App movil", "Full Stack"),
                        "data", a("Dashboard SaaS", "Backend", "Otro"),
                        "ui-ux", a("UI/UX Design", "Design System", "Dashboard SaaS", "App movil", "UI/UX Design"),
                        "diseno-grafico", a("Branding", "Ilustraciones 3D", "Motion / Animacion", "Branding"),
                        "devops", a("Backend", "Otro", "Backend"));
        static final String[] TIPOS_PROYECTO = { "Personal", "Cliente", "Open source", "Concepto" };
        static final int[] PESO_TIPOS_PROYECTO = { 40, 25, 20, 15 };
        static final String[] DURACIONES = { "Menos de 1 semana", "1 – 4 semanas", "1 – 3 meses", "3 – 6 meses",
                        "6+ meses", "En curso" };
        static final String[] ROLES_PROYECTO_PROG = { "Desarrollador principal", "Desarrollador principal",
                        "Colaborador", "Freelance" };
        static final String[] ROLES_PROYECTO_DISENO = { "Diseñador principal", "Diseñador principal",
                        "Lead / Director creativo", "Colaborador", "Freelance" };
        static final String[] COVERS_DISENO = { "cover-sketch", "cover-artboard", "cover-mesh", "cover-orbit",
                        "cover-pixel", "cover-signal",
                        "cover-void", "cover-mint-glow", "cover-amber-glow" };
        static final String[] ICONOS_PROYECTO = { "layers", "code", "terminal", "globe", "cpu", "chart",
                        "shopping-cart", "trending-up" };
        static final String[] ETIQUETAS_PROYECTO = { "Open Source", "Portfolio", "Producción", "MVP", "Accesibilidad",
                        "Rendimiento", "Mobile first",
                        "Caso de estudio", "Premiado", "En vivo" };

        static final String[] DOMINIOS_PROYECTO = { "pedidos de restaurantes", "reservas de canchas deportivas",
                        "gestión de inventario",
                        "seguimiento de hábitos", "un marketplace de artesanías", "telemedicina rural",
                        "turismo en el Caribe",
                        "educación financiera", "una librería independiente", "una cafetería de especialidad",
                        "mantenimiento de flotas",
                        "agendamiento de citas", "reciclaje comunitario", "movilidad en bicicleta", "cultivos de café",
                        "eventos y boletería",
                        "bienestar y meditación", "adopción de mascotas", "logística de última milla",
                        "aprendizaje de idiomas" };
        static final String[] PRODUCTOS_PROYECTO_PROG = { "App", "Panel", "API", "Plataforma", "Portal", "Servicio",
                        "Herramienta", "Sistema" };
        static final String[] PRODUCTOS_PROYECTO_DISENO = { "Rediseño", "Identidad visual", "Sistema de diseño",
                        "Campaña", "Piezas", "Concepto", "Kit gráfico" };
        static final String[] NOMBRES_MARCA = { "Brisa", "Arrecife", "Ceiba", "Nómada", "Faro", "Trama", "Colibrí",
                        "Savia", "Marea", "Tinta",
                        "Cacao", "Orilla", "Ruta", "Pulso", "Selva", "Mango", "Cumbia", "Alba", "Nube", "Sendero",
                        "Lumbre", "Ágora", "Piloto", "Cometa" };

        static final String[] MENSAJES_CHAT_EMPRESA = {
                        "Hola %1$s, gracias por postularte a %2$s. Revisamos tu perfil y nos gustó mucho; queremos conversar contigo.",
                        "Perfecto, ¿te sirve esta semana en la tarde? Podemos hacer una videollamada de 30 minutos.",
                        "Genial, te escribo con el enlace. Vas a hablar con el equipo de producto y con quien sería tu líder técnico.",
                        "Solo te pedimos que tengas a la mano un proyecto de tu portafolio para contarnos cómo lo resolviste.",
                        "Quedamos atentos. ¡Gracias por el tiempo!" };
        static final String[] MENSAJES_CHAT_TALENTO = {
                        "¡Hola! Muchas gracias por escribirme. Con gusto, quedo muy atento a los siguientes pasos.",
                        "Sí, me sirve. Tengo disponibilidad martes y jueves después de las 3:00 p. m.",
                        "Listo, ahí estaré. ¿Hay algo que deba preparar de antemano?",
                        "Claro que sí, llevo el proyecto del portafolio con una demo corta. ¡Hasta entonces!" };

        static List<String> lista(String... v) {
                return Arrays.asList(v);
        }
}
