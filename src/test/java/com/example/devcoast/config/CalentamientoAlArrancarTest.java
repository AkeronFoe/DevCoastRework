package com.example.devcoast.config;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.bson.Document;
import org.junit.jupiter.api.Test;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import com.example.devcoast.repository.EmpresaRepository;
import com.example.devcoast.repository.UsuarioRepository;
import com.mongodb.client.MongoDatabase;

class CalentamientoAlArrancarTest {

    private final MongoTemplate mongo = mock(MongoTemplate.class);
    private final MongoDatabase db = mock(MongoDatabase.class);
    private final UsuarioRepository usuarios = mock(UsuarioRepository.class);
    private final EmpresaRepository empresas = mock(EmpresaRepository.class);
    private final CalentamientoAlArrancar calentamiento = new CalentamientoAlArrancar(mongo, usuarios, empresas, new BCryptPasswordEncoder(4));

    @Test
    void hacePingYUnaConsultaDeCadaTipoDeCuenta() {
        when(mongo.getDb()).thenReturn(db);

        calentamiento.calentar();

        verify(db).runCommand(any(Document.class));
        verify(usuarios).findByCorreo(any());
        verify(empresas).findByCorreo(any());
    }

    @Test
    void siLaBaseNoResponde_NoTumbaLaApp() {
        when(mongo.getDb()).thenThrow(new RuntimeException("sin conexión"));

        assertDoesNotThrow(calentamiento::calentar);
    }
}
