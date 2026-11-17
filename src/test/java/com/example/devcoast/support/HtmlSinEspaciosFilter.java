package com.example.devcoast.support;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingResponseWrapper;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

// Solo existe en las pruebas (vive en src/test): los @WebMvcTest lo recogen solos al escanear el paquete.
// En HTML un salto de línea o una sangría valen lo mismo que un espacio, así que las pruebas comparan el texto con los
// espacios colapsados. Si no, cualquier formateador que parta una frase en dos líneas rompe los tests sin que la página cambie.
@Component
public class HtmlSinEspaciosFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        ContentCachingResponseWrapper envuelta = new ContentCachingResponseWrapper(response);
        chain.doFilter(request, envuelta);

        String tipo = envuelta.getContentType();
        byte[] cuerpo = envuelta.getContentAsByteArray();
        if (tipo == null || !tipo.toLowerCase().contains("html") || cuerpo.length == 0) {
            envuelta.copyBodyToResponse();
            return;
        }

        String codificacion = envuelta.getCharacterEncoding();
        Charset charset = codificacion != null ? Charset.forName(codificacion) : StandardCharsets.UTF_8;
        // además de colapsar espacios, se quita el que queda pegado a una etiqueta de cierre (un formateador que mueve «</option>»
        // a la línea siguiente deja ahí un espacio que el navegador ignora)
        byte[] normalizado = new String(cuerpo, charset).replaceAll("\\s+", " ").replaceAll(" </", "</").getBytes(charset);
        response.setContentLength(normalizado.length);
        response.getOutputStream().write(normalizado);
    }
}
