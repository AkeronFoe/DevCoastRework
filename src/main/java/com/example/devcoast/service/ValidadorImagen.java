package com.example.devcoast.service;

public final class ValidadorImagen {

    public static final long MAX_BYTES = 5L * 1024 * 1024;

    private ValidadorImagen() {
    }

    public enum Tipo {
        JPEG, PNG, GIF, WEBP
    }

    public static Tipo detectar(byte[] d) {
        if (d == null || d.length < 12)
            return null;
        if ((d[0] & 0xFF) == 0xFF && (d[1] & 0xFF) == 0xD8 && (d[2] & 0xFF) == 0xFF)
            return Tipo.JPEG;
        if ((d[0] & 0xFF) == 0x89 && d[1] == 'P' && d[2] == 'N' && d[3] == 'G'
                && d[4] == 0x0D && d[5] == 0x0A && d[6] == 0x1A && d[7] == 0x0A)
            return Tipo.PNG;
        if (d[0] == 'G' && d[1] == 'I' && d[2] == 'F' && d[3] == '8' && (d[4] == '7' || d[4] == '9') && d[5] == 'a')
            return Tipo.GIF;
        if (d[0] == 'R' && d[1] == 'I' && d[2] == 'F' && d[3] == 'F'
                && d[8] == 'W' && d[9] == 'E' && d[10] == 'B' && d[11] == 'P')
            return Tipo.WEBP;
        return null;
    }

    public static String problema(byte[] datos) {
        if (datos == null || datos.length == 0)
            return "Archivo vacío";
        if (datos.length > MAX_BYTES)
            return "La imagen pesa más de 5 MB";
        if (detectar(datos) == null)
            return "El archivo debe ser una imagen JPG, PNG, GIF o WebP";
        return null;
    }
}
