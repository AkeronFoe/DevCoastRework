package com.example.devcoast.service;

import java.text.Normalizer;

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

    public static Area deProyecto(String categoria) {
        String c = sinTildes(categoria).toLowerCase().trim();
        if (c.matches("^(frontend|backend|full ?stack|app movil|mobile|api|devops|data).*"))
            return Area.PROGRAMACION;
        if (c.matches("^(ui|ux|ilustracion|branding|motion|design system|dashboard|diseno|grafico|3d).*"))
            return Area.DISENO;
        return Area.OTRAS;
    }

    public static String categoriaVisible(String categoria) {
        return textoVisible(categoria);
    }

    // los formularios guardan algunas palabras sin tilde (Hibrido, Educacion...).
    // Esto las escribe bien solo al mostrarlas; lo guardado no se toca
    public static String textoVisible(String texto) {
        if (texto == null)
            return null;
        return texto
                .replaceAll("\\bProgramacion\\b", "Programación")
                .replaceAll("\\bDiseno\\b", "Diseño")
                .replaceAll("\\bGrafico\\b", "Gráfico")
                .replaceAll("\\bIlustracion\\b", "Ilustración")
                .replaceAll("\\bEducacion\\b", "Educación")
                .replaceAll("\\bHibrido\\b", "Híbrido")
                .replaceAll("\\bPracticas\\b", "Prácticas")
                .replaceAll("\\brestriccion\\b", "restricción")
                .replaceAll("\\bBasico\\b", "Básico");
    }

    private static String sinTildes(String s) {
        if (s == null)
            return "";
        return Normalizer.normalize(s, Normalizer.Form.NFD).replaceAll("\\p{M}+", "").replace('ñ', 'n').replace('Ñ',
                'N');
    }
}
