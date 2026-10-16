package com.example.devcoast.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

import com.example.devcoast.config.RoleGuard;

import jakarta.servlet.http.HttpSession;

// TEMPORAL (ronda 1 del rediseño náutico): página para ver el shell nuevo (menú lateral + barra superior)
// y los componentes compartidos antes de que exista la primera página rediseñada. Se elimina en la ronda 3,
// cuando Empleos pase a usar el shell.
@Controller
public class ShellPreviewController {

    @GetMapping("/shell-preview")
    public String shellPreview(HttpSession session) {
        if (RoleGuard.sinSesion(session))
            return "redirect:/login";
        return "shell-preview";
    }
}
