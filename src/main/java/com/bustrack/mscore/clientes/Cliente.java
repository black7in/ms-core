package com.bustrack.mscore.clientes;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "clientes")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Cliente {

    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(length = 20, unique = true, nullable = false)
    private String ci;

    @Column(length = 100, nullable = false)
    private String nombre;

    @Column(length = 20) private String telefono;

    @Column(length = 150) private String email;

    @Column(name = "fecha_registro", nullable = false, updatable = false)
    private LocalDateTime fechaRegistro;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        fechaRegistro = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() { updatedAt = LocalDateTime.now(); }
}
