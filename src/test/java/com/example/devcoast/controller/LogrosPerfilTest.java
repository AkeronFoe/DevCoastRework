package com.example.devcoast.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.example.devcoast.model.Usuario;

class LogrosPerfilTest {

    @Test
    void sinNadaSoloEstaMiembro() {
        List<LogrosPerfil.Logro> l = LogrosPerfil.de(new Usuario(), 0, 0, 0);
        assertEquals(12, l.size());
        assertEquals(1, l.stream().filter(LogrosPerfil.Logro::desbloqueado).count());
        assertEquals("Miembro", l.get(0).nombre());
        assertEquals("Escribe tu biografía", l.get(1).progreso());
        assertEquals("0 de 3", l.get(4).progreso());
    }

    @Test
    void sedesbloqueanConLoQueYaTiene() {
        Usuario u = new Usuario();
        u.setBio("Hola");
        u.setLinkedinUrl("https://linkedin.com/in/a");
        u.setGithubUrl("https://github.com/a");
        u.setDisponibilidad("full-time");
        List<LogrosPerfil.Logro> l = LogrosPerfil.de(u, 5, 1, 1);
        assertTrue(l.stream().filter(x -> List.of("Historia propia", "Conectado", "Código público", "Disponible",
                "Portafolio activo", "Portafolio rico", "Estrella", "Radar empresarial", "Preseleccionado").contains(x.nombre()))
                .allMatch(LogrosPerfil.Logro::desbloqueado));
        assertFalse(l.stream().filter(x -> x.nombre().equals("Trayectoria")).findFirst().get().desbloqueado());
        assertEquals("", l.get(1).progreso());
    }
}
