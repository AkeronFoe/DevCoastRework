package com.example.devcoast.service;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Locale;
import java.util.regex.Pattern;

public final class UrlSegura {

    public static final int MAX_LARGO = 500;

    private static final Pattern TIENE_ESQUEMA = Pattern.compile("^[a-zA-Z][a-zA-Z0-9+.-]*:.*$");
    private static final Pattern CARACTERES_PROHIBIDOS = Pattern.compile("[\\s\"'<>`\\\\\\u0000-\\u001f]");

    private UrlSegura() {
    }

    public static String normalizar(String entrada) {
        if (entrada == null)
            return null;
        String u = entrada.trim();
        if (u.isEmpty() || u.length() > MAX_LARGO)
            return null;
        if (u.startsWith("//"))
            return null;
        if (!TIENE_ESQUEMA.matcher(u).matches() || esHostConPuerto(u))
            u = "https://" + u;
        return esSegura(u) ? u : null;
    }

    public static boolean esSegura(String url) {
        if (url == null || url.length() > MAX_LARGO)
            return false;
        if (CARACTERES_PROHIBIDOS.matcher(url).find())
            return false;
        String bajo = url.toLowerCase(Locale.ROOT);
        if (!bajo.startsWith("http://") && !bajo.startsWith("https://"))
            return false;
        try {
            URI uri = new URI(url);
            String host = uri.getHost();
            return host != null && !host.isBlank() && (host.contains(".") || "localhost".equalsIgnoreCase(host));
        } catch (URISyntaxException e) {
            return false;
        }
    }

    public static String paraMostrar(String url) {
        return url != null && esSegura(url.trim()) ? url.trim() : null;
    }

    private static boolean esHostConPuerto(String u) {
        int dosPuntos = u.indexOf(':');
        if (dosPuntos <= 0 || dosPuntos + 1 >= u.length())
            return false;
        String antes = u.substring(0, dosPuntos);
        char despues = u.charAt(dosPuntos + 1);
        return antes.contains(".") && Character.isDigit(despues);
    }
}
