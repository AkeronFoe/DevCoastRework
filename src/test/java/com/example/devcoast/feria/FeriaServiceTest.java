package com.example.devcoast.feria;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.InetSocketAddress;
import java.nio.file.Path;
import java.util.Date;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import com.example.devcoast.model.Cobro;
import com.example.devcoast.model.Empleo;
import com.example.devcoast.model.Empresa;
import com.example.devcoast.model.Invitacion;
import com.example.devcoast.model.Mensaje;
import com.example.devcoast.model.Notificacion;
import com.example.devcoast.model.Proyecto;
import com.example.devcoast.model.Solicitud;
import com.example.devcoast.model.Usuario;
import com.example.devcoast.service.CleanupService;
import com.example.devcoast.service.PlanService;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;

import de.bwaldvogel.mongo.MongoServer;
import de.bwaldvogel.mongo.backend.memory.MemoryBackend;

class FeriaServiceTest {

    private MongoServer server;
    private MongoClient client;
    private MongoTemplate mongo;
    private FeriaService feria;
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder(4);
    private DatosFeria datos;

    @BeforeEach
    void preparar() {
        server = new MongoServer(new MemoryBackend());
        InetSocketAddress addr = server.bind();
        client = MongoClients.create("mongodb://" + addr.getHostString() + ":" + addr.getPort());
        mongo = new MongoTemplate(client, "devcoast_feria_test");
        feria = new FeriaService(mongo, encoder, new CleanupService(mongo), "feria/datos");
        datos = feria.leerDatos();
        assertTrue(datos.valido(), datos.errores.toString());
    }

    @AfterEach
    void cerrar() {
        client.close();
        server.shutdownNow();
    }

    private long cuenta(Class<?> tipo) {
        return mongo.count(new Query(), tipo);
    }

    private Usuario usuario(String correo) {
        return mongo.findOne(Query.query(Criteria.where("correo").is(correo)), Usuario.class);
    }

    private Empresa empresa(String correo) {
        return mongo.findOne(Query.query(Criteria.where("correo").is(correo)), Empresa.class);
    }

    @Test
    void importarCreaLoQueDicenLosArchivos() {
        FeriaService.Resultado r = feria.importar(datos);
        assertEquals(datos.empresas.size(), r.empresas());
        assertEquals(datos.empresas.size(), cuenta(Empresa.class));
        assertEquals(datos.talentos.size(), cuenta(Usuario.class));
        assertEquals(datos.ofertas.size(), cuenta(Empleo.class));
        assertEquals(datos.proyectos.size(), cuenta(Proyecto.class));
    }

    @Test
    void importarDosVecesNoDuplicaNada() {
        feria.importar(datos);
        long e = cuenta(Empresa.class), t = cuenta(Usuario.class), o = cuenta(Empleo.class), p = cuenta(Proyecto.class);
        feria.importar(datos);
        assertEquals(e, cuenta(Empresa.class));
        assertEquals(t, cuenta(Usuario.class));
        assertEquals(o, cuenta(Empleo.class));
        assertEquals(p, cuenta(Proyecto.class));
    }

    @Test
    void importarConservaIdYClaveDeLoQueYaExiste() {
        feria.importar(datos);
        Usuario antes = usuario(FeriaService.DEMO_TALENTO);
        feria.importar(datos);
        Usuario despues = usuario(FeriaService.DEMO_TALENTO);
        assertEquals(antes.getId(), despues.getId());
        assertEquals(antes.getPassword(), despues.getPassword());
    }

    @Test
    void lasCuentasDemoEntranConLaClaveDeLaDemoYLasDemasNoTienenClaveConocida() {
        feria.importar(datos);
        assertTrue(encoder.matches("demo1234", usuario(FeriaService.DEMO_TALENTO).getPassword()));
        assertTrue(encoder.matches("demo1234", empresa(FeriaService.DEMO_EMPRESA).getPassword()));
        Usuario otro = usuario("mateo.gomez@example.com");
        assertNotNull(otro);
        assertFalse(encoder.matches("demo1234", otro.getPassword()));
        assertFalse(encoder.matches("devcoast123", otro.getPassword()));
    }

    @Test
    void elPlanProQuedaVigenteYLosDemasSonGratis() {
        feria.importar(datos);
        Empresa pro = empresa(FeriaService.DEMO_EMPRESA);
        assertEquals(PlanService.PLAN_PRO, pro.getPlan());
        assertTrue(pro.getPlanVenceEn().after(new Date(System.currentTimeMillis() + 30L * 86_400_000L)), "el PRO no debe vencerse durante el evento");
        assertEquals(PlanService.PLAN_GRATIS, empresa(FeriaService.DEMO_STARTUP).getPlan());
    }

