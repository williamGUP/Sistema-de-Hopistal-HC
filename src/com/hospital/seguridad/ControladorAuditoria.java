package com.hospital.seguridad;

import com.hospital.template.TemplateEngine;
import com.hospital.web.RequestContext;
import com.hospital.web.Router;
import com.hospital.web.Views;

import java.util.List;
import java.util.Map;

/**
 * Bitacora de seguridad (solo ADMIN_GENERAL). Muestra los accesos correctos,
 * los intentos fallidos y las peticiones denegadas por falta de permisos, que
 * es la informacion clave para detectar un uso indebido del sistema.
 */
public final class ControladorAuditoria {

    private static final int LIMITE = 150;

    private final TemplateEngine motor;

    public ControladorAuditoria(TemplateEngine motor) {
        this.motor = motor;
    }

    public void register(Router router) {
        router.get("/auditoria", this::listar);
    }

    private void listar(RequestContext ctx) throws Exception {
        Map<String, Object> data = Views.baseData("Bitacora de seguridad", "auditoria", ctx);
        MotorConSesion.inyectar(data);

        String usuario = ctx.param("q", "").trim();
        String resultado = ctx.param("resultado", "").trim().toUpperCase();

        List<Map<String, Object>> eventos = Auditoria.listar(LIMITE, usuario, resultado);

        data.put("q", usuario);
        data.put("resultado", resultado);
        data.put("eventos", eventos);
        data.put("eventosVisibles", eventos.size());
        data.put("limite", LIMITE);
        data.put("totalEventos", Auditoria.contarEventos());
        data.put("totalOk", Auditoria.contar(Auditoria.OK));
        data.put("totalFallidos", Auditoria.contar(Auditoria.FALLIDO));
        data.put("totalDenegados", Auditoria.contar(Auditoria.DENEGADO));
        data.put("totalCierres", Auditoria.contar(Auditoria.CERRADA));
        data.put("hayFiltro", !usuario.isEmpty() || !resultado.isEmpty());
        data.put("resultadoCorrecto", Auditoria.OK.equals(resultado));
        data.put("resultadoFallido", Auditoria.FALLIDO.equals(resultado));
        data.put("resultadoDenegado", Auditoria.DENEGADO.equals(resultado));
        data.put("resultadoCierre", Auditoria.CERRADA.equals(resultado));

        ctx.html(200, motor.render("auditoria", data));
    }
}