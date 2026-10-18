package com.example.devcoast.controller;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import com.example.devcoast.model.Empleo;
import com.example.devcoast.service.AreaEmpleo;
import com.example.devcoast.service.MatchEmpleo;

import lombok.Getter;

@Getter
public class EmpleoVista {

    public record Habilidad(String nombre, String estado) {
    }

    private String id;
    private String titulo;
    private String empresaNombre;
    private String empresaInicial;
    private String empresaColor;
    private String logoUrl;
    private String areaClave;
    private String categoria;
    private String modalidad;
    private String seniority;
    private String salarioTexto;
    private String salarioEtiqueta;
    private String hace;
    private boolean nueva;
    private String descripcionCorta;
    private Long postulantes;
    private Integer match;
    private String nivel;
    private int cubos;
    private List<Habilidad> habilidades = new ArrayList<>();
    private int habilidadesRestantes;
    private boolean solicitada;
    private boolean cerrada;

    public static EmpleoVista de(Empleo e, MatchEmpleo.Resultado m, boolean solicitada, boolean destacado, Long postulantes,
            Date ahora) {
        EmpleoVista v = new EmpleoVista();
        v.id = e.getId();
        v.titulo = e.getTitulo();
        v.empresaNombre = e.getEmpresaNombre();
        v.empresaInicial = e.getEmpresaNombre() != null && !e.getEmpresaNombre().isBlank()
                ? e.getEmpresaNombre().trim().substring(0, 1).toUpperCase(Locale.ROOT) : null;
        v.empresaColor = e.getColorIcono() != null && e.getColorIcono().matches("#[0-9a-fA-F]{3,8}") ? e.getColorIcono() : null;
        v.logoUrl = e.getLogoUrl() != null && !e.getLogoUrl().isBlank() ? e.getLogoUrl() : null;
        AreaEmpleo.Area area = AreaEmpleo.de(e.getCategoria());
        v.areaClave = area == AreaEmpleo.Area.PROGRAMACION ? "p" : area == AreaEmpleo.Area.DISENO ? "d" : "o";
        v.categoria = e.getCategoriaMostrar();
        v.modalidad = AreaEmpleo.textoVisible(vacioANull(e.getModalidad()));
        v.seniority = AreaEmpleo.textoVisible(vacioANull(e.getSeniority()));
        v.postulantes = postulantes;
        v.solicitada = solicitada;
        v.cerrada = e.getEstado() != null && !"ACTIVO".equalsIgnoreCase(e.getEstado());
        v.descripcionCorta = destacado ? corta(e.getDescripcionRol(), 140) : null;

        Date fecha = e.getFechaPublicacionEfectiva();
        if (fecha != null && ahora != null) {
            long dias = Math.max(0, (ahora.getTime() - fecha.getTime()) / 86_400_000L);
            v.nueva = dias <= 2;
            v.hace = dias == 0 ? "hoy" : dias == 1 ? "hace 1 día" : dias < 30 ? "hace " + dias + " días"
                    : dias < 365 ? "hace " + (dias / 30) + (dias / 30 == 1 ? " mes" : " meses") : "hace más de un año";
        }
        armarSalario(v, e);

        int max = destacado ? 12 : 6;
        List<Habilidad> todas = new ArrayList<>();
        if (m != null) {
            v.match = m.porcentaje();
            v.nivel = m.nivel();
            v.cubos = m.porcentaje() / 10;
            m.tiene().forEach(h -> todas.add(new Habilidad(h, "tiene")));
            m.faltan().forEach(h -> todas.add(new Habilidad(h, "falta")));
        } else {
            MatchEmpleo.habilidadesRequeridas(e.getStack()).forEach(h -> todas.add(new Habilidad(h, "neutra")));
        }
        v.habilidades = new ArrayList<>(todas.subList(0, Math.min(max, todas.size())));
        v.habilidadesRestantes = Math.max(0, todas.size() - max);
        return v;
    }

    private static void armarSalario(EmpleoVista v, Empleo e) {
        if (!e.isMostrarSalario() || (e.getSalarioMin() == null && e.getSalarioMax() == null))
            return;
        int ref = e.getSalarioMax() != null ? e.getSalarioMax() : e.getSalarioMin();
        double divisor = ref >= 1_000_000 ? 1_000_000 : ref >= 1_000 ? 1_000 : 1;
        String sufijo = divisor == 1_000_000 ? " M" : divisor == 1_000 ? " K" : "";
        String a = e.getSalarioMin() != null ? num(e.getSalarioMin() / divisor) : null;
        String b = e.getSalarioMax() != null ? num(e.getSalarioMax() / divisor) : null;
        v.salarioTexto = "$" + (a != null && b != null ? (a.equals(b) ? a : a + "–" + b) : a != null ? "Desde " + a : "Hasta " + b) + sufijo;
        String per = e.getSalarioPeriodo() == null ? "" : e.getSalarioPeriodo().trim().replaceFirst("^/\\s*", "").toLowerCase(Locale.ROOT);
        String perAbrev = per.startsWith("mes") || per.startsWith("mens") ? "MES" : per.startsWith("anu") || per.startsWith("año") ? "AÑO"
                : per.startsWith("hora") ? "HORA" : per.startsWith("seman") ? "SEMANA" : per.isEmpty() ? null : per.toUpperCase(Locale.ROOT);
        String mon = vacioANull(e.getMoneda());
        v.salarioEtiqueta = "SALARIO" + (mon != null || perAbrev != null
                ? " · " + (mon != null ? mon.toUpperCase(Locale.ROOT) : "") + (mon != null && perAbrev != null ? "/" : "") + (perAbrev != null ? perAbrev : "")
                : "");
    }

    private static String num(double d) {
        double r = Math.round(d * 10) / 10.0;
        return r == Math.floor(r) ? String.valueOf((long) r) : String.valueOf(r).replace('.', ',');
    }

    private static String corta(String s, int max) {
        if (s == null)
            return null;
        String t = s.replaceAll("\\s+", " ").trim();
        if (t.isEmpty())
            return null;
        if (t.length() <= max)
            return t;
        int corte = t.lastIndexOf(' ', max);
        return t.substring(0, corte > max / 2 ? corte : max).replaceAll("[\\s,.;:]+$", "") + "…";
    }

    private static String vacioANull(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }
}
