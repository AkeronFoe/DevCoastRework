package com.example.devcoast.model;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

@Document(collection = "usuarios")
public class Usuario {

    @Id
    private String id;

    private String tipo;
    private String nombre;

    @Field("rol_titulo")
    private String rolTitulo;

    private String bio;
    private List<String> stack;
    private String especialidad;
    private String modalidad;

    @Field("salario_esperado")
    private String salarioEsperado;

    private String disponibilidad;

    @Field("email_contacto")
    private String emailContacto;

    private String telefono;

    @Field("portfolio_url")
    private String portfolioUrl;

    @Field("linkedin_url")
    private String linkedinUrl;

    @Field("github_url")
    private String githubUrl;

    @Field("empresa_id")
    private String empresaId;

    private String correo;
    private String password;
    private boolean activo = true;

    @Field("creado_en")
    private Date creadoEn;

    @Field("actualizado_en")
    private Date actualizadoEn;

    private String ubicacion;

    @Field("foto_url")
    private String fotoUrl;

    @Field("cv_bullets")
    private List<String> cvBullets;

    @Field("habilidades_blandas")
    private List<String> habilidadesBlandas;

    @Field("nivel_espanol")
    private Integer nivelEspanol;

    @Field("nivel_ingles")
    private Integer nivelIngles;

    @Field("cv_experiencias")
    private List<CvExperiencia> cvExperiencias;

    @Field("cv_educaciones")
    private List<CvEducacion> cvEducaciones;

    @Field("perfil_publico")
    private boolean perfilPublico = true;

    @Field("mostrar_correo")
    private boolean mostrarCorreo;

    @Field("permitir_mensajes")
    private Boolean permitirMensajes = true;

    @Field("empleos_favoritos")
    private List<String> empleosFavoritos = new ArrayList<>();

    @Field("onboarding_completo")
    private boolean onboardingCompleto = false;

    private boolean destacar = false;

    public Usuario() {}

    public String getId()                          { return id; }
    public String getTipo()                        { return tipo; }
    public String getNombre()                      { return nombre; }
    public String getRolTitulo()                   { return rolTitulo; }
    public String getBio()                         { return bio; }
    public List<String> getStack()                 { return stack; }
    public String getEspecialidad()                { return especialidad; }
    public String getModalidad()                   { return modalidad; }
    public String getSalarioEsperado()             { return salarioEsperado; }
    public String getDisponibilidad()              { return disponibilidad; }
    public String getEmailContacto()               { return emailContacto; }
    public String getTelefono()                    { return telefono; }
    public String getPortfolioUrl()                { return portfolioUrl; }
    public String getLinkedinUrl()                 { return linkedinUrl; }
    public String getGithubUrl()                   { return githubUrl; }
    public String getEmpresaId()                   { return empresaId; }
    public String getCorreo()                      { return correo; }
    public String getPassword()                    { return password; }
    public boolean isActivo()                      { return activo; }
    public Date getCreadoEn()                      { return creadoEn; }
    public Date getActualizadoEn()                 { return actualizadoEn; }
    public String getUbicacion()                   { return ubicacion; }
    public String getFotoUrl()                     { return fotoUrl; }
    public List<String> getCvBullets()             { return cvBullets; }
    public List<String> getHabilidadesBlandas()    { return habilidadesBlandas; }
    public Integer getNivelEspanol()               { return nivelEspanol; }
    public Integer getNivelIngles()                { return nivelIngles; }
    public List<CvExperiencia> getCvExperiencias() { return cvExperiencias; }
    public List<CvEducacion> getCvEducaciones()    { return cvEducaciones; }
    public boolean isPerfilPublico()               { return perfilPublico; }
    public boolean isMostrarCorreo()               { return mostrarCorreo; }
    public Boolean getPermitirMensajes()           { return permitirMensajes; }
    public List<String> getEmpleosFavoritos()      { return empleosFavoritos; }
    public boolean isOnboardingCompleto()          { return onboardingCompleto; }
    public boolean isDestacar()                    { return destacar; }

