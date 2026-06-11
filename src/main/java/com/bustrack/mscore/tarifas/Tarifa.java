package com.bustrack.mscore.tarifas;

import com.bustrack.mscore.common.enums.TipoDia;
import com.bustrack.mscore.rutas.Ruta;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "tarifas")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Tarifa {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "ruta_id", nullable = false)
    private Ruta ruta;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_dia", nullable = false)
    private TipoDia tipoDia;

    @Column(name = "precio_base", precision = 10, scale = 2, nullable = false)
    private BigDecimal precioBase;

    @Column(name = "vigente_desde", nullable = false)
    private String vigenteDesde;

    @Column(name = "vigente_hasta")
    private String vigenteHasta;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
