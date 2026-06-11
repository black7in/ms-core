package com.bustrack.mscore;

import jakarta.persistence.EntityManager;
import jakarta.persistence.Tuple;
import jakarta.persistence.TupleElement;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/reportes")
@PreAuthorize("hasRole('ADMIN')")
public class ReportesController {

    private final EntityManager em;

    public ReportesController(EntityManager em) {
        this.em = em;
    }

    @PostMapping("/query")
    public ResponseEntity<?> query(@RequestBody Map<String, String> body) {
        String sql = body.get("sql");
        if (sql == null || sql.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "El campo 'sql' es requerido"));
        }

        String trimmed = sql.strip();
        String upper = trimmed.toUpperCase();

        if (!upper.startsWith("SELECT")) {
            return ResponseEntity.badRequest().body(Map.of("error", "Solo se permiten sentencias SELECT"));
        }

        String[] forbidden = {"INSERT", "UPDATE", "DELETE", "DROP", "TRUNCATE", "CREATE", "ALTER", "EXEC", "EXECUTE"};
        for (String kw : forbidden) {
            if (upper.matches("(?s).*\\b" + kw + "\\b.*")) {
                return ResponseEntity.badRequest().body(Map.of("error", "Sentencia no permitida: contiene " + kw));
            }
        }

        // Strip trailing semicolons before wrapping
        String cleanSql = trimmed.replaceAll(";+$", "");
        String safeSql = "SELECT * FROM (" + cleanSql + ") AS _q LIMIT 100";

        try {
            @SuppressWarnings("unchecked")
            List<Tuple> results = em.createNativeQuery(safeSql, Tuple.class).getResultList();

            List<String> columnas = results.isEmpty() ? List.of() :
                results.get(0).getElements().stream()
                    .map(TupleElement::getAlias)
                    .collect(Collectors.toList());

            List<List<String>> filas = results.stream()
                .map(t -> columnas.stream()
                    .map(col -> String.valueOf(t.get(col)))
                    .collect(Collectors.toList()))
                .collect(Collectors.toList());

            return ResponseEntity.ok(Map.of(
                "columnas", columnas,
                "filas", filas,
                "total", filas.size()
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", "SQL inválido: " + e.getMessage()));
        }
    }

    @GetMapping("/datos-entrenamiento")
    public Object datosEntrenamiento(@RequestParam String tipo) {
        return switch (tipo) {
            case "demanda" -> getDemanda();
            case "clientes" -> getClientes();
            case "ventas" -> getVentas();
            default -> Map.of("error", "tipo inválido: demanda, clientes, ventas");
        };
    }

    private Map<String, Object> getDemanda() {
        List<Object[]> rows = em.createNativeQuery("""
            WITH base AS (
                SELECT
                    v.fecha::date                                        AS fecha,
                    r.id::text                                           AS ruta_id,
                    EXTRACT(DOW   FROM v.fecha::date)::int              AS dia_semana,
                    EXTRACT(MONTH FROM v.fecha::date)::int              AS mes,
                    EXTRACT(WEEK  FROM v.fecha::date)::int              AS semana_del_anio,
                    CASE WHEN EXISTS (
                        SELECT 1 FROM tarifas tf
                        WHERE tf.ruta_id = r.id AND tf.tipo_dia = 'FERIADO'
                          AND tf.vigente_desde::date <= v.fecha::date
                          AND (tf.vigente_hasta IS NULL OR tf.vigente_hasta::date >= v.fecha::date)
                    ) THEN 1 ELSE 0 END                                  AS es_feriado,
                    CASE WHEN EXISTS (
                        SELECT 1 FROM tarifas tf
                        WHERE tf.ruta_id = r.id AND tf.tipo_dia = 'TEMPORADA_ALTA'
                          AND tf.vigente_desde::date <= v.fecha::date
                          AND (tf.vigente_hasta IS NULL OR tf.vigente_hasta::date >= v.fecha::date)
                    ) THEN 1 ELSE 0 END                                  AS es_temporada_alta,
                    ROUND(
                        COUNT(av.id) FILTER (WHERE av.estado != 'LIBRE')::numeric /
                        NULLIF(COUNT(av.id), 0), 4
                    )                                                    AS ocupacion
                FROM viajes v
                JOIN horarios h       ON v.horario_id = h.id
                JOIN rutas r          ON h.ruta_id    = r.id
                JOIN asientos_viaje av ON av.viaje_id  = v.id
                WHERE v.estado IN ('FINALIZADO', 'EN_RUTA')
                GROUP BY v.fecha::date, r.id
            )
            SELECT
                b.ruta_id,
                b.fecha,
                b.dia_semana,
                b.es_feriado,
                b.mes,
                b.semana_del_anio,
                b.es_temporada_alta,
                COALESCE((
                    SELECT ROUND(AVG(p.ocupacion)::numeric, 4)
                    FROM   base p
                    WHERE  p.ruta_id = b.ruta_id
                      AND  p.fecha BETWEEN b.fecha - INTERVAL '14 days' AND b.fecha - INTERVAL '8 days'
                ), 0)                                                    AS ocupacion_semana_anterior,
                COALESCE((
                    SELECT p7.ocupacion FROM base p7
                    WHERE  p7.ruta_id = b.ruta_id
                      AND  p7.fecha   = b.fecha - INTERVAL '7 days'
                    LIMIT 1
                ), 0)                                                    AS ocupacion_mismo_dia_semana_pasada,
                b.ocupacion
            FROM base b
            ORDER BY b.fecha DESC
            """).getResultList();

        var filas = rows.stream().map(r -> {
            var m = new LinkedHashMap<String, Object>();
            m.put("ruta_id",                          str(r[0]));
            m.put("fecha",                            str(r[1]));
            m.put("dia_semana",                       num(r[2]));
            m.put("es_feriado",                       num(r[3]));
            m.put("mes",                              num(r[4]));
            m.put("semana_del_anio",                  num(r[5]));
            m.put("es_temporada_alta",                num(r[6]));
            m.put("ocupacion_semana_anterior",        dbl(r[7]));
            m.put("ocupacion_mismo_dia_semana_pasada",dbl(r[8]));
            m.put("ocupacion",                        dbl(r[9]));
            return m;
        }).toList();

        return Map.of("filas", filas);
    }

    private Map<String, Object> getClientes() {
        List<Object[]> rows = em.createNativeQuery("""
            SELECT
                c.id::text                                               AS cliente_id,
                ROUND(
                    COUNT(b.id)::numeric /
                    NULLIF((
                        EXTRACT(YEAR  FROM AGE(MAX(b.fecha_venta), MIN(b.fecha_venta))) * 12 +
                        EXTRACT(MONTH FROM AGE(MAX(b.fecha_venta), MIN(b.fecha_venta))) + 1
                    )::numeric, 0), 2)                                   AS frecuencia_mensual,
                ROUND(AVG(b.precio_pagado)::numeric, 2)                  AS gasto_promedio,
                COUNT(DISTINCT r.id)::int                                AS variedad_rutas,
                COALESCE(
                    MODE() WITHIN GROUP (ORDER BY EXTRACT(DOW FROM v.fecha::date)::int), 0
                )::int                                                   AS dia_preferido
            FROM clientes c
            JOIN boletos  b  ON b.cliente_id = c.id AND b.estado != 'CANCELADO'
            JOIN viajes   v  ON b.viaje_id   = v.id
            JOIN horarios h  ON v.horario_id = h.id
            JOIN rutas    r  ON h.ruta_id    = r.id
            GROUP BY c.id
            ORDER BY frecuencia_mensual DESC
            """).getResultList();

        var filas = rows.stream().map(r -> {
            var m = new LinkedHashMap<String, Object>();
            m.put("cliente_id",        str(r[0]));
            m.put("frecuencia_mensual",dbl(r[1]));
            m.put("gasto_promedio",    dbl(r[2]));
            m.put("variedad_rutas",    num(r[3]));
            m.put("dia_preferido",     num(r[4]));
            return m;
        }).toList();

        return Map.of("filas", filas);
    }

    private static String str(Object v) { return v == null ? null : v.toString(); }
    private static Number num(Object v) { return v == null ? 0 : (Number) v; }
    private static Double dbl(Object v) {
        if (v == null) return 0.0;
        return ((java.math.BigDecimal) v).doubleValue();
    }

    private Object getVentas() {
        return em.createNativeQuery("""
            SELECT
                u.id::text AS usuario_id,
                u.nombre,
                COUNT(b.id) FILTER (WHERE b.estado != 'CANCELADO')::int AS total_ventas,
                COUNT(b.id) FILTER (WHERE b.estado = 'CANCELADO')::int AS total_cancelaciones,
                ROUND(
                    COUNT(b.id) FILTER (WHERE b.estado = 'CANCELADO')::numeric /
                    NULLIF(COUNT(b.id), 0) * 100, 2
                ) AS tasa_cancelaciones,
                COALESCE(ROUND(AVG(b.precio_pagado) FILTER (WHERE b.estado != 'CANCELADO')::numeric, 2), 0)::float AS ticket_promedio,
                COUNT(DISTINCT DATE(b.fecha_venta))::int AS dias_con_ventas
            FROM usuarios u
            LEFT JOIN boletos b ON b.vendedor_id = u.id
            WHERE u.rol IN ('VENDEDOR', 'ADMIN')
            GROUP BY u.id, u.nombre
            ORDER BY total_ventas DESC
            """).getResultList();
    }
}
