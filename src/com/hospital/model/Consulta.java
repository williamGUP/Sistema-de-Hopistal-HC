package com.hospital.model;

import java.time.LocalDateTime

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