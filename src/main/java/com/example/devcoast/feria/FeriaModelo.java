package com.example.devcoast.feria;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

// les dice a todas las plantillas si la app corre en modo feria y si la sesión actual es una sesión de demo
// getSession(false): mirar la sesión no debe crear una por cada visita anónima ni por cada consulta a /salud
@ControllerAdvice(annotations = Controller.class)
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
    public boolean verRecorrido(HttpServletRequest request) {
        if (!feria || !"GET".equals(request.getMethod()) || !"/dashboard".equals(request.getRequestURI()))
            return false;
        HttpSession session = request.getSession(false);
        if (session == null)
            return false;
        boolean ver = Boolean.TRUE.equals(session.getAttribute("verRecorrido"));
        if (ver)
            session.removeAttribute("verRecorrido");
        return ver;
    }

    @ModelAttribute("modoDemo")
    public boolean modoDemo(HttpServletRequest request) {
        HttpSession session = feria ? request.getSession(false) : null;
        return session != null && Boolean.TRUE.equals(session.getAttribute("modoDemo"));
    }
}
