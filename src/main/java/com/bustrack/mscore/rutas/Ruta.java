package com.bustrack.mscore.rutas;

import com.bustrack.mscore.terminales.Terminal;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "rutas", uniqueConstraints = @UniqueConstraint(columnNames = {"terminal_origen_id", "terminal_destino_id"}))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Ruta {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "terminal_origen_id", nullable = false)
    private Terminal origen;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "terminal_destino_id", nullable = false)
    private Terminal destino;

    @Column(name = "distancia_km", precision = 8, scale = 2)
    private BigDecimal distanciaKm;

    @Column(name = "duracion_estimada_min")
    private Integer duracionEstimadaMin;

    @Column(nullable = false)
    @Builder.Default
    private boolean activa = true;

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
