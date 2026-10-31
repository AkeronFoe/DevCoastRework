package com.example.devcoast.config;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;

import com.example.devcoast.model.Cobro;
import com.example.devcoast.model.Empleo;
import com.example.devcoast.model.Empresa;
import com.example.devcoast.model.Invitacion;
import com.example.devcoast.model.Mensaje;
import com.example.devcoast.model.Notificacion;
import com.example.devcoast.model.Proyecto;
import com.example.devcoast.model.Solicitud;
import com.example.devcoast.model.Usuario;

// oigan, esto de acá borra los datos de mentiritas de la versión vieja para que no se mezclen con los nuevos.
// Las cuentas de personas reales ni las toca.
final class LimpiezaSeederAntiguo {

    private static final Logger log = LoggerFactory.getLogger(LimpiezaSeederAntiguo.class);
    private static final int LOTE = 1000;

    private static final List<String> DESCRIPCIONES_EMPRESA_VIEJAS = List.of(
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
            "Empresa de desarrollo de aplicaciones móviles con enfoque en UX.");

    private final MongoTemplate mongo;

    LimpiezaSeederAntiguo(MongoTemplate mongo) {
        this.mongo = mongo;
    }

    Map<String, Long> limpiar() {
        Map<String, Long> borrados = new LinkedHashMap<>();

        List<String> usuarios = ids(mongo.find(usuariosViejos(), Usuario.class).stream().map(Usuario::getId).toList());
        List<String> empresas = ids(mongo.find(empresasViejas(), Empresa.class).stream().map(Empresa::getId).toList());
        if (usuarios.isEmpty() && empresas.isEmpty()) {
            log.info("[DataSeeder] Limpieza: no se encontraron datos del seeder anterior.");
            return borrados;
        }
        log.warn(
                "[DataSeeder] Limpieza: {} talentos y {} empresas del seeder anterior; se borran junto con lo que colgaba de ellos.",
                usuarios.size(), empresas.size());

        List<String> empleos = new ArrayList<>();
        for (List<String> lote : trozos(empresas))
            mongo.find(new Query(Criteria.where("empresaId").in(lote)), Empleo.class)
                    .forEach(e -> empleos.add(e.getId()));

        List<Solicitud> solicitudes = new ArrayList<>();
        for (List<String> lote : trozos(usuarios))
            solicitudes.addAll(mongo.find(new Query(Criteria.where("usuarioId").in(lote)), Solicitud.class));
        for (List<String> lote : trozos(empleos))
            solicitudes.addAll(mongo.find(new Query(Criteria.where("empleoId").in(lote)), Solicitud.class));
        List<String> solicitudIds = ids(solicitudes.stream().map(Solicitud::getId).distinct().toList());
        Set<String> usuariosViejos = Set.copyOf(usuarios);
        List<String> solicitudesDeOtros = solicitudes.stream().filter(s -> !usuariosViejos.contains(s.getUsuarioId()))
                .map(Solicitud::getId).distinct().toList();

        for (List<String> lote : trozos(solicitudIds)) {
            borrados.merge("mensajes",
                    mongo.remove(new Query(Criteria.where("solicitudId").in(lote)), Mensaje.class).getDeletedCount(),
                    Long::sum);
            borrados.merge("solicitudes",
                    mongo.remove(new Query(Criteria.where("id").in(lote)), Solicitud.class).getDeletedCount(),
                    Long::sum);
        }
        for (List<String> lote : trozos(solicitudesDeOtros)) {
            String alternativas = String.join("|", lote.stream().map(Pattern::quote).toList());
            borrados.merge("notificaciones", mongo
                    .remove(new Query(Criteria.where("url").regex(alternativas)), Notificacion.class).getDeletedCount(),
                    Long::sum);
        }
        List<String> destinatarios = new ArrayList<>(usuarios);
        destinatarios.addAll(empresas);
        for (List<String> lote : trozos(destinatarios))
            borrados.merge("notificaciones", mongo
                    .remove(new Query(Criteria.where("destinatarioId").in(lote)), Notificacion.class).getDeletedCount(),
                    Long::sum);

        for (List<String> lote : trozos(usuarios)) {
            borrados.merge("proyectos",
                    mongo.remove(new Query(Criteria.where("usuarioId").in(lote)), Proyecto.class).getDeletedCount(),
                    Long::sum);
            borrados.merge("invitaciones",
                    mongo.remove(new Query(Criteria.where("talentoId").in(lote)), Invitacion.class).getDeletedCount(),
                    Long::sum);
        }
        for (List<String> lote : trozos(empresas)) {
            borrados.merge("invitaciones",
                    mongo.remove(new Query(Criteria.where("empresaId").in(lote)), Invitacion.class).getDeletedCount(),
                    Long::sum);
            borrados.merge("cobros",
                    mongo.remove(new Query(Criteria.where("empresaId").in(lote)), Cobro.class).getDeletedCount(),
                    Long::sum);
            borrados.merge("empleos",
                    mongo.remove(new Query(Criteria.where("empresaId").in(lote)), Empleo.class).getDeletedCount(),
                    Long::sum);
        }
        for (List<String> lote : trozos(empleos))
            mongo.updateMulti(new Query(Criteria.where("empleosFavoritos").in(lote)),
                    new Update().pullAll("empleosFavoritos", lote.toArray()), Usuario.class);

        for (List<String> lote : trozos(usuarios))
            borrados.merge("usuarios",
                    mongo.remove(new Query(Criteria.where("id").in(lote)), Usuario.class).getDeletedCount(), Long::sum);
        for (List<String> lote : trozos(empresas))
            borrados.merge("empresas",
                    mongo.remove(new Query(Criteria.where("id").in(lote)), Empresa.class).getDeletedCount(), Long::sum);

        log.warn("[DataSeeder] Limpieza terminada: {}", borrados);
        return borrados;
    }

    private Query usuariosViejos() {
        Criteria generados = new Criteria().andOperator(
                Criteria.where("portfolioUrl").regex("^https://portfolio\\.[a-z0-9]+\\.dev$"),
                Criteria.where("linkedinUrl").regex("^https://linkedin\\.com/in/[a-z0-9]+$"));
        Criteria demo = new Criteria().andOperator(
                Criteria.where("correo").is("demo@devcoast.co"),
                Criteria.where("portfolioUrl").is("https://carlosramirez.dev"));
        return new Query(new Criteria().orOperator(generados, demo));
    }

    private Query empresasViejas() {
        Criteria generadas = new Criteria().andOperator(
                Criteria.where("descripcion").in(DESCRIPCIONES_EMPRESA_VIEJAS),
                Criteria.where("correo").regex("^contacto[0-9]*@"),
                Criteria.where("sitioWeb").regex("^https://www\\.[a-z0-9]+\\.co$"));
        Criteria demo = new Criteria().andOperator(
                Criteria.where("correo").is("empresa@devcoast.co"),
                Criteria.where("sitioWeb").is("https://www.devcoastlabs.co"));
        return new Query(new Criteria().orOperator(generadas, demo));
    }

    private static List<String> ids(List<String> l) {
        return l.stream().filter(i -> i != null).distinct().toList();
    }

    private static List<List<String>> trozos(List<String> l) {
        List<List<String>> out = new ArrayList<>();
        for (int i = 0; i < l.size(); i += LOTE)
            out.add(l.subList(i, Math.min(l.size(), i + LOTE)));
        return out;
    }
}
