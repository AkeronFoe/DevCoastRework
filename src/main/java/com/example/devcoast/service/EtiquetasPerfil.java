package com.example.devcoast.service;

import java.text.Normalizer;
import java.util.Map;

import com.example.devcoast.service.AreaEmpleo.Area;

/**
 * Textos y filtros de los campos del perfil del talento (modalidad, disponibilidad, especialidad).
 * El registro y el onboarding guardan claves en minúscula ("remoto", "full-time", "ui-ux") y Mi perfil
 * antes guardaba textos con mayúscula ("Remoto", "Full-time"); aquí se unifica al mostrar y al filtrar
 * sin tocar lo que ya está guardado.
 */
public final class EtiquetasPerfil {

    private EtiquetasPerfil() {
    }

    private static final Map<String, String> ESPECIALIDADES = Map.of(
            "frontend", "Frontend", "backend", "Backend", "fullstack", "Full Stack", "ui-ux", "UI / UX",
            "diseno-grafico", "Diseño gráfico", "data", "Data / Analytics", "mobile", "Mobile");

    /** Claves que guarda el registro, en el orden del formulario. */
    public static final java.util.List<String> CLAVES_ESPECIALIDAD =
            java.util.List.of("frontend", "backend", "fullstack", "ui-ux", "diseno-grafico", "data", "mobile");

    private static String base(String v) {
        if (v == null)
            return "";
        return Normalizer.normalize(v, Normalizer.Form.NFD).replaceAll("\\p{M}+", "").toLowerCase().trim();
    }

    // ───────── modalidad ─────────
    /** remoto | hibrido | presencial, o "" si no es una de las tres (p. ej. "freelance" o vacío). */
    public static String modalidadClave(String v) {
        String b = base(v);
        return b.equals("remoto") || b.equals("hibrido") || b.equals("presencial") ? b : "";
    }

    public static String modalidad(String v) {
        if (v == null || v.isBlank())
            return null;
        return switch (modalidadClave(v)) {
            case "remoto" -> "Remoto";
            case "hibrido" -> "Híbrido";
            case "presencial" -> "Presencial";
            default -> capitalizar(v.trim());
        };
    }

    /** Expresión regular (sin distinguir mayúsculas) con las variantes guardadas de esa modalidad; null si la clave no vale. */
    public static String modalidadRegex(String clave) {
        return switch (base(clave)) {
            case "remoto" -> "^remoto$";
            case "hibrido" -> "^h[ií]brido$";
            case "presencial" -> "^presencial$";
            default -> null;
        };
    }

    // ───────── disponibilidad ─────────
    /** full-time | part-time | freelance | practicas, o "". */
    public static String disponibilidadClave(String v) {
        String b = base(v).replace(' ', '-');
        return switch (b) {
            case "full-time", "fulltime", "tiempo-completo" -> "full-time";
            case "part-time", "parttime", "medio-tiempo" -> "part-time";
            case "freelance" -> "freelance";
            case "practica", "practicas" -> "practicas";
            default -> "";
        };
    }

    public static String disponibilidad(String v) {
        if (v == null || v.isBlank())
            return null;
        return switch (disponibilidadClave(v)) {
            case "full-time" -> "Tiempo completo";
            case "part-time" -> "Medio tiempo";
            case "freelance" -> "Freelance";
            case "practicas" -> "Prácticas";
            default -> capitalizar(v.trim());
        };
    }

    public static String disponibilidadRegex(String clave) {
        return switch (disponibilidadClave(clave)) {
            case "full-time" -> "^(full[ -]?time|tiempo completo)$";
            case "part-time" -> "^(part[ -]?time|medio tiempo)$";
            case "freelance" -> "^freelance$";
            case "practicas" -> "^pr[aá]cticas?$";
            default -> null;
        };
    }

    // ───────── especialidad ─────────
    /** "ui-ux" → "UI / UX"; un texto libre queda tal cual. */
    public static String especialidad(String v) {
        if (v == null || v.isBlank())
            return null;
        String t = ESPECIALIDADES.get(base(v));
        return t != null ? t : v.trim();
    }

    /** Clave del registro a la que corresponde el valor guardado, o "" si es un texto libre. */
    public static String especialidadClave(String v) {
        String b = base(v);
        if (ESPECIALIDADES.containsKey(b))
            return b;
        for (Map.Entry<String, String> e : ESPECIALIDADES.entrySet())
            if (base(e.getValue()).equals(b))
                return e.getKey();
        return "";
    }

    /** Área para el icono y el color de la tarjeta: programación, diseño u otras. */
    public static Area area(String especialidad) {
        return switch (especialidadClave(especialidad)) {
            case "frontend", "backend", "fullstack", "data", "mobile" -> Area.PROGRAMACION;
            case "ui-ux", "diseno-grafico" -> Area.DISENO;
            default -> especialidad == null || especialidad.isBlank() ? Area.OTRAS : AreaEmpleo.de(especialidad);
        };
    }

    private static String capitalizar(String s) {
        return s.isEmpty() ? s : Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }
}
