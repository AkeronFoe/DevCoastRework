package com.example.devcoast.service;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

import com.example.devcoast.model.Empleo;

class ValidacionEmpleoTest {

    private static Empleo valido() {
        Empleo e = new Empleo();
        e.setTitulo("Backend Java");
        e.setCategoria("Backend");
        e.setSeniority("Senior");
        e.setModalidad("Remoto");
        e.setContrato("Indefinido");
        e.setEmpresaNombre("Costa Tech");
        e.setEmpresaSector("Software");
        e.setEmpresaTamano("11-50");
        e.setEmpresaDescripcion("Equipo de producto");
        e.setStack("Java, Spring");
        e.setVacantes(2);
        e.setSalarioMin(1000);
        e.setSalarioMax(2000);
        return e;
    }

    @Test
    void aceptaUnaOfertaCompleta() {
        assertNull(ValidacionEmpleo.validar(valido()));
    }

    @Test
    void exigeLosCamposObligatorios() {
        Empleo e = valido();
        e.setTitulo("  ");
        assertNotNull(ValidacionEmpleo.validar(e));
    }

    @Test
    void rechazaTextosDemasiadoLargos() {
        Empleo e = valido();
        e.setTitulo("x".repeat(ValidacionEmpleo.MAX_TITULO + 1));
        assertNotNull(ValidacionEmpleo.validar(e));
        e = valido();
        e.setDescripcionRol("x".repeat(ValidacionEmpleo.MAX_DESCRIPCION_ROL + 1));
        assertNotNull(ValidacionEmpleo.validar(e));
    }

    @Test
    void rechazaVacantesYSalariosFueraDeRango() {
        Empleo e = valido();
        e.setVacantes(ValidacionEmpleo.MAX_VACANTES + 1);
        assertNotNull(ValidacionEmpleo.validar(e));
        e = valido();
        e.setVacantes(0);
        assertNotNull(ValidacionEmpleo.validar(e));
        e = valido();
        e.setSalarioMin(-5);
        assertNotNull(ValidacionEmpleo.validar(e));
        e = valido();
        e.setSalarioMin(3000);
        e.setSalarioMax(2000);
        assertNotNull(ValidacionEmpleo.validar(e));
    }
}
