package com.example.devcoast.config;

import org.bson.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

import com.example.devcoast.repository.EmpresaRepository;
import com.example.devcoast.repository.UsuarioRepository;

// oigan, esto de acá hace una "visita de prueba" a la base apenas arranca la app, para que la primera persona que inicie sesión
// no tenga que esperar a que se abra la conexión con Atlas ni a que se caliente el cifrado de contraseñas
@Component
public class CalentamientoAlArrancar {

    private static final Logger log = LoggerFactory.getLogger(CalentamientoAlArrancar.class);

    private final MongoTemplate mongo;
    private final UsuarioRepository usuarios;
    private final EmpresaRepository empresas;
    private final BCryptPasswordEncoder encoder;

    public CalentamientoAlArrancar(MongoTemplate mongo, UsuarioRepository usuarios, EmpresaRepository empresas,
            BCryptPasswordEncoder encoder) {
        this.mongo = mongo;
        this.usuarios = usuarios;
        this.empresas = empresas;
        this.encoder = encoder;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void alEstarLista() {
        Thread hilo = new Thread(this::calentar, "devcoast-calentamiento");
        hilo.setDaemon(true);
        hilo.start();
    }

    void calentar() {
        long t0 = System.currentTimeMillis();
        try {
            mongo.getDb().runCommand(new Document("ping", 1));
            usuarios.findByCorreo("calentamiento@devcoast.invalid");
            empresas.findByCorreo("calentamiento@devcoast.invalid");
            encoder.matches("calentamiento", encoder.encode("calentamiento"));
            log.info("[Calentamiento] Conexión con la base lista en {} ms", System.currentTimeMillis() - t0);
        } catch (Exception e) {
            log.warn("[Calentamiento] No se pudo calentar la conexión ({} ms): {}", System.currentTimeMillis() - t0, e.toString());
        }
    }
}
