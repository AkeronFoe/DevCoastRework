package com.example.devcoast.service;

import java.nio.charset.StandardCharsets;

public final class Credenciales {

    public static final int MIN_CARACTERES = 8;
    public static final int MAX_BYTES = 72;

    private Credenciales() {
    }

    public static boolean cumpleReglas(String password) {
        return password != null && password.trim().length() >= MIN_CARACTERES && cabeEnBcrypt(password);
    }

    public static boolean cabeEnBcrypt(String password) {
        return password != null && password.getBytes(StandardCharsets.UTF_8).length <= MAX_BYTES;
    }

    public static String problema(String password) {
        if (password == null || password.trim().length() < MIN_CARACTERES)
            return "La contraseña debe tener al menos " + MIN_CARACTERES + " caracteres";
        if (!cabeEnBcrypt(password))
            return "La contraseña es demasiado larga (máximo " + MAX_BYTES + " bytes, unos 70 caracteres)";
        return null;
    }
}
