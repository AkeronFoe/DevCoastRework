package com.example.devcoast.model;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import org.junit.jupiter.api.Test;

class UsuarioTest {

    @Test
    void perfilVacioDaCero() {
        Usuario u = new Usuario();
        assertEquals(0, u.getCompletitudPerfil());
    }

    @Test
    void perfilCompletoDaCien() {
        Usuario u = new Usuario();
        u.setNombre("Ana Torres");
        u.setRolTitulo("Backend Developer");
        u.setBio("Me gusta el backend y el café.");
        u.setStack(List.of("Java", "Spring Boot", "MongoDB"));
        u.setEmailContacto("ana@correo.com");
        u.setUbicacion("Bogotá");
        u.setModalidad("remoto");
        u.setPortfolioUrl("https://ana.dev");
        u.setLinkedinUrl("https://linkedin.com/in/ana");
        u.setGithubUrl("https://github.com/ana");
        u.setCvExperiencias(List.of(new Usuario.CvExperiencia()));
        u.setCvEducaciones(List.of(new Usuario.CvEducacion()));

        assertEquals(100, u.getCompletitudPerfil());
    }

    @Test
    void perfilAMedioLlenarDaUnPuntoIntermedio() {
        Usuario u = new Usuario();
        u.setNombre("Luis Pérez");
        u.setRolTitulo("QA");
        u.setBio("Bio corta.");
        u.setStack(List.of("Selenium"));
        u.setEmailContacto("luis@correo.com");
        u.setUbicacion("Lima");

        assertEquals(50, u.getCompletitudPerfil());
    }
}
