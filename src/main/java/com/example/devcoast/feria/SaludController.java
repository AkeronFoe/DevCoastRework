package com.example.devcoast.feria;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

import org.bson.Document;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.devcoast.model.Empleo;
import com.example.devcoast.model.Empresa;
import com.example.devcoast.model.Usuario;

// GET /salud: responde si la app y la base están vivas y cuántos datos hay. Lo usan el chequeo previo y el hosting. No expone datos personales.
@RestController
public class SaludController {

    private final MongoTemplate mongo;
    private final String perfil;

    public SaludController(MongoTemplate mongo, @Value("${devcoast.perfil:}") String perfil) {
        this.mongo = mongo;
        this.perfil = perfil.isBlank() ? "normal" : perfil;
    }

    @GetMapping("/salud")
    public ResponseEntity<Map<String, Object>> salud() {
        Map<String, Object> r = new LinkedHashMap<>();
        r.put("hora", Instant.now().toString());
        r.put("perfil", perfil);
        try {
            mongo.getDb().runCommand(new Document("ping", 1));
            r.put("mongo", "ok");
            r.put("empresas", mongo.estimatedCount(Empresa.class));
            r.put("talentos", mongo.estimatedCount(Usuario.class));
            r.put("ofertas", mongo.estimatedCount(Empleo.class));
            r.put("estado", "ok");
            return ResponseEntity.ok(r);
        } catch (RuntimeException e) {
            r.put("mongo", "caida");
            r.put("estado", "error");
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(r);
        }
    }
}
