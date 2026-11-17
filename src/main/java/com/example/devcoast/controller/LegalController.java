package com.example.devcoast.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

// privacidad y términos: páginas públicas, sin sesión
@Controller
public class LegalController {

    @GetMapping("/privacidad")
    public String privacidad(Model model) {
        model.addAttribute("seccion", "privacidad");
        model.addAttribute("titulo", "Privacidad");
        return "legal";
    }

    @GetMapping("/terminos")
    public String terminos(Model model) {
        model.addAttribute("seccion", "terminos");
        model.addAttribute("titulo", "Términos de uso");
        return "legal";
    }
}
