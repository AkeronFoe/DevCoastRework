package com.example.devcoast.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import com.example.devcoast.service.UrlSegura;

import jakarta.servlet.http.HttpServletRequest;

// datos que comparten las plantillas públicas: la dirección base (para la vista previa al compartir el enlace) y el contacto del equipo
@ControllerAdvice(annotations = Controller.class)
public class PaginaModelo {

    private static final UrlsVista URLS = new UrlsVista();

    private final String urlPublica;
    private final String contacto;

    public PaginaModelo(@Value("${devcoast.url-publica:}") String urlPublica, @Value("${devcoast.contacto:}") String contacto) {
        this.urlPublica = urlPublica == null ? "" : urlPublica.trim().replaceAll("/+$", "");
        this.contacto = contacto == null ? "" : contacto.trim();
    }

    // con DEVCOAST_URL_PUBLICA se usa tal cual; si no, se arma con la petición (detrás de un proxy hace falta forward-headers-strategy=framework)
    @ModelAttribute("urlBase")
    public String urlBase(HttpServletRequest request) {
        if (!urlPublica.isEmpty())
            return urlPublica;
        String esquema = request.getScheme();
        int puerto = request.getServerPort();
        boolean puertoPorDefecto = ("http".equals(esquema) && puerto == 80) || ("https".equals(esquema) && puerto == 443);
        return esquema + "://" + request.getServerName() + (puertoPorDefecto ? "" : ":" + puerto);
    }

    @ModelAttribute("contactoCorreo")
    public String contactoCorreo() {
        return contacto;
    }

    @ModelAttribute("urls")
    public UrlsVista urls() {
        return URLS;
    }

    public static final class UrlsVista {

        public String segura(String url) {
            return UrlSegura.paraMostrar(url);
        }
    }
}
