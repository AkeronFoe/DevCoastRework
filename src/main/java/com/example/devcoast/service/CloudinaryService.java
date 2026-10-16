package com.example.devcoast.service;

import java.io.IOException;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;

@Service
public class CloudinaryService {

    private final Cloudinary cloudinary;

    public CloudinaryService(Cloudinary cloudinary) {
        this.cloudinary = cloudinary;
    }

    public String subirFotoPerfil(MultipartFile file, String usuarioId) throws IOException {
        Map<?, ?> result = cloudinary.uploader().upload(
                file.getBytes(),
                ObjectUtils.asMap(
                        "folder", "devcoast/perfiles",
                        "public_id", "perfil_" + usuarioId,
                        "overwrite", true));
        return (String) result.get("secure_url");
    }

    public String subirImagenProyecto(MultipartFile file) throws IOException {
        Map<?, ?> result = cloudinary.uploader().upload(
                file.getBytes(),
                ObjectUtils.asMap(
                        "folder", "devcoast/proyectos"));
        return (String) result.get("secure_url");
    }
}
