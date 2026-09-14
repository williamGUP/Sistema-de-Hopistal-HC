package com.hospital.model;

import java.time.LocalDate;

public class Operacion {
    private Long id;
    private Long pacienteId;
    private LocalDate fecha;
    private String tipoOperacion;
    private String cirujano;
    private String resultado;
    private String observaciones;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getPacienteId() { return pacienteId; }
    public void setPacienteId(Long pacienteId) { this.pacienteId = pacienteId; }

    public LocalDate getFecha() { return fecha; }
    public void setFecha(LocalDate fecha) { this.fecha = fecha; }

    public String getTipoOperacion() { return tipoOperacion; }
    public void setTipoOperacion(String tipoOperacion) { this.tipoOperacion = tipoOperacion; }

    public String getCirujano() { return cirujano; }
    public void setCirujano(String cirujano) { this.cirujano = cirujano; }

    public String getResultado() { return resultado; }
    public void setResultado(String resultado) { this.resultado = resultado; }

    public String getObservaciones() { return observaciones; }
    public void setObservaciones(String observaciones) { this.observaciones = observaciones; }
}
