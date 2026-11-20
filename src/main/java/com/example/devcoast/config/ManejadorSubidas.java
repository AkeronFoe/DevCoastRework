package com.example.devcoast.config;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

@RestControllerAdvice
public class ManejadorSubidas {

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<Map<String, Object>> demasiadoGrande() {
        return ResponseEntity.status(413).body(Map.of("ok", false, "error", "La imagen pesa más de 5 MB"));
    }
}
