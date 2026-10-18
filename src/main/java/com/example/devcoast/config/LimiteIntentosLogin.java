package com.example.devcoast.config;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.LongSupplier;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

// oigan, esto de acá frena a quien intenta adivinar contraseñas: después de varios intentos fallidos seguidos hay que esperar un rato
@Component
public class LimiteIntentosLogin {

    static final long VENTANA_MS = 15 * 60 * 1000L;
    static final int MAX_POR_CUENTA_Y_EQUIPO = 5;
    static final int MAX_POR_EQUIPO = 30;
    static final int MAX_POR_CUENTA = 20;

    private final Map<String, Deque<Long>> fallos = new ConcurrentHashMap<>();
    private final LongSupplier reloj;

    public LimiteIntentosLogin() {
        this(System::currentTimeMillis);
    }

    LimiteIntentosLogin(LongSupplier reloj) {
        this.reloj = reloj;
    }

    public boolean bloqueado(String ip, String correo) {
        return cuenta(claveCuentaYEquipo(ip, correo)) >= MAX_POR_CUENTA_Y_EQUIPO
                || cuenta(claveEquipo(ip)) >= MAX_POR_EQUIPO
                || cuenta(claveCuenta(correo)) >= MAX_POR_CUENTA;
    }

    public void fallo(String ip, String correo) {
        registrar(claveCuentaYEquipo(ip, correo));
        registrar(claveEquipo(ip));
        registrar(claveCuenta(correo));
    }

    public void exito(String ip, String correo) {
        fallos.remove(claveCuentaYEquipo(ip, correo));
    }

    @Scheduled(fixedDelay = 10 * 60 * 1000L)
    public void limpiar() {
        long corte = reloj.getAsLong() - VENTANA_MS;
        fallos.entrySet().removeIf(e -> {
            synchronized (e.getValue()) {
                while (!e.getValue().isEmpty() && e.getValue().peekFirst() < corte)
                    e.getValue().pollFirst();
                return e.getValue().isEmpty();
            }
        });
    }

    private int cuenta(String clave) {
        Deque<Long> q = fallos.get(clave);
        if (q == null)
            return 0;
        synchronized (q) {
            podar(q);
            return q.size();
        }
    }

    private void registrar(String clave) {
        Deque<Long> q = fallos.computeIfAbsent(clave, k -> new ArrayDeque<>());
        synchronized (q) {
            podar(q);
            q.addLast(reloj.getAsLong());
        }
    }

    private void podar(Deque<Long> q) {
        long corte = reloj.getAsLong() - VENTANA_MS;
        while (!q.isEmpty() && q.peekFirst() < corte)
            q.pollFirst();
    }

    private static String recortar(String s) {
        String t = s == null ? "" : s.trim().toLowerCase();
        return t.length() > 120 ? t.substring(0, 120) : t;
    }

    private static String claveCuentaYEquipo(String ip, String correo) {
        return "c|" + recortar(ip) + "|" + recortar(correo);
    }

    private static String claveEquipo(String ip) {
        return "i|" + recortar(ip);
    }

    private static String claveCuenta(String correo) {
        return "u|" + recortar(correo);
    }
}
