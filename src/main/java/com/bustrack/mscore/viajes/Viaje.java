package com.bustrack.mscore.viajes;

import com.bustrack.mscore.buses.Bus;
import com.bustrack.mscore.choferes.Chofer;
import com.bustrack.mscore.common.enums.EstadoAsiento;
import com.bustrack.mscore.common.enums.EstadoViaje;
import com.bustrack.mscore.horarios.Horario;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "viajes", uniqueConstraints = @UniqueConstraint(columnNames = {"horario_id", "fecha"}))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Viaje {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "horario_id", nullable = false)
    private Horario horario;

    @Column(nullable = false)
    private String fecha;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "bus_id")
    private Bus bus;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "chofer_titular_id")
    private Chofer choferTitular;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "chofer_auxiliar_id")
    private Chofer choferAuxiliar;

    @Column(name = "carril_asignado", length = 10)
    private String carrilAsignado;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private EstadoViaje estado = EstadoViaje.PROGRAMADO;

    @Column(name = "hora_salida_real")
    private LocalDateTime horaSalidaReal;

    @Column(name = "hora_llegada_real")
    private LocalDateTime horaLlegadaReal;

    @OneToMany(mappedBy = "viaje", cascade = CascadeType.ALL)
    private List<AsientoViaje> asientos;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public int getTotalVendidos() {
        if (asientos == null) return 0;
        return (int) asientos.stream().filter(a -> a.getEstado() != EstadoAsiento.LIBRE).count();
    }

    public int getTotalLibres() {
        if (asientos == null) return 0;
        return (int) asientos.stream().filter(a -> a.getEstado() == EstadoAsiento.LIBRE).count();
    }

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
