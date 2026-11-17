package com.example.devcoast.config;

import org.springframework.stereotype.Component;

import jakarta.servlet.http.HttpSession;

// el portero de la app: dice si quien entra es talento, empresa, o si ni siquiera ha iniciado sesión
@Component
public class RoleGuard {

    public static final String TIPO_EMPRESA = "empresa";
    public static final String TIPO_TALENTO = "talento";

    // lo que guarda la sesión al entrar (login normal o entrada a la demo)
    public static void iniciarSesion(HttpSession session, String id, String correo, String nombre, String tipo, String fotoUrl) {
        session.setAttribute("usuarioActualId", id);
        session.setAttribute("usuarioActualCorreo", correo);
        session.setAttribute("usuarioActualNombre", nombre);
        session.setAttribute("usuarioActualTipo", tipo);
        if (fotoUrl != null && !fotoUrl.isBlank()) {
            session.setAttribute("usuarioActualFotoUrl", fotoUrl);
        } else {
            session.removeAttribute("usuarioActualFotoUrl");
        }
    }

    public static boolean sinSesion(HttpSession session) {
        return session.getAttribute("usuarioActualId") == null;
    }

    public static boolean esEmpresa(HttpSession session) {
        return TIPO_EMPRESA.equals(getTipo(session));
    }

    public static boolean esTalento(HttpSession session) {
        return TIPO_TALENTO.equals(getTipo(session));
    }

    public static boolean soloEmpresa(HttpSession session) {
        return sinSesion(session) || !esEmpresa(session);
    }

    public static boolean soloTalento(HttpSession session) {
        return sinSesion(session) || !esTalento(session);
    }

    public static String getTipo(HttpSession session) {
        Object t = session.getAttribute("usuarioActualTipo");
        return t == null ? "" : t.toString();
    }

    public static String getId(HttpSession session) {
        Object id = session.getAttribute("usuarioActualId");
        return id == null ? null : id.toString();
    }

    public static String getNombre(HttpSession session) {
        Object n = session.getAttribute("usuarioActualNombre");
        return n == null ? "" : n.toString();
    }

    public static String getCorreo(HttpSession session) {
        Object c = session.getAttribute("usuarioActualCorreo");
        return c == null ? "" : c.toString();
    }
}
