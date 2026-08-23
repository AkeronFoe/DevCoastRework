package com.example.devcoast.repository;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import com.example.devcoast.model.Usuario;

public interface UsuarioRepository extends MongoRepository<Usuario, String> {

    Usuario findByCorreo(String correo);

    List<Usuario> findByPerfilPublicoTrueAndTipoNot(String tipo);

    Page<Usuario> findByPerfilPublicoTrueAndTipoNot(String tipo, Pageable pageable);

    @Query("{ 'tipo': { $ne: ?0 }, 'perfil_publico': true, $or: [" +
            "{ 'nombre': { $regex: ?1, $options: 'i' } }," +
            "{ 'rol_titulo': { $regex: ?1, $options: 'i' } }," +
            "{ 'stack': { $regex: ?1, $options: 'i' } }" +
            "] }")
    Page<Usuario> searchTalentos(String tipo, String q, Pageable pageable);
}