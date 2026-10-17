package com.example.devcoast.controller;

import java.util.ArrayList;
import java.util.List;

import lombok.Getter;

/**
 * La carta de navegación del Dashboard: cada solicitud activa es un barquito en la zona de su etapa.
 * Con los estados reales hay dos zonas: EN ALTA MAR (en revisión) y PUERTO (aceptadas).
 * Las solicitudes que no avanzaron no se dibujan. Máximo 4 barcos por zona; el resto se resume en "+N más".
 */
@Getter
public class CartaVista {

    public static final int MAX_POR_ZONA = 4;

    /** Un barquito. Los colores de la vela (s1, s2, s3) dependen del área: naranja programación, teal diseño, gris otras. */
    public record Barco(String id, String cargo, String empresa, Integer match, String area, boolean aceptada,
            String s1, String s2, String s3, int indice, String aria) {
    }

    public record Zona(String titulo, String subtitulo, int total, List<Barco> barcos) {
        public int extra() {
            return Math.max(0, total - barcos.size());
        }

        public String aria() {
            return subtitulo.substring(0, 1) + subtitulo.substring(1).toLowerCase() + ": " + total;
        }
    }

    private final List<Zona> zonas = new ArrayList<>();
    private int totalEnRuta;

    public boolean isVacia() {
        return totalEnRuta == 0;
    }

    public static CartaVista de(List<SolicitudVista> solicitudes) {
        CartaVista c = new CartaVista();
        List<SolicitudVista> revision = new ArrayList<>(), aceptadas = new ArrayList<>();
        for (SolicitudVista s : solicitudes) {
            if ("revision".equals(s.getEstado()))
                revision.add(s);
            else if ("aprobada".equals(s.getEstado()))
                aceptadas.add(s);
        }
        int[] indice = { 0 };
        c.zonas.add(zona("EN ALTA MAR", "EN REVISIÓN", revision, "en revisión", false, indice));
        c.zonas.add(zona("PUERTO", "DECISIÓN", aceptadas, "aceptada", true, indice));
        c.totalEnRuta = revision.size() + aceptadas.size();
        return c;
    }

    private static Zona zona(String titulo, String subtitulo, List<SolicitudVista> lista, String estado, boolean aceptada, int[] indice) {
        List<Barco> barcos = new ArrayList<>();
        for (SolicitudVista s : lista) {
            if (barcos.size() >= MAX_POR_ZONA)
                break;
            String area = "p".equals(s.getAreaClave()) ? "Programación" : "d".equals(s.getAreaClave()) ? "Diseño" : "Otras";
            String[] vela = "p".equals(s.getAreaClave()) ? new String[] { "#FF7B4F", "#EA6A44", "#CC4B27" }
                    : "d".equals(s.getAreaClave()) ? new String[] { "#2DD4BF", "#25B9A6", "#1F8F82" }
                    : new String[] { "#CDD1DB", "#B0B4C0", "#8A8F9C" };
            String empresa = s.getEmpresa() == null ? "" : s.getEmpresa();
            String aria = s.getCargo() + (empresa.isEmpty() ? "" : ", " + empresa) + ", " + estado
                    + (s.getMatch() != null ? ", match " + s.getMatch() + " por ciento" : "") + ", área " + area;
            barcos.add(new Barco(s.getId(), s.getCargo(), empresa, s.getMatch(), area, aceptada,
                    vela[0], vela[1], vela[2], indice[0]++, aria));
        }
        return new Zona(titulo, subtitulo, lista.size(), barcos);
    }
}
