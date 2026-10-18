package com.example.devcoast.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

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
import com.example.devcoast.model.Notificacion;
import com.example.devcoast.model.Proyecto;
import com.example.devcoast.model.Solicitud;
import com.example.devcoast.model.Usuario;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;

import de.bwaldvogel.mongo.MongoServer;
import de.bwaldvogel.mongo.backend.memory.MemoryBackend;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class LimpiezaSeederAntiguoTest {

    private MongoServer server;
    private MongoClient client;
    private MongoTemplate mongo;

    private Usuario viejo;
    private Usuario demoViejo;
    private Usuario real;
    private Empresa empresaVieja;
    private Empresa empresaReal;
    private Empleo ofertaVieja;
    private Empleo ofertaReal;
    private Solicitud realAVieja;
    private Solicitud realAReal;
    private Solicitud viejoAReal;
    private Notificacion avisoRealPorVieja;
    private Notificacion avisoRealOk;

    @BeforeAll
    void prepararYLimpiar() {
        server = new MongoServer(new MemoryBackend());
        InetSocketAddress addr = server.bind();
        client = MongoClients.create("mongodb://" + addr.getHostString() + ":" + addr.getPort());
        mongo = new MongoTemplate(client, "devcoast_limpieza_test");

        viejo = usuario("alejandrogarcia0@gmail.com", "https://portfolio.alejandrogarcia0.dev", "https://linkedin.com/in/alejandrogarcia0");
        demoViejo = usuario("demo@devcoast.co", "https://carlosramirez.dev", "https://linkedin.com/in/carlosandresramirez");
        real = usuario("persona.real@gmail.com", "https://miportafolio.com", "https://www.linkedin.com/in/persona-real");

        empresaVieja = empresa("contacto@techsolutions.com", "https://www.techsolutions.co",
                "Compañía especializada en desarrollo de software a medida para clientes corporativos.");
        empresaReal = empresa("rrhh@miempresa.com", "https://miempresa.com", "Somos una empresa real.");

        ofertaVieja = oferta(empresaVieja);
        ofertaReal = oferta(empresaReal);
        real.setEmpleosFavoritos(new ArrayList<>(List.of(ofertaVieja.getId(), ofertaReal.getId())));
        mongo.save(real);

        mongo.save(proyecto(viejo));
        mongo.save(proyecto(real));
        realAVieja = mongo.save(solicitud(real, ofertaVieja));
        realAReal = mongo.save(solicitud(real, ofertaReal));
        viejoAReal = mongo.save(solicitud(viejo, ofertaReal));
        Mensaje m = new Mensaje();
        m.setSolicitudId(viejoAReal.getId());
        m.setTexto("hola");
        m.setFechaEnvio(new Date());
        mongo.save(m);
        Cobro c = new Cobro();
        c.setEmpresaId(empresaVieja.getId());
        mongo.save(c);
        avisoRealPorVieja = mongo.save(aviso(real.getId(), "/solicitudes/" + realAVieja.getId()));
        avisoRealOk = mongo.save(aviso(real.getId(), "/solicitudes/" + realAReal.getId()));

        new DataSeeder(mongo, new BCryptPasswordEncoder(4), 20, 8, false, false, true).sembrar();
    }

    @AfterAll
    void cerrar() {
        client.close();
        server.shutdownNow();
    }

    @Test
    void borraLosTalentosYEmpresasDelSeederAnteriorYSuDemo() {
        assertNull(mongo.findById(viejo.getId(), Usuario.class));
        assertNull(mongo.findById(empresaVieja.getId(), Empresa.class));
        assertNull(mongo.findById(demoViejo.getId(), Usuario.class), "la demo vieja se reemplaza por la nueva");
    }

    @Test
    void borraTodoLoQueColgabaDeEllos() {
        assertNull(mongo.findById(ofertaVieja.getId(), Empleo.class));
        assertEquals(0, mongo.count(Query.query(Criteria.where("usuarioId").is(viejo.getId())), Proyecto.class));
        assertNull(mongo.findById(viejoAReal.getId(), Solicitud.class));
        assertEquals(0, mongo.count(Query.query(Criteria.where("solicitudId").is(viejoAReal.getId())), Mensaje.class));
        assertEquals(0, mongo.count(Query.query(Criteria.where("empresaId").is(empresaVieja.getId())), Cobro.class));
    }

    @Test
    void laPersonaRealConservaSuCuentaSuEmpresaYSusCosas() {
        assertNotNull(mongo.findById(real.getId(), Usuario.class));
        assertNotNull(mongo.findById(empresaReal.getId(), Empresa.class));
        assertNotNull(mongo.findById(ofertaReal.getId(), Empleo.class));
        assertNotNull(mongo.findById(realAReal.getId(), Solicitud.class));
        assertEquals(1, mongo.count(Query.query(Criteria.where("usuarioId").is(real.getId())), Proyecto.class));
        assertNotNull(mongo.findById(avisoRealOk.getId(), Notificacion.class));
    }

    @Test
    void loQueLaPersonaRealTeniaConLoViejoTambienSeVa() {
        assertNull(mongo.findById(realAVieja.getId(), Solicitud.class));
        assertNull(mongo.findById(avisoRealPorVieja.getId(), Notificacion.class));
        Usuario r = mongo.findById(real.getId(), Usuario.class);
        assertEquals(List.of(ofertaReal.getId()), r.getEmpleosFavoritos(), "el favorito a la oferta borrada se quita");
    }

    @Test
    void despuesSeSiembraLoNuevoIncluidaLaDemoActualizada() {
        Usuario demo = mongo.findOne(Query.query(Criteria.where("correo").is("demo@devcoast.co")), Usuario.class);
        assertNotNull(demo);
        assertFalse(demo.getId().equals(demoViejo.getId()));
        assertEquals("https://carlos.example.com", demo.getPortfolioUrl());
        assertTrue(mongo.count(new Query(), Usuario.class) >= 20 + 2 + 1);
    }

    private Usuario usuario(String correo, String portfolio, String linkedin) {
        Usuario u = new Usuario();
        u.setTipo("talento");
        u.setNombre("Alguien");
        u.setCorreo(correo);
        u.setPortfolioUrl(portfolio);
        u.setLinkedinUrl(linkedin);
        u.setCreadoEn(new Date());
        return mongo.save(u);
    }

    private Empresa empresa(String correo, String web, String descripcion) {
        Empresa e = new Empresa();
        e.setTipo("empresa");
        e.setNombreEmpresa("Empresa " + correo);
        e.setCorreo(correo);
        e.setSitioWeb(web);
        e.setDescripcion(descripcion);
        e.setCreadoEn(new Date());
        return mongo.save(e);
    }

    private Empleo oferta(Empresa e) {
        Empleo o = new Empleo();
        o.setEmpresaId(e.getId());
        o.setEmpresaNombre(e.getNombreEmpresa());
        o.setTitulo("Dev");
        o.setEstado("ACTIVO");
        o.setFechaPublicacion(new Date());
        return mongo.save(o);
    }

    private Proyecto proyecto(Usuario u) {
        Proyecto p = new Proyecto();
        p.setUsuarioId(u.getId());
        p.setNombre("P");
        return mongo.save(p);
    }

    private Solicitud solicitud(Usuario u, Empleo e) {
        Solicitud s = new Solicitud();
        s.setUsuarioId(u.getId());
        s.setEmpleoId(e.getId());
        s.setEstado("revision");
        s.setFechaSolicitud(new Date());
        return s;
    }

    private Notificacion aviso(String destinatario, String url) {
        Notificacion n = new Notificacion();
        n.setDestinatarioId(destinatario);
        n.setUrl(url);
        n.setFechaCreacion(new Date());
        return n;
    }
}
