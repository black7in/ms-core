package com.bustrack.mscore.dashboard;

import jakarta.persistence.EntityManager;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import java.math.BigDecimal;
import java.util.*;

@Controller
public class DashboardController {

    private final EntityManager em;

    public DashboardController(EntityManager em) { this.em = em; }

    @QueryMapping @PreAuthorize("hasAnyRole('ADMIN','SUPERVISOR')")
    public Map<String, Object> resumenVentas(@Argument String fechaInicio, @Argument String fechaFin) {
        var q = "SELECT COALESCE(COUNT(b),0), COALESCE(SUM(b.precioPagado),0) FROM Boleto b " +
                "JOIN b.viaje v WHERE v.fecha BETWEEN :inicio AND :fin AND b.estado != 'CANCELADO'";
        var r = (Object[]) em.createQuery(q)
                .setParameter("inicio", fechaInicio).setParameter("fin", fechaFin).getSingleResult();
        var viajes = em.createQuery("SELECT COUNT(v) FROM Viaje v WHERE v.fecha BETWEEN :i AND :f")
                .setParameter("i", fechaInicio).setParameter("f", fechaFin).getSingleResult();
        return Map.of("totalBoletos", ((Number)r[0]).intValue(),
                "totalIngresos", ((BigDecimal)r[1]).doubleValue(),
                "viajesHoy", ((Number)viajes).intValue(),
                "ocupacionPromedio", 0.0);
    }

    @QueryMapping @PreAuthorize("hasAnyRole('ADMIN','SUPERVISOR')")
    public List<Map<String, Object>> ocupacionPorRuta(@Argument String rutaId,
            @Argument String fechaInicio, @Argument String fechaFin) {
        var q = "SELECT v.fecha, b.capacidad, COUNT(a.id) FROM Viaje v " +
                "JOIN v.bus b JOIN v.asientos a " +
                "WHERE v.horario.ruta.id = :rutaId AND v.fecha BETWEEN :i AND :f AND a.estado != 'LIBRE' " +
                "GROUP BY v.fecha, b.capacidad ORDER BY v.fecha";
        return em.createQuery(q, Object[].class)
                .setParameter("rutaId", rutaId).setParameter("i", fechaInicio).setParameter("f", fechaFin)
                .getResultList().stream().map(o -> {
            int vendidos = ((Number)o[2]).intValue(), capacidad = ((Number)o[1]).intValue();
            return Map.<String,Object>of("fecha", o[0].toString(), "vendidos", vendidos,
                    "capacidad", capacidad, "porcentaje", capacidad > 0 ?
                    Math.round((double)vendidos / capacidad * 10000) / 100.0 : 0.0);
        }).toList();
    }

    @QueryMapping @PreAuthorize("hasAnyRole('ADMIN','SUPERVISOR')")
    public List<Map<String, Object>> ingresosPorRuta(@Argument String fechaInicio, @Argument String fechaFin) {
        var q = "SELECT v.horario.ruta.id, v.horario.ruta.origen.ciudad, v.horario.ruta.destino.ciudad, " +
                "COALESCE(SUM(b.precioPagado), 0), COUNT(b) " +
                "FROM Boleto b JOIN b.viaje v " +
                "WHERE b.estado != 'CANCELADO' AND v.fecha BETWEEN :inicio AND :fin " +
                "GROUP BY v.horario.ruta.id, v.horario.ruta.origen.ciudad, v.horario.ruta.destino.ciudad " +
                "ORDER BY SUM(b.precioPagado) DESC";
        return em.createQuery(q, Object[].class)
                .setParameter("inicio", fechaInicio).setParameter("fin", fechaFin)
                .getResultList().stream().map(o -> Map.<String,Object>of(
                    "rutaId", o[0].toString(),
                    "origen", o[1].toString(),
                    "destino", o[2].toString(),
                    "totalIngresos", ((BigDecimal)o[3]).doubleValue(),
                    "totalBoletos", ((Number)o[4]).intValue()
                )).toList();
    }

    @QueryMapping @PreAuthorize("hasAnyRole('ADMIN','SUPERVISOR')")
    public List<Map<String, Object>> ingresosPorDia(@Argument String fechaInicio, @Argument String fechaFin) {
        var q = "SELECT DATE(b.fechaVenta) as f, COALESCE(SUM(b.precioPagado),0) as t, COUNT(b) as c " +
                "FROM Boleto b WHERE b.estado != 'CANCELADO' AND DATE(b.fechaVenta) BETWEEN :inicio AND :fin " +
                "GROUP BY DATE(b.fechaVenta) ORDER BY f";
        return em.createQuery(q, Object[].class)
                .setParameter("inicio", fechaInicio).setParameter("fin", fechaFin).getResultList()
                .stream().map(o -> Map.<String, Object>of(
                    "fecha", o[0].toString(),
                    "totalIngresos", ((BigDecimal)o[1]).doubleValue(),
                    "totalBoletos", ((Number)o[2]).intValue()
                )).toList();
    }

    @QueryMapping @PreAuthorize("hasAnyRole('ADMIN','SUPERVISOR')")
    public int noShowsPorViaje(@Argument String viajeId) {
        var count = em.createQuery(
                "SELECT COUNT(b) FROM Boleto b WHERE b.viaje.id = :viajeId AND b.estado = 'NO_SHOW'",
                Long.class)
                .setParameter("viajeId", viajeId)
                .getSingleResult();
        return count.intValue();
    }
}
