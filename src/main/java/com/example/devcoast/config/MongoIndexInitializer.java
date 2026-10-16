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
    }

    private void createIndex(String collection, String name, Index index) {
        try {
            mongoTemplate.indexOps(collection).createIndex(index);
        } catch (Exception e) {
            log.warn("No se pudo crear el índice '{}' en la colección '{}': {}", name, collection, e.getMessage());
        }
    }
}
