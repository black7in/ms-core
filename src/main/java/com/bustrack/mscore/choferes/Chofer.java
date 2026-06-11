package com.bustrack.mscore.choferes;

import com.bustrack.mscore.common.enums.EstadoChofer;
import com.bustrack.mscore.usuarios.Usuario;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "choferes")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Chofer {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @OneToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "usuario_id", unique = true)
    private Usuario usuario;

    @Column(length = 20, unique = true, nullable = false)
    private String ci;

    @Column(length = 100, nullable = false)
    private String nombre;

    @Column(length = 20)
    private String telefono;

    @Column(name = "licencia_numero", length = 30, unique = true, nullable = false)
    private String licenciaNumero;

    @Column(name = "licencia_categoria", length = 10, nullable = false)
    private String licenciaCategoria;

    @Column(name = "licencia_vence", nullable = false)
    private String licenciaVence;

    @Column(name = "foto_perfil_s3_key", length = 255)
    private String fotoPerfilS3Key;

    @Column(name = "foto_facial_s3_key", length = 255)
    private String fotoFacialS3Key;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private EstadoChofer estado = EstadoChofer.ACTIVO;

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