    public void setId(String id)                                      { this.id = id; }
    public void setTipo(String tipo)                                  { this.tipo = tipo; }
    public void setNombre(String nombre)                              { this.nombre = nombre; }
    public void setRolTitulo(String rolTitulo)                        { this.rolTitulo = rolTitulo; }
    public void setBio(String bio)                                    { this.bio = bio; }
    public void setStack(List<String> stack)                          { this.stack = stack; }
    public void setEspecialidad(String especialidad)                  { this.especialidad = especialidad; }
    public void setModalidad(String modalidad)                        { this.modalidad = modalidad; }
    public void setSalarioEsperado(String salarioEsperado)            { this.salarioEsperado = salarioEsperado; }
    public void setDisponibilidad(String disponibilidad)              { this.disponibilidad = disponibilidad; }
    public void setEmailContacto(String emailContacto)                { this.emailContacto = emailContacto; }
    public void setTelefono(String telefono)                          { this.telefono = telefono; }
    public void setPortfolioUrl(String portfolioUrl)                  { this.portfolioUrl = portfolioUrl; }
    public void setLinkedinUrl(String linkedinUrl)                    { this.linkedinUrl = linkedinUrl; }
    public void setGithubUrl(String githubUrl)                        { this.githubUrl = githubUrl; }
    public void setEmpresaId(String empresaId)                        { this.empresaId = empresaId; }
    public void setCorreo(String correo)                              { this.correo = correo; }
    public void setPassword(String password)                          { this.password = password; }
    public void setActivo(boolean activo)                             { this.activo = activo; }
    public void setCreadoEn(Date creadoEn)                            { this.creadoEn = creadoEn; }
    public void setActualizadoEn(Date actualizadoEn)                  { this.actualizadoEn = actualizadoEn; }
    public void setUbicacion(String ubicacion)                        { this.ubicacion = ubicacion; }
    public void setFotoUrl(String fotoUrl)                            { this.fotoUrl = fotoUrl; }
    public void setCvBullets(List<String> cvBullets)                  { this.cvBullets = cvBullets; }
    public void setHabilidadesBlandas(List<String> habilidadesBlandas){ this.habilidadesBlandas = habilidadesBlandas; }
    public void setNivelEspanol(Integer nivelEspanol)                 { this.nivelEspanol = nivelEspanol; }
    public void setNivelIngles(Integer nivelIngles)                   { this.nivelIngles = nivelIngles; }
    public void setCvExperiencias(List<CvExperiencia> cvExperiencias) { this.cvExperiencias = cvExperiencias; }
    public void setCvEducaciones(List<CvEducacion> cvEducaciones)     { this.cvEducaciones = cvEducaciones; }
    public void setPerfilPublico(boolean perfilPublico)               { this.perfilPublico = perfilPublico; }
    public void setMostrarCorreo(boolean mostrarCorreo)               { this.mostrarCorreo = mostrarCorreo; }
    public void setPermitirMensajes(Boolean permitirMensajes)         { this.permitirMensajes = permitirMensajes; }
    public void setEmpleosFavoritos(List<String> empleosFavoritos)    { this.empleosFavoritos = empleosFavoritos; }
    public void setOnboardingCompleto(boolean onboardingCompleto)     { this.onboardingCompleto = onboardingCompleto; }
    public void setDestacar(boolean destacar)                         { this.destacar = destacar; }

    public int getCompletitudPerfil() {
        int puntos = 0;
        int total  = 12;
        if (nombre        != null && !nombre.isBlank())        puntos++;
        if (rolTitulo     != null && !rolTitulo.isBlank())     puntos++;
        if (bio           != null && !bio.isBlank())           puntos++;
        if (stack         != null && !stack.isEmpty())         puntos++;
        if (emailContacto != null && !emailContacto.isBlank()) puntos++;
        if (ubicacion     != null && !ubicacion.isBlank())     puntos++;
        if (modalidad     != null && !modalidad.isBlank())     puntos++;
        if (portfolioUrl  != null && !portfolioUrl.isBlank())  puntos++;
        if (linkedinUrl   != null && !linkedinUrl.isBlank())   puntos++;
        if (githubUrl     != null && !githubUrl.isBlank())     puntos++;
        if (cvExperiencias != null && !cvExperiencias.isEmpty()) puntos++;
        if (cvEducaciones  != null && !cvEducaciones.isEmpty())  puntos++;
        return (puntos * 100) / total;
    }

    public static class CvExperiencia {
        private String cargo;
        private String empresa;
        private String periodo;
        private String modalidad;
        private String descripcion;

        public CvExperiencia() {}

        public String getCargo()       { return cargo; }
        public String getEmpresa()     { return empresa; }
        public String getPeriodo()     { return periodo; }
        public String getModalidad()   { return modalidad; }
        public String getDescripcion() { return descripcion; }

        public void setCargo(String cargo)           { this.cargo = cargo; }
        public void setEmpresa(String empresa)       { this.empresa = empresa; }
        public void setPeriodo(String periodo)       { this.periodo = periodo; }
        public void setModalidad(String modalidad)   { this.modalidad = modalidad; }
        public void setDescripcion(String desc)      { this.descripcion = desc; }
    }

    public static class CvEducacion {
        private String institucion;
        private String titulo;
        private String periodo;
        private String descripcion;

        public CvEducacion() {}

        public String getInstitucion() { return institucion; }
        public String getTitulo()      { return titulo; }
        public String getPeriodo()     { return periodo; }
        public String getDescripcion() { return descripcion; }

        public void setInstitucion(String institucion) { this.institucion = institucion; }
        public void setTitulo(String titulo)           { this.titulo = titulo; }
        public void setPeriodo(String periodo)         { this.periodo = periodo; }
        public void setDescripcion(String desc)        { this.descripcion = desc; }
    }
}
