package com.example.devcoast.service;

import java.text.Normalizer;

/**
 * Agrupa la categoría de una oferta (texto libre del formulario) en tres áreas para filtros e iconos,
 * y arregla al mostrar las categorías guardadas sin tilde. No modifica ningún dato guardado.
 */
public final class AreaEmpleo {

    public enum Area {
        PROGRAMACION("programacion", "Programación"),
        DISENO("diseno", "Diseño"),
        OTRAS("otras", "Otras");

        private final String clave;
        private final String etiqueta;

        Area(String clave, String etiqueta) {
            this.clave = clave;
            this.etiqueta = etiqueta;
        }

        /** Valor del parámetro ?area= */
        public String clave() {
            return clave;
        }

        public String etiqueta() {
            return etiqueta;
        }

        public static Area deClave(String clave) {
            if (clave == null)
                return null;
            for (Area a : values())
                if (a.clave.equals(clave.trim().toLowerCase()) || a.etiqueta.equalsIgnoreCase(clave.trim()))
                    return a;
            return null;
        }
    }

    private AreaEmpleo() {
    }

    public static Area de(String categoria) {
        String c = sinTildes(categoria).toLowerCase().trim();
        if (c.matches("^(programacion|devops|data|frontend|backend|full ?stack|mobile|qa|cloud|software|ia|ml)\\b.*"))
            return Area.PROGRAMACION;
        if (c.matches("^(diseno|ui|ux|branding|grafico|motion|ilustracion|3d)\\b.*"))
            return Area.DISENO;
        return Area.OTRAS;
    }

    /**
     * Área de un proyecto del portafolio según su categoría (las del formulario de subir proyecto):
     * Frontend, Backend, Full Stack y App móvil son Programación; UI/UX, Ilustraciones 3D, Branding,
     * Motion, Design System y Dashboard SaaS son Diseño; "Otro" y lo desconocido, Otras.
     */
    public static Area deProyecto(String categoria) {
        String c = sinTildes(categoria).toLowerCase().trim();
        if (c.matches("^(frontend|backend|full ?stack|app movil|mobile|api|devops|data).*"))
            return Area.PROGRAMACION;
        if (c.matches("^(ui|ux|ilustracion|branding|motion|design system|dashboard|diseno|grafico|3d).*"))
            return Area.DISENO;
        return Area.OTRAS;
    }

    /** "Programacion · Backend" se muestra como "Programación · Backend"; el resto queda igual. */
    public static String categoriaVisible(String categoria) {
        if (categoria == null)
            return null;
        return categoria
                .replaceAll("\\bProgramacion\\b", "Programación")
                .replaceAll("\\bDiseno\\b", "Diseño")
                .replaceAll("\\bGrafico\\b", "Gráfico")
                .replaceAll("\\bIlustracion\\b", "Ilustración");
    }

    private static String sinTildes(String s) {
        if (s == null)
            return "";
        return Normalizer.normalize(s, Normalizer.Form.NFD).replaceAll("\\p{M}+", "").replace('ñ', 'n').replace('Ñ', 'N');
    }
}
