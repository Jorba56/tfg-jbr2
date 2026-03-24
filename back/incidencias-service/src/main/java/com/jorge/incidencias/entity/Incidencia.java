package com.jorge.incidencias.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Entidad que representa un registro de error o excepción capturada en el sistema.
 * Mapea la tabla "incidencias" en la base de datos para mantener un historial de auditoría.
 */
@Entity
@Table(name = "incidencias")
public class Incidencia {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idIncidencia;

    @Column(name = "endpoint")
    private String endpoint;

    @Column(name = "tipo")
    private String tipo;

    @Column(name = "clase")
    private String clase;

    @Column(name = "metodo")
    private String metodo;

    @Column(name = "traza", columnDefinition = "TEXT")
    private String traza;

    @Column(name = "fecha")
    private LocalDateTime fecha;

    @Column(name = "id_usuario")
    private Long idUsuario;


    // ¡Añade aquí los Getters y Setters correspondientes para todos los campos!
    public Long getIdIncidencia() { return idIncidencia; }
    public void setIdIncidencia(Long idIncidencia) { this.idIncidencia = idIncidencia; }

    public String getEndpoint() { return endpoint; }
    public void setEndpoint(String endpoint) { this.endpoint = endpoint; }

    public String getTipo() { return tipo; }
    public void setTipo(String tipo) { this.tipo = tipo; }

    public String getClase() { return clase; }
    public void setClase(String clase) { this.clase = clase; }

    public String getMetodo() { return metodo; }
    public void setMetodo(String metodo) { this.metodo = metodo; }

    public String getTraza() { return traza; }
    public void setTraza(String traza) { this.traza = traza; }

    public LocalDateTime getFecha() { return fecha; }
    public void setFecha(LocalDateTime fecha) { this.fecha = fecha; }

    public Long getIdUsuario() { return idUsuario; }
    public void setIdUsuario(Long idUsuario) { this.idUsuario = idUsuario; }
}