package com.example.devcoast.feria;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

// les dice a todas las plantillas si la app corre en modo feria y si la sesión actual es una sesión de demo
@ControllerAdvice
public class FeriaModelo {

    private final boolean feria;

    public FeriaModelo(@Value("${devcoast.perfil:}") String perfil) {
        this.feria = "feria".equalsIgnoreCase(perfil);
    }

    @ModelAttribute("feria")
    public boolean feria() {
        return feria;
    }

    // el recorrido guiado se ofrece una sola vez al entrar a la demo, en el primer dashboard que se carga
    @ModelAttribute("verRecorrido")
    public boolean verRecorrido(HttpSession session, HttpServletRequest request) {
        if (!feria || !"GET".equals(request.getMethod()) || !"/dashboard".equals(request.getRequestURI()))
            return false;
        boolean ver = Boolean.TRUE.equals(session.getAttribute("verRecorrido"));
        if (ver)
            session.removeAttribute("verRecorrido");
        return ver;
    }

    @ModelAttribute("modoDemo")
    public boolean modoDemo(HttpSession session) {
        return feria && Boolean.TRUE.equals(session.getAttribute("modoDemo"));
    }
}
