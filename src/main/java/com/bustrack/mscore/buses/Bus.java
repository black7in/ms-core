package com.bustrack.mscore.buses;

import com.bustrack.mscore.common.enums.EstadoBus;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "buses")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Bus {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(length = 15, unique = true, nullable = false)
    private String placa;

    @Column(length = 50, nullable = false)
    private String marca;

    @Column(length = 50, nullable = false)
    private String modelo;

    @Column(nullable = false)
    private Integer anio;

    @Column(name = "numero_carriles", nullable = false)
    @Builder.Default
    private Integer numeroCarriles = 2;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado_mecanico", nullable = false)
    @Builder.Default
    private EstadoBus estadoMecanico = EstadoBus.OPERATIVO;

    @Column(name = "foto_s3_key", length = 255)
    private String fotoS3Key;

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
