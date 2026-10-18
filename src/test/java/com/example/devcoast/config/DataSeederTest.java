package com.example.devcoast.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.bson.Document;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import com.example.devcoast.model.Cobro;
import com.example.devcoast.model.Empleo;
import com.example.devcoast.model.Empresa;
import com.example.devcoast.model.Mensaje;
import com.example.devcoast.model.Proyecto;
import com.example.devcoast.model.Solicitud;
import com.example.devcoast.model.Usuario;
import com.example.devcoast.service.EtiquetasPerfil;
import com.example.devcoast.service.PlanService;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;

import de.bwaldvogel.mongo.MongoServer;
import de.bwaldvogel.mongo.backend.memory.MemoryBackend;

/** Siembra una muestra pequeña sobre MongoDB en memoria y comprueba que los datos son coherentes con lo que la app espera. */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class DataSeederTest {

    private static final int TALENTOS = 400;
    private static final int EMPRESAS = 120;

    private MongoServer server;
    private MongoClient client;
    private MongoTemplate mongo;
    private DataSeeder seeder;

    @BeforeAll
    void sembrar() {
        server = new MongoServer(new MemoryBackend());
        InetSocketAddress addr = server.bind();
        client = MongoClients.create("mongodb://" + addr.getHostString() + ":" + addr.getPort());
        mongo = new MongoTemplate(client, "devcoast_seed_test");
        seeder = new DataSeeder(mongo, new BCryptPasswordEncoder(4), TALENTOS, EMPRESAS, false, false);
        seeder.sembrar();
    }

    @AfterAll
    void cerrar() {
        client.close();
        server.shutdownNow();
    }

    @Test
    void creaTodasLasColeccionesConElVolumenPedido() {
        assertEquals(TALENTOS + 2, mongo.count(new Query(), Usuario.class), "talentos + 2 cuentas de demo");
        assertEquals(EMPRESAS + 2, mongo.count(new Query(), Empresa.class), "empresas + 2 cuentas de demo");
        for (Class<?> c : List.of(Empleo.class, Proyecto.class, Solicitud.class, Mensaje.class, Cobro.class))
            assertTrue(mongo.count(new Query(), c) > 0, "debe haber " + c.getSimpleName());
        Document marca = mongo.findById("devcoast-seed", Document.class, "seed_meta");
        assertNotNull(marca);
        assertEquals("completo", marca.getString("estado"));
    }

    @Test
    void losCorreosSonUnicosEntreTalentosYEmpresas() {
        Set<String> vistos = new HashSet<>();
        for (Usuario u : mongo.findAll(Usuario.class))
            assertTrue(vistos.add(u.getCorreo()), "correo repetido: " + u.getCorreo());
        for (Empresa e : mongo.findAll(Empresa.class))
            assertTrue(vistos.add(e.getCorreo()), "correo repetido: " + e.getCorreo());
    }

    @Test
    void losValoresDeLosTalentosSonLosQueEntiendenLosFiltros() {
        Set<String> mod = Set.of("remoto", "hibrido", "presencial");
        Set<String> disp = Set.of("full-time", "part-time", "freelance", "practicas");
        for (Usuario u : mongo.findAll(Usuario.class)) {
            assertTrue(mod.contains(u.getModalidad()), "modalidad: " + u.getModalidad());
            assertTrue(disp.contains(u.getDisponibilidad()), "disponibilidad: " + u.getDisponibilidad());
            assertTrue(EtiquetasPerfil.CLAVES_ESPECIALIDAD.contains(u.getEspecialidad()), "especialidad: " + u.getEspecialidad());
            assertFalse(u.getStack().isEmpty());
            assertTrue(u.getNivelIngles() >= 1 && u.getNivelIngles() <= 5);
        }
    }

    @Test
    void lasOfertasRespetanLosLimitesDelPlanDeSuEmpresa() {
        Map<String, Empresa> empresas = new HashMap<>();
        mongo.findAll(Empresa.class).forEach(e -> empresas.put(e.getId(), e));
        Map<String, Integer> activas = new HashMap<>();
        Map<String, Integer> destacadas = new HashMap<>();
        for (Empleo e : mongo.findAll(Empleo.class)) {
            assertTrue(empresas.containsKey(e.getEmpresaId()), "oferta sin empresa");
            assertTrue(List.of("ACTIVO", "CERRADO").contains(e.getEstado()));
            assertTrue(List.of("ABIERTO", "SCREENING", "ENTREVISTAS", "OFERTA", "CERRADO_PROCESO").contains(e.getFaseReclutamiento()));
            assertTrue(e.getSalarioMin() <= e.getSalarioMax());
            if ("ACTIVO".equals(e.getEstado())) {
                activas.merge(e.getEmpresaId(), 1, Integer::sum);
                if (e.isDestacar())
                    destacadas.merge(e.getEmpresaId(), 1, Integer::sum);
            }
        }
        PlanService planes = new PlanService(null);
        activas.forEach((id, n) -> assertTrue(n <= planes.limiteVacantesActivas(empresas.get(id)), "demasiadas ofertas activas"));
        destacadas.forEach((id, n) -> assertTrue(n <= planes.limiteDestacados(empresas.get(id)), "demasiados destacados"));
    }

    @Test
    void lasSolicitudesSonUnicasYApuntanAOfertasYTalentosReales() {
        Set<String> empleos = new HashSet<>();
        mongo.findAll(Empleo.class).forEach(e -> empleos.add(e.getId()));
        Set<String> usuarios = new HashSet<>();
        mongo.findAll(Usuario.class).forEach(u -> usuarios.add(u.getId()));
        Set<String> pares = new HashSet<>();
        Set<String> estados = Set.of("revision", "aprobada", "rechazada", "descartado");
        for (Solicitud s : mongo.findAll(Solicitud.class)) {
            assertTrue(empleos.contains(s.getEmpleoId()));
            assertTrue(usuarios.contains(s.getUsuarioId()));
            assertTrue(pares.add(s.getUsuarioId() + "|" + s.getEmpleoId()), "solicitud duplicada");
            assertTrue(estados.contains(s.getEstado()));
            assertFalse(s.getHistorial().isEmpty());
            assertEquals("revision", s.getHistorial().get(0).getEstado());
        }
    }

    @Test
    void losMensajesYCobrosSeRefierenASolicitudesExistentes() {
        Set<String> solicitudes = new HashSet<>();
        mongo.findAll(Solicitud.class).forEach(s -> solicitudes.add(s.getId()));
        for (Mensaje m : mongo.findAll(Mensaje.class))
            assertTrue(solicitudes.contains(m.getSolicitudId()));
        for (Cobro c : mongo.findAll(Cobro.class)) {
            assertTrue(solicitudes.contains(c.getSolicitudId()));
            assertEquals(PlanService.TARIFA_COLOCACION, c.getMonto());
        }
    }

    @Test
    void elIdLlevaLaFechaRealDelDocumentoParaQueNadaAparezcaComoNuevo() {
        for (Usuario u : mongo.findAll(Usuario.class)) {
            long diff = Math.abs(new ObjectId(u.getId()).getDate().getTime() - u.getCreadoEn().getTime());
            assertTrue(diff < 1500, "el id debe tener la fecha de creación (" + diff + " ms de diferencia)");
        }
    }

    @Test
    void lasCuentasDeDemoExistenConSuContrasena() {
        BCryptPasswordEncoder enc = new BCryptPasswordEncoder(4);
        for (String correo : List.of("demo@devcoast.co", "diseno@devcoast.co")) {
            Usuario u = mongo.findOne(Query.query(Criteria.where("correo").is(correo)), Usuario.class);
            assertNotNull(u, correo);
            assertTrue(enc.matches("demo1234", u.getPassword()));
            assertTrue(mongo.count(Query.query(Criteria.where("usuarioId").is(u.getId())), Solicitud.class) > 0);
            assertTrue(mongo.count(Query.query(Criteria.where("usuarioId").is(u.getId())), Proyecto.class) > 0);
        }
        Empresa pro = mongo.findOne(Query.query(Criteria.where("correo").is("empresa@devcoast.co")), Empresa.class);
        assertNotNull(pro);
        assertEquals(PlanService.PLAN_PRO, pro.getPlan());
        assertTrue(mongo.count(Query.query(Criteria.where("empresaId").is(pro.getId())), Empleo.class) >= 9);
        Empresa gratis = mongo.findOne(Query.query(Criteria.where("correo").is("startup@devcoast.co")), Empresa.class);
        assertNotNull(gratis);
        assertEquals(PlanService.PLAN_GRATIS, gratis.getPlan());
    }

    @Test
    void sembrarDeNuevoNoDuplicaNada() {
        long usuarios = mongo.count(new Query(), Usuario.class);
        long solicitudes = mongo.count(new Query(), Solicitud.class);
        seeder.sembrar();
        assertEquals(usuarios, mongo.count(new Query(), Usuario.class));
        assertEquals(solicitudes, mongo.count(new Query(), Solicitud.class));
    }
}
