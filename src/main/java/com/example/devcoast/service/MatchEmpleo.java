package com.example.devcoast.service;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import com.example.devcoast.model.Empleo;
import com.example.devcoast.model.Usuario;

// oigan, esto de acá calcula el famoso % de match: cuánto de lo que pide la oferta ya lo tiene la persona en su perfil
public final class MatchEmpleo {

    public static final int NIVEL_ALTO = 85;
    public static final int NIVEL_MEDIO = 65;

    private MatchEmpleo() {
    }

    public record Resultado(int porcentaje, List<String> tiene, List<String> faltan) {
        public String nivel() {
            return MatchEmpleo.nivel(porcentaje);
        }
    }

    public static Resultado calcular(Usuario talento, Empleo empleo) {
        if (talento == null || empleo == null || talento.getStack() == null || talento.getStack().isEmpty())
            return null;
        List<String> requeridas = habilidadesRequeridas(empleo.getStack());
        if (requeridas.isEmpty())
            return null;

        Set<String> delTalento = new LinkedHashSet<>();
        for (String s : talento.getStack()) {
            String k = clave(s);
            if (!k.isEmpty())
                delTalento.add(k);
        }
        if (delTalento.isEmpty())
            return null;

        List<String> tiene = new ArrayList<>();
        List<String> faltan = new ArrayList<>();
        for (String req : requeridas)
            (delTalento.contains(clave(req)) ? tiene : faltan).add(req);

        int pct = Math.round(tiene.size() * 100f / requeridas.size());
        return new Resultado(pct, List.copyOf(tiene), List.copyOf(faltan));
    }

    public static int porcentaje(Usuario talento, Empleo empleo) {
        Resultado r = calcular(talento, empleo);
        return r == null ? 0 : r.porcentaje();
    }

    public static String nivel(int porcentaje) {
        if (porcentaje >= NIVEL_ALTO)
            return "alto";
        if (porcentaje >= NIVEL_MEDIO)
            return "medio";
        return "bajo";
    }

    public static List<String> habilidadesRequeridas(String stack) {
        List<String> out = new ArrayList<>();
        if (stack == null || stack.isBlank())
            return out;
        Set<String> vistas = new LinkedHashSet<>();
        for (String parte : stack.split("[,;\\n]")) {
            String t = parte.trim();
            if (t.isEmpty())
                continue;
            if (vistas.add(clave(t)) && !clave(t).isEmpty())
                out.add(t);
        }
        return out;
    }

    static String clave(String s) {
        if (s == null)
            return "";
        String sinTildes = Normalizer.normalize(s, Normalizer.Form.NFD).replaceAll("\\p{M}+", "");
        return sinTildes.toLowerCase().replaceAll("[^a-z0-9+#]", "");
    }
}
