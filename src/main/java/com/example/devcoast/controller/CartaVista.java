package com.example.devcoast.controller;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import lombok.Getter;

@Getter
public class CartaVista {

    public static final int MAX_POR_ZONA = 4;
    public static final int DIAS_ZARPANDO = 3;
    public static final int DIAS_PUERTO = 30;
    private static final long DIA = 86_400_000L;

    public record Item(String id, String titulo, String detalle, Integer match, String areaClave, String estado,
            Date fechaEnvio, Date fechaDecision, String href) {
    }

    public record Barco(String id, String titulo, String detalle, Integer match, String area, boolean aceptada,
            String href, String s1, String s2, String s3, int indice, String aria) {
    }

    public record Zona(String titulo, String subtitulo, String estadoFiltro, int total, List<Barco> barcos) {
        public int extra() {
            return Math.max(0, total - barcos.size());
        }

        public String aria() {
            return subtitulo.substring(0, 1) + subtitulo.substring(1).toLowerCase() + ": " + total;
        }
    }

    private final List<Zona> zonas = new ArrayList<>();
    private int totalEnRuta;
    private String hrefMas = "/solicitudes";
    private String textoVacia = "Todavía no has zarpado. Postúlate a una oferta desde Empleos.";
    private String etiqueta = "Tus solicitudes en ruta";
    private String textoZonaVacia = "Sin solicitudes en esta etapa.";

    public CartaVista conTextos(String hrefMas, String textoVacia) {
        this.hrefMas = hrefMas;
        this.textoVacia = textoVacia;
        return this;
    }

    public CartaVista paraEmpresa() {
        this.etiqueta = "Tus candidatos en ruta";
        this.textoZonaVacia = "Sin postulaciones en esta etapa.";
        return this;
    }

    public boolean isVacia() {
        return totalEnRuta == 0;
    }

    public static CartaVista de(List<Item> items, Date ahora) {
        List<Item> zarpando = new ArrayList<>(), altaMar = new ArrayList<>(), puerto = new ArrayList<>();
        for (Item i : items) {
            if ("revision".equals(i.estado())) {
                if (dias(i.fechaEnvio(), ahora) <= DIAS_ZARPANDO)
                    zarpando.add(i);
                else
                    altaMar.add(i);
            } else if ("aprobada".equals(i.estado())) {
                Date decision = i.fechaDecision() != null ? i.fechaDecision() : i.fechaEnvio();
                if (dias(decision, ahora) <= DIAS_PUERTO)
                    puerto.add(i);
            }
        }
        CartaVista c = new CartaVista();
        int[] indice = { 0 };
        c.zonas.add(zona("ZARPANDO", "ENVIADA", "proceso", zarpando, "en revisión, recién enviada", false, indice));
        c.zonas.add(zona("EN ALTA MAR", "EN REVISIÓN", "proceso", altaMar, "en revisión", false, indice));
        c.zonas.add(zona("PUERTO", "DECISIÓN", "aceptadas", puerto, "aceptada", true, indice));
        c.totalEnRuta = zarpando.size() + altaMar.size() + puerto.size();
        return c;
    }

    private static long dias(Date fecha, Date ahora) {
        if (fecha == null || ahora == null)
            return Long.MAX_VALUE;
        return Math.max(0, (ahora.getTime() - fecha.getTime()) / DIA);
    }

    private static Zona zona(String titulo, String subtitulo, String filtro, List<Item> lista, String estado, boolean aceptada, int[] indice) {
        List<Barco> barcos = new ArrayList<>();
        for (Item s : lista) {
            if (barcos.size() >= MAX_POR_ZONA)
                break;
            String area = "p".equals(s.areaClave()) ? "Programación" : "d".equals(s.areaClave()) ? "Diseño" : "Otras";
            String[] vela = "p".equals(s.areaClave()) ? new String[] { "#FF7B4F", "#EA6A44", "#CC4B27" }
                    : "d".equals(s.areaClave()) ? new String[] { "#2DD4BF", "#25B9A6", "#1F8F82" }
                    : new String[] { "#CDD1DB", "#B0B4C0", "#8A8F9C" };
            String detalle = s.detalle() == null ? "" : s.detalle();
            String aria = s.titulo() + (detalle.isEmpty() ? "" : ", " + detalle) + ", " + estado
                    + (s.match() != null ? ", match " + s.match() + " por ciento" : "") + ", área " + area;
            barcos.add(new Barco(s.id(), s.titulo(), detalle, s.match(), area, aceptada, s.href(),
                    vela[0], vela[1], vela[2], indice[0]++, aria));
        }
        return new Zona(titulo, subtitulo, filtro, lista.size(), barcos);
    }

    public static List<Item> deSolicitudes(List<SolicitudVista> solicitudes) {
        List<Item> items = new ArrayList<>();
        for (SolicitudVista s : solicitudes)
            items.add(new Item(s.getId(), s.getCargo(), s.getEmpresa(), s.getMatch(), s.getAreaClave(), s.getEstado(),
                    s.getFechaEnvio(), s.getFechaDecision(), "/solicitudes/" + s.getId()));
        return items;
    }
}
