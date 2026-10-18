package com.example.devcoast.config;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.LongSupplier;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

// esto de acá cuenta cuántas veces alguien hace algo en un rato (por ejemplo, crear cuentas) y avisa cuando ya fue demasiado
@Component
public class LimiteTasa {

    private static final long GUARDAR_MS = 60 * 60 * 1000L;

    private final Map<String, Deque<Long>> golpes = new ConcurrentHashMap<>();
    private final LongSupplier reloj;

    public LimiteTasa() {
        this(System::currentTimeMillis);
    }

    LimiteTasa(LongSupplier reloj) {
        this.reloj = reloj;
    }

    /** Anota este intento y dice si ya se pasó de {@code max} en los últimos {@code ventanaMs}. */
    public boolean excedido(String clave, int max, long ventanaMs) {
        String k = clave == null ? "" : (clave.length() > 160 ? clave.substring(0, 160) : clave);
        long ahora = reloj.getAsLong();
        Deque<Long> q = golpes.computeIfAbsent(k, x -> new ArrayDeque<>());
        synchronized (q) {
            while (!q.isEmpty() && q.peekFirst() < ahora - ventanaMs)
                q.pollFirst();
            boolean pasado = q.size() >= max;
            if (!pasado)
                q.addLast(ahora);
            return pasado;
        }
    }

    @Scheduled(fixedDelay = 10 * 60 * 1000L)
    public void limpiar() {
        long corte = reloj.getAsLong() - GUARDAR_MS;
        golpes.entrySet().removeIf(e -> {
            synchronized (e.getValue()) {
                return e.getValue().isEmpty() || e.getValue().peekLast() < corte;
            }
        });
    }
}
