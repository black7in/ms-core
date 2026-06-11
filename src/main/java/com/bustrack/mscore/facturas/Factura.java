package com.bustrack.mscore.facturas;

import com.bustrack.mscore.boletos.Boleto;
import com.bustrack.mscore.common.enums.BlockchainEstado;
import com.bustrack.mscore.common.enums.EstadoFactura;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity @Table(name = "facturas")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Factura {

    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @OneToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "boleto_id", unique = true, nullable = false)
    private Boleto boleto;

    @Column(name = "numero_factura", length = 20, unique = true, nullable = false)
    private String numeroFactura;

    @Column(length = 20) private String nit;

    @Column(name = "razon_social", length = 150) private String razonSocial;

    @Column(name = "nombre_cliente", length = 100, nullable = false)
    private String nombreCliente;

    @Column(precision = 10, scale = 2, nullable = false)
    private java.math.BigDecimal monto;

    @Column(nullable = false)
    private LocalDateTime fecha;

    @Column(name = "pdf_s3_key", length = 255) private String pdfS3Key;

    @Column(name = "hash_sha256", length = 64) private String hashSha256;

    @Column(name = "blockchain_tx_hash", length = 255) private String blockchainTxHash;

    @Enumerated(EnumType.STRING) @Column(name = "blockchain_estado", nullable = false)
    @Builder.Default private BlockchainEstado blockchainEstado = BlockchainEstado.PENDIENTE;

    @Enumerated(EnumType.STRING) @Column(nullable = false)
    @Builder.Default private EstadoFactura estado = EstadoFactura.VIGENTE;

    @Column(name = "created_at", nullable = false, updatable = false) private LocalDateTime createdAt;
    @Column(name = "updated_at", nullable = false) private LocalDateTime updatedAt;

    @PrePersist protected void onCreate() { createdAt = LocalDateTime.now(); updatedAt = LocalDateTime.now(); fecha = LocalDateTime.now(); }
    @PreUpdate protected void onUpdate() { updatedAt = LocalDateTime.now(); }
}