    @Test
    void lasOfertasUsanElFormatoQueEsperaLaApp() {
        feria.importar(datos);
        for (Empleo e : mongo.findAll(Empleo.class)) {
            assertNotNull(e.getEmpresaId());
            assertTrue(List.of("Remoto", "Hibrido", "Presencial").contains(e.getModalidad()), e.getModalidad());
            assertFalse(e.getCategoria().contains("ó"), "las categorías se guardan sin tilde como en los formularios: " + e.getCategoria());
            assertFalse(e.getStack().isBlank());
        }
        Empleo cerrada = mongo.findOne(Query.query(Criteria.where("estado").is("CERRADO")), Empleo.class);
        assertNotNull(cerrada);
        assertEquals("CERRADO_PROCESO", cerrada.getFaseReclutamiento());
    }

    @Test
    void reiniciarArmaLaHistoriaDeLasCuentasDemo() {
        feria.reiniciar(datos, false);
        Usuario carlos = usuario(FeriaService.DEMO_TALENTO);
        List<Solicitud> deCarlos = mongo.find(Query.query(Criteria.where("usuarioId").is(carlos.getId())), Solicitud.class);
        assertTrue(deCarlos.size() >= 8, "postulaciones de Carlos: " + deCarlos.size());
        assertTrue(deCarlos.stream().map(Solicitud::getEstado).distinct().count() >= 3, "estados variados");
        assertTrue(deCarlos.stream().anyMatch(s -> "aprobada".equals(s.getEstado())));

        assertEquals(2, mongo.count(Query.query(Criteria.where("talentoId").is(carlos.getId()).and("estado").is("pendiente")), Invitacion.class));
        assertTrue(mongo.count(Query.query(Criteria.where("destinatarioId").is(carlos.getId()).and("leida").is(false)), Notificacion.class) >= 3);

        Solicitud conChat = deCarlos.stream().filter(s -> "aprobada".equals(s.getEstado())
                && mongo.exists(Query.query(Criteria.where("solicitudId").is(s.getId())), Mensaje.class)).findFirst().orElse(null);
        assertNotNull(conChat, "Carlos tiene un chat activo");
        assertTrue(mongo.count(Query.query(Criteria.where("solicitudId").is(conChat.getId()).and("leido").is(false)), Mensaje.class) >= 1);

        long chats = deCarlos.stream().filter(s -> mongo.exists(Query.query(Criteria.where("solicitudId").is(s.getId())), Mensaje.class)).count();
        assertTrue(chats >= 2, "la bandeja de Carlos tiene al menos dos conversaciones: " + chats);

        Empresa pro = empresa(FeriaService.DEMO_EMPRESA);
        long deLaEmpresa = mongo.count(Query.query(Criteria.where("empleoEmpresaNombre").is(pro.getNombreEmpresa())), Solicitud.class);
        assertTrue(deLaEmpresa >= 10, "postulaciones a la empresa PRO: " + deLaEmpresa);
        assertTrue(mongo.count(Query.query(Criteria.where("destinatarioId").is(pro.getId()).and("leida").is(false)), Notificacion.class) >= 3);

        Empresa gratis = empresa(FeriaService.DEMO_STARTUP);
        assertTrue(mongo.count(Query.query(Criteria.where("empresaId").is(gratis.getId())), Cobro.class) >= 1, "la empresa gratis tiene un cobro");
    }

    @Test
    void lasOfertasAjenasALaFeriaNoEntranEnLaHistoria() {
        feria.importar(datos);
        Empresa ajena = new Empresa();
        ajena.setCorreo("ajena@example.com");
        ajena.setNombreEmpresa("Empresa ajena");
        mongo.save(ajena);
        Empleo coincide = new Empleo();
        coincide.setEmpresaId(ajena.getId());
        coincide.setEmpresaNombre("Empresa ajena");
        coincide.setTitulo("Java Spring React perfecto");
        coincide.setEstado("ACTIVO");
        coincide.setStack("Java, Spring Boot, React, TypeScript, PostgreSQL, Docker, Git");
        coincide.setFechaPublicacion(new Date());
        mongo.save(coincide);

        feria.reiniciar(datos, false);

        assertEquals(0, mongo.count(Query.query(Criteria.where("empleoId").is(coincide.getId())), Solicitud.class));
    }

    @Test
    void laMejorOfertaDeLaEmpresaProQuedaSinPostularParaElGuion() {
        feria.reiniciar(datos, false);
        Usuario carlos = usuario(FeriaService.DEMO_TALENTO);
        Empresa pro = empresa(FeriaService.DEMO_EMPRESA);
        List<Empleo> ofertas = mongo.find(Query.query(Criteria.where("empresaId").is(pro.getId()).and("estado").is("ACTIVO")), Empleo.class);
        long sinPostular = ofertas.stream().filter(e -> !mongo.exists(
                Query.query(Criteria.where("usuarioId").is(carlos.getId()).and("empleoId").is(e.getId())), Solicitud.class)).count();
        assertTrue(sinPostular >= 2, "queda al menos una oferta sin postular además de la invitación");
        Empleo reservada = ofertas.stream().filter(e -> e.getTitulo().startsWith("Full Stack Developer (Java")).findFirst().orElseThrow();
        assertNull(mongo.findOne(Query.query(Criteria.where("usuarioId").is(carlos.getId()).and("empleoId").is(reservada.getId())), Solicitud.class));
    }

