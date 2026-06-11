package com.bustrack.mscore.boletos;

import com.bustrack.mscore.clientes.Cliente;
import com.bustrack.mscore.common.enums.EstadoBoleto;
import com.bustrack.mscore.usuarios.Usuario;
import com.bustrack.mscore.viajes.AsientoViaje;
import com.bustrack.mscore.viajes.Viaje;
import com.bustrack.mscore.facturas.Factura;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity @Table(name = "boletos")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Boleto {

    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "viaje_id", nullable = false)
    private Viaje viaje;

    @OneToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "asiento_id", unique = true, nullable = false)
    private AsientoViaje asiento;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "cliente_id", nullable = false)
    private Cliente cliente;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "vendedor_id", nullable = false)
    private Usuario vendedor;

    @Column(name = "precio_pagado", precision = 10, scale = 2, nullable = false)
    private java.math.BigDecimal precioPagado;

    @Column(name = "fecha_venta", nullable = false)
    private LocalDateTime fechaVenta;

    @Column(name = "qr_code", length = 255, unique = true, nullable = false)
    private String qrCode;

    @Column(name = "pdf_s3_key", length = 255)
    private String pdfS3Key;

    @Column(name = "recordatorio_enviado", nullable = false)
    @Builder.Default
    private boolean recordatorioEnviado = false;

    @Column(name = "monto_devuelto", precision = 10, scale = 2)
    private java.math.BigDecimal montoDevuelto;

    @Column(name = "fecha_cancelacion")
    private LocalDateTime fechaCancelacion;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private EstadoBoleto estado = EstadoBoleto.VIGENTE;

    @OneToOne(mappedBy = "boleto")
    private Factura factura;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist protected void onCreate() { createdAt = LocalDateTime.now(); updatedAt = LocalDateTime.now(); }
    @PreUpdate protected void onUpdate() { updatedAt = LocalDateTime.now(); }
}
