package com.example.devcoast.feria;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class DatosFeriaTest {

    @TempDir
    Path tmp;

    private static final String EMPRESAS = "correo,nombre,sector,plan,logo\nuno@x.co,Uno,Fintech,GRATIS,\ndos@x.co,Dos,Startup,PRO,\n";

    private void archivo(String nombre, String contenido) throws IOException {
        Files.writeString(tmp.resolve(nombre), contenido, StandardCharsets.UTF_8);
    }

    private static String oferta(String empresa, String titulo, String extra) {
        return empresa + "," + titulo + ",Programación · Backend,Remoto,Java,Descripción larga" + extra + "\n";
    }

    private static final String OFERTAS_CAB = "empresa_correo,titulo,categoria,modalidad,stack,descripcion\n";

    @Test
    void losDatosQueVienenEnElRepositorioSonValidos() {
        DatosFeria d = DatosFeria.cargar(Path.of("feria/datos"));
        assertTrue(d.valido(), "errores: " + d.errores);
        assertTrue(d.empresas.size() >= 8);
        assertTrue(d.ofertas.size() >= 20);
        assertTrue(d.talentos.size() >= 12);
        assertTrue(d.proyectos.size() >= 10);
    }

    @Test
    void laCuentasDemoEstanEnLosDatosDelRepositorio() {
        DatosFeria d = DatosFeria.cargar(Path.of("feria/datos"));
        for (String demo : FeriaService.DEMO_CORREOS) {
            boolean esta = d.empresas.stream().anyMatch(f -> demo.equals(f.get("correo")))
                    || d.talentos.stream().anyMatch(f -> demo.equals(f.get("correo")));
            assertTrue(esta, "falta la cuenta demo " + demo);
        }
    }

    @Test
    void carpetaInexistenteEsUnError() {
        DatosFeria d = DatosFeria.cargar(tmp.resolve("no-existe"));
        assertFalse(d.valido());
    }

    @Test
    void sinEmpresasCsvEsUnError() {
        assertFalse(DatosFeria.cargar(tmp).valido());
    }

    @Test
    void datosMinimosValidos() throws IOException {
        archivo("empresas.csv", EMPRESAS);
        archivo("ofertas.csv", OFERTAS_CAB + oferta("uno@x.co", "Backend", ""));
        DatosFeria d = DatosFeria.cargar(tmp);
        assertTrue(d.valido(), d.errores.toString());
        assertEquals(2, d.empresas.size());
        assertEquals(1, d.ofertas.size());
    }

    @Test
    void ofertaDeUnaEmpresaQueNoExisteSeRechazaConSuArchivoYRegistro() throws IOException {
        archivo("empresas.csv", EMPRESAS);
        archivo("ofertas.csv", OFERTAS_CAB + oferta("nadie@x.co", "Backend", ""));
        DatosFeria d = DatosFeria.cargar(tmp);
        assertFalse(d.valido());
        assertTrue(d.errores.get(0).startsWith("ofertas.csv · registro 2"), d.errores.get(0));
    }

    @Test
    void correoRepetidoPlanInvalidoYCorreoMalFormado() throws IOException {
        archivo("empresas.csv", "correo,nombre,sector,plan\nuno@x.co,Uno,Fintech,GRATIS\nuno@x.co,Otra,Fintech,GRATIS\n"
                + "tres@x.co,Tres,Fintech,VIP\nmal-correo,Cuatro,Fintech,\n");
        DatosFeria d = DatosFeria.cargar(tmp);
        assertEquals(3, d.errores.size(), d.errores.toString());
    }

    @Test
    void elPlanGratisAdmiteSoloDosOfertasActivas() throws IOException {
        archivo("empresas.csv", EMPRESAS);
        archivo("ofertas.csv", OFERTAS_CAB + oferta("uno@x.co", "A", "") + oferta("uno@x.co", "B", "") + oferta("uno@x.co", "C", ""));
        DatosFeria d = DatosFeria.cargar(tmp);
        assertFalse(d.valido());
        assertTrue(d.errores.stream().anyMatch(e -> e.contains("admite 2")), d.errores.toString());
    }

    @Test
    void ofertaDuplicadaYModalidadInvalida() throws IOException {
        archivo("empresas.csv", EMPRESAS);
        archivo("ofertas.csv", OFERTAS_CAB + oferta("dos@x.co", "A", "") + oferta("dos@x.co", "A", "")
                + "dos@x.co,B,Programación · Backend,Volando,Java,Descripción\n");
        DatosFeria d = DatosFeria.cargar(tmp);
        assertEquals(2, d.errores.size(), d.errores.toString());
    }

    @Test
    void sinPermisoElTalentoNoSeImportaPeroNoEsError() throws IOException {
        archivo("empresas.csv", EMPRESAS);
        archivo("talentos.csv", "correo,nombre,rol,especialidad,stack,permiso\nana@x.co,Ana,Dev,backend,Java,si\nbeto@x.co,Beto,Dev,backend,Java,\n");
        archivo("proyectos.csv", "talento_correo,nombre,categoria,descripcion_corta\nbeto@x.co,P1,Backend,algo\nana@x.co,P2,Backend,algo\n");
        DatosFeria d = DatosFeria.cargar(tmp);
        assertTrue(d.valido(), d.errores.toString());
        assertEquals(1, d.talentos.size());
        assertEquals(1, d.proyectos.size(), "el proyecto de quien no dio permiso se omite");
        assertTrue(d.avisos.stream().anyMatch(a -> a.contains("Beto")));
    }

    @Test
    void imagenQueNoExisteOApuntaFueraDeLaCarpetaSeRechaza() throws IOException {
        archivo("empresas.csv", "correo,nombre,sector,logo\nuno@x.co,Uno,Fintech,logos/falta.png\ndos@x.co,Dos,Fintech,../../secreto.png\n");
        DatosFeria d = DatosFeria.cargar(tmp);
        assertEquals(2, d.errores.size(), d.errores.toString());
    }

    @Test
    void especialidadFueraDelCatalogoSeRechaza() throws IOException {
        archivo("empresas.csv", EMPRESAS);
        archivo("talentos.csv", "correo,nombre,rol,especialidad,stack,permiso\nana@x.co,Ana,Dev,astronauta,Java,si\n");
        assertFalse(DatosFeria.cargar(tmp).valido());
    }
}
