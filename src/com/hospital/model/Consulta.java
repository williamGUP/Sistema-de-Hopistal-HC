package com.hospital.model;

import java.time.LocalDateTime;

/** Una atención médica / entrada de historia clínica de un paciente. */
public class Consulta {
    private Long id;
    private Long pacienteId;
    private LocalDateTime fecha;
    private String medico;
    private String motivo;
    private String sintomas;
    private String diagnostico;
    private String tratamiento;
    private String observaciones;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getPacienteId() { return pacienteId; }
    public void setPacienteId(Long pacienteId) { this.pacienteId = pacienteId; }

    public LocalDateTime getFecha() { return fecha; }
    public void setFecha(LocalDateTime fecha) { this.fecha = fecha; }

    public String getMedico() { return medico; }
    public void setMedico(String medico) { this.medico = medico; }

    public String getMotivo() { return motivo; }
    public void setMotivo(String motivo) { this.motivo = motivo; }

    public String getSintomas() { return sintomas; }
    public void setSintomas(String sintomas) { this.sintomas = sintomas; }

    public String getDiagnostico() { return diagnostico; }
    public void setDiagnostico(String diagnostico) { this.diagnostico = diagnostico; }

    public String getTratamiento() { return tratamiento; }
    public void setTratamiento(String tratamiento) { this.tratamiento = tratamiento; }

    public String getObservaciones() { return observaciones; }
    public void setObservaciones(String observaciones) { this.observaciones = observaciones; }
}
