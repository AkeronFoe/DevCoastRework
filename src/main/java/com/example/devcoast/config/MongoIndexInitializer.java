package com.example.devcoast.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.index.Index;
import org.springframework.stereotype.Component;

@Component
public class MongoIndexInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(MongoIndexInitializer.class);

    private final MongoTemplate mongoTemplate;

    public MongoIndexInitializer(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    // oigan, esto de acá crea los "atajos" de la base de datos para que las búsquedas no se pongan lentas cuando haya muchísimos datos
    @Override
    public void run(ApplicationArguments args) {
        createIndex("notificaciones", "dest_fecha_idx",
                new Index().named("dest_fecha_idx")
                        .on("destinatario_id", Direction.ASC)
                        .on("fecha_creacion", Direction.DESC));
        createIndex("notificaciones", "dest_leida_idx",
                new Index().named("dest_leida_idx")
                        .on("destinatario_id", Direction.ASC)
                        .on("leida", Direction.ASC));

        createIndex("invitaciones", "talento_fecha_idx",
                new Index().named("talento_fecha_idx")
                        .on("talento_id", Direction.ASC)
                        .on("fecha_envio", Direction.DESC));
        createIndex("invitaciones", "empresa_fecha_idx",
                new Index().named("empresa_fecha_idx")
                        .on("empresa_id", Direction.ASC)
                        .on("fecha_envio", Direction.DESC));
        createIndex("invitaciones", "talento_estado_idx",
                new Index().named("talento_estado_idx")
                        .on("talento_id", Direction.ASC)
                        .on("estado", Direction.ASC));

        createIndex("usuarios", "correo_idx", new Index().named("correo_idx").on("correo", Direction.ASC));
        createIndex("empresas", "correo_idx", new Index().named("correo_idx").on("correo", Direction.ASC));
        createIndex("usuarios", "publico_id_idx",
                new Index().named("publico_id_idx").on("perfil_publico", Direction.ASC).on("_id", Direction.DESC));
        createIndex("empleos", "estado_id_idx",
                new Index().named("estado_id_idx").on("estado", Direction.ASC).on("_id", Direction.DESC));
        createIndex("empleos", "empresa_estado_idx",
                new Index().named("empresa_estado_idx").on("empresaId", Direction.ASC).on("estado", Direction.ASC));
        createIndex("proyectos", "usuario_fecha_idx",
                new Index().named("usuario_fecha_idx").on("usuarioId", Direction.ASC).on("creadoEn", Direction.DESC));
        createIndex("mensajes", "remitente_idx", new Index().named("remitente_idx").on("remitenteId", Direction.ASC));
        createIndex("cobros", "empresa_fecha_idx",
                new Index().named("empresa_fecha_idx").on("empresaId", Direction.ASC).on("fecha", Direction.DESC));
    }

    private void createIndex(String collection, String name, Index index) {
        try {
            mongoTemplate.indexOps(collection).createIndex(index);
        } catch (Exception e) {
            log.warn("No se pudo crear el índice '{}' en la colección '{}': {}", name, collection, e.getMessage());
        }
    }
}
