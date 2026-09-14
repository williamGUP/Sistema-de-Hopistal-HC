package com.hospital.model;

import java.time.LocalDate;

/// Enfermedad registrada en la historia clínica de un paciente. 
public class Enfermedad {

    private Long id;
    private Long pacienteId;
    private String nombre;
    private LocalDate fechaDiagnostico;
    private String estado;
    private String observaciones;

    public Enfermedad() {
    }

    public Enfermedad(Long id, Long pacienteId, String nombre,
                      LocalDate fechaDiagnostico, String estado,
                      String observaciones) {
        this.id = id;
        this.pacienteId = pacienteId;
        this.nombre = nombre;
        this.fechaDiagnostico = fechaDiagnostico;
        this.estado = estado;
        this.observaciones = observaciones;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getPacienteId() {
        return pacienteId;
    }

    public void setPacienteId(Long pacienteId) {
        this.pacienteId = pacienteId;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public LocalDate getFechaDiagnostico() {
        return fechaDiagnostico;
    }

    public void setFechaDiagnostico(LocalDate fechaDiagnostico) {
        this.fechaDiagnostico = fechaDiagnostico;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }

    public String getObservaciones() {
        return observaciones;
    }

    public void setObservaciones(String observaciones) {
        this.observaciones = observaciones;
    }
}