    @Test
    void reiniciarDosVecesDejaLoMismoSinAcumular() {
        feria.reiniciar(datos, false);
        long s = cuenta(Solicitud.class), i = cuenta(Invitacion.class), m = cuenta(Mensaje.class), n = cuenta(Notificacion.class), c = cuenta(Cobro.class);
        feria.reiniciar(datos, false);
        assertEquals(s, cuenta(Solicitud.class));
        assertEquals(i, cuenta(Invitacion.class));
        assertEquals(m, cuenta(Mensaje.class));
        assertEquals(n, cuenta(Notificacion.class));
        assertEquals(c, cuenta(Cobro.class));
    }

    @Test
    void reiniciarDeshaceLoQueHizoUnVisitanteEnLasCuentasDemo() {
        feria.reiniciar(datos, false);
        Empresa pro = empresa(FeriaService.DEMO_EMPRESA);
        Empleo cerrada = mongo.findOne(Query.query(Criteria.where("empresaId").is(pro.getId()).and("estado").is("ACTIVO")), Empleo.class);
        cerrada.setEstado("CERRADO");
        mongo.save(cerrada);
        Empleo propia = new Empleo();
        propia.setEmpresaId(pro.getId());
        propia.setTitulo("Oferta de un visitante");
        propia.setEstado("ACTIVO");
        mongo.save(propia);
        Usuario carlos = usuario(FeriaService.DEMO_TALENTO);
        carlos.setBio("Bio cambiada por un visitante");
        mongo.save(carlos);
        Proyecto extra = new Proyecto();
        extra.setUsuarioId(carlos.getId());
        extra.setNombre("Proyecto de un visitante");
        mongo.save(extra);
        mongo.remove(Query.query(Criteria.where("usuarioId").is(carlos.getId())), Solicitud.class);

        feria.reiniciar(datos, false);

        assertEquals("ACTIVO", mongo.findById(cerrada.getId(), Empleo.class).getEstado());
        assertNull(mongo.findById(propia.getId(), Empleo.class));
        assertNull(mongo.findById(extra.getId(), Proyecto.class));
        assertFalse(usuario(FeriaService.DEMO_TALENTO).getBio().contains("visitante"));
        assertTrue(mongo.count(Query.query(Criteria.where("usuarioId").is(carlos.getId())), Solicitud.class) >= 8);
    }

    @Test
    void purgarRegistrosBorraSoloLasCuentasQueCrearonLosVisitantes() {
        feria.reiniciar(datos, false);
        Usuario visitante = new Usuario();
        visitante.setTipo("talento");
        visitante.setCorreo("visitante@example.com");
        visitante.setNombre("Visitante");
        visitante.setCreadoEn(new Date(System.currentTimeMillis() + 1000));
        mongo.save(visitante);
        Empresa visitanteEmpresa = new Empresa();
        visitanteEmpresa.setCorreo("empresa.visitante@example.com");
        visitanteEmpresa.setNombreEmpresa("Visitante SAS");
        visitanteEmpresa.setCreadoEn(new Date(System.currentTimeMillis() + 1000));
        mongo.save(visitanteEmpresa);
        long curadosUsuarios = datos.talentos.size();

        feria.reiniciar(datos, true);

        assertNull(usuario("visitante@example.com"));
        assertNull(empresa("empresa.visitante@example.com"));
        assertEquals(curadosUsuarios, cuenta(Usuario.class));
        assertEquals(datos.empresas.size(), cuenta(Empresa.class));
    }

    @Test
    void sinPurgarLasCuentasDeVisitantesSeConservan() {
        feria.reiniciar(datos, false);
        Usuario visitante = new Usuario();
        visitante.setTipo("talento");
        visitante.setCorreo("visitante@example.com");
        visitante.setCreadoEn(new Date(System.currentTimeMillis() + 1000));
        mongo.save(visitante);
        feria.reiniciar(datos, false);
        assertNotNull(usuario("visitante@example.com"));
    }

    @Test
    void conDatosConErroresNoSeImportaNada() {
        DatosFeria malos = DatosFeria.cargar(Path.of("no/existe"));
        assertThrows(IllegalStateException.class, () -> feria.importar(malos));
        assertEquals(0, cuenta(Empresa.class));
    }
}
