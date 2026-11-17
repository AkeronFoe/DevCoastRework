package com.example.devcoast.feria;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

// lector de CSV sin dependencias: coma como separador, comillas dobles para textos con comas o saltos de línea, UTF-8 (con o sin BOM)
final class CsvFeria {

    private CsvFeria() {
    }

    record Fila(int numero, Map<String, String> campos) {

        String get(String columna) {
            String v = campos.get(columna);
            return v == null ? "" : v.trim();
        }

        boolean vacio(String columna) {
            return get(columna).isEmpty();
        }

        boolean si(String columna) {
            String v = get(columna).toLowerCase();
            return v.equals("si") || v.equals("sí") || v.equals("true") || v.equals("1") || v.equals("x");
        }

        Integer entero(String columna) {
            String v = get(columna).replaceAll("[^0-9]", "");
            return v.isEmpty() ? null : Integer.valueOf(v);
        }

        List<String> lista(String columna) {
            List<String> r = new ArrayList<>();
            for (String parte : get(columna).split("[;|]"))
                if (!parte.isBlank())
                    r.add(parte.trim());
            return r;
        }
    }

    static List<Fila> leer(Path archivo) throws IOException {
        try (Reader r = Files.newBufferedReader(archivo, StandardCharsets.UTF_8)) {
            StringBuilder sb = new StringBuilder();
            char[] buf = new char[8192];
            int n;
            while ((n = r.read(buf)) > 0)
                sb.append(buf, 0, n);
            return parsear(sb.toString());
        }
    }

    static List<Fila> parsear(String texto) {
        if (texto.startsWith("﻿"))
            texto = texto.substring(1);
        List<List<String>> registros = new ArrayList<>();
        List<String> actual = new ArrayList<>();
        StringBuilder campo = new StringBuilder();
        boolean entreComillas = false;
        for (int i = 0; i < texto.length(); i++) {
            char c = texto.charAt(i);
            if (entreComillas) {
                if (c == '"') {
                    if (i + 1 < texto.length() && texto.charAt(i + 1) == '"') {
                        campo.append('"');
                        i++;
                    } else {
                        entreComillas = false;
                    }
                } else {
                    campo.append(c);
                }
            } else if (c == '"') {
                entreComillas = true;
            } else if (c == ',') {
                actual.add(campo.toString());
                campo.setLength(0);
            } else if (c == '\n' || c == '\r') {
                if (c == '\r' && i + 1 < texto.length() && texto.charAt(i + 1) == '\n')
                    i++;
                actual.add(campo.toString());
                campo.setLength(0);
                registros.add(actual);
                actual = new ArrayList<>();
            } else {
                campo.append(c);
            }
        }
        if (campo.length() > 0 || !actual.isEmpty()) {
            actual.add(campo.toString());
            registros.add(actual);
        }

        List<Fila> filas = new ArrayList<>();
        List<String> encabezado = null;
        int numero = 0;
        for (List<String> reg : registros) {
            numero++;
            boolean enBlanco = reg.stream().allMatch(String::isBlank);
            if (enBlanco || reg.get(0).trim().startsWith("#"))
                continue;
            if (encabezado == null) {
                encabezado = reg.stream().map(h -> h.trim().toLowerCase()).toList();
                continue;
            }
            Map<String, String> campos = new LinkedHashMap<>();
            for (int i = 0; i < encabezado.size(); i++)
                campos.put(encabezado.get(i), i < reg.size() ? reg.get(i) : "");
            filas.add(new Fila(numero, campos));
        }
        return filas;
    }
}
