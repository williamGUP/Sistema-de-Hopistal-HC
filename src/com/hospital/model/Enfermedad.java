package com.hospital.model;

import java.time.LocalDate;

/** Un antecedente / diagnóstico de enfermedad de un paciente. */
public class Enfermedad {
    private Long id;
    private Long pacienteId;
    private String nombre;
    private LocalDate fechaDiagnostico;
    private String estado; // ACTIVA, CONTROLADA, CURADA
    private String observaciones;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getPacienteId() { return pacienteId; }
    public void setPacienteId(Long pacienteId) { this.pacienteId = pacienteId; }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public LocalDate getFechaDiagnostico() { return fechaDiagnostico; }
    public void setFechaDiagnostico(LocalDate fechaDiagnostico) { this.fechaDiagnostico = fechaDiagnostico; }

    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }

    public String getObservaciones() { return observaciones; }
    public void setObservaciones(String observaciones) { this.observaciones = observaciones; }
}
