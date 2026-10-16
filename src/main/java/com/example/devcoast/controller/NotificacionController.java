package com.example.devcoast.controller;

import java.util.Map;

import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

import com.example.devcoast.model.Notificacion;
import com.example.devcoast.repository.NotificacionRepository;

import jakarta.servlet.http.HttpSession;

@Controller
public class NotificacionController {

    private final NotificacionRepository notificacionRepository;
    private final MongoTemplate mongoTemplate;

    public NotificacionController(NotificacionRepository notificacionRepository, MongoTemplate mongoTemplate) {
        this.notificacionRepository = notificacionRepository;
        this.mongoTemplate = mongoTemplate;
    }

    @GetMapping("/notificaciones/{id}/ir")
    public String irANotificacion(@PathVariable String id, HttpSession session) {
        String uid = (String) session.getAttribute("usuarioActualId");
        if (uid == null) return "redirect:/login";

        Notificacion n = notificacionRepository.findById(id).orElse(null);
        if (n != null && uid.equals(n.getDestinatarioId())) {
            n.setLeida(true);
            notificacionRepository.save(n);
            if (n.getUrl() != null && !n.getUrl().isBlank())
                return "redirect:" + n.getUrl();
        }
        return "redirect:/dashboard";
    }

    // esto lo llama el AJAX del botón "marcar todas como leídas"
    @PostMapping("/notificaciones/leer-todas")
    public ResponseEntity<Map<String, Object>> leerTodas(HttpSession session) {
        String uid = (String) session.getAttribute("usuarioActualId");
        if (uid == null) return ResponseEntity.status(401).body(Map.of("ok", false));

        mongoTemplate.updateMulti(
                Query.query(Criteria.where("destinatarioId").is(uid).and("leida").is(false)),
                Update.update("leida", true),
                Notificacion.class);

        return ResponseEntity.ok(Map.of("ok", true));
    }
}
