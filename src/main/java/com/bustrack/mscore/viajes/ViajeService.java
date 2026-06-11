package com.bustrack.mscore.viajes;

import com.bustrack.mscore.buses.BusRepository;
import com.bustrack.mscore.choferes.ChoferRepository;
import com.bustrack.mscore.common.enums.*;
import com.bustrack.mscore.horarios.Horario;
import com.bustrack.mscore.horarios.HorarioRepository;
import com.bustrack.mscore.push.ExpoPushService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.*;

@Service
@Transactional
public class ViajeService {

    private final ViajeRepository viajeRepo;
    private final AsientoViajeRepository asientoRepo;
    private final HorarioRepository horarioRepo;
    private final BusRepository busRepo;
    private final ChoferRepository choferRepo;
    private final ExpoPushService expoPushService;

    public ViajeService(ViajeRepository viajeRepo, AsientoViajeRepository asientoRepo,
                        HorarioRepository horarioRepo, BusRepository busRepo,
                        ChoferRepository choferRepo, ExpoPushService expoPushService) {
        this.viajeRepo = viajeRepo;
        this.asientoRepo = asientoRepo;
        this.horarioRepo = horarioRepo;
        this.busRepo = busRepo;
        this.choferRepo = choferRepo;
        this.expoPushService = expoPushService;
    }

    public Viaje findById(String id) {
        return viajeRepo.findById(id).orElseThrow(() -> new RuntimeException("Viaje no encontrado"));
    }

    public Viaje crearViaje(String horarioId, String fecha, String busId, String choferId, String auxId, String carril) {
        var horario = horarioRepo.findById(horarioId)
                .orElseThrow(() -> new RuntimeException("Horario no encontrado"));
        if (!horario.isActivo()) throw new RuntimeException("Horario inactivo");

        var duplicados = viajeRepo.findByHorarioIdAndFecha(horarioId, fecha);
        if (!duplicados.isEmpty()) throw new RuntimeException("Ya existe un viaje para ese horario y fecha");

        if (busId != null) {
            var bus = busRepo.findById(busId).orElseThrow(() -> new RuntimeException("Bus no encontrado"));
            if (bus.getEstadoMecanico() != EstadoBus.OPERATIVO) throw new RuntimeException("Bus no operativo");
        }
        if (choferId != null) {
            var c = choferRepo.findById(choferId).orElseThrow(() -> new RuntimeException("Chofer no encontrado"));
            if (c.getEstado() != EstadoChofer.ACTIVO) throw new RuntimeException("Chofer no activo");
        }

        var viaje = Viaje.builder().horario(horario).fecha(fecha).carrilAsignado(carril)
                .estado(EstadoViaje.PROGRAMADO).build();
        if (busId != null) viaje.setBus(busRepo.findById(busId).orElse(null));
        if (choferId != null) viaje.setChoferTitular(choferRepo.findById(choferId).orElse(null));
        if (auxId != null) viaje.setChoferAuxiliar(choferRepo.findById(auxId).orElse(null));

        var saved = viajeRepo.save(viaje);

        for (int i = 1; i <= 44; i++) {
            asientoRepo.save(AsientoViaje.builder().viaje(saved).numeroAsiento(i).estado(EstadoAsiento.LIBRE).build());
        }

        var result = findById(saved.getId());
        if (choferId != null) notifyChoferTitular(result);
        return result;
    }

    public Viaje iniciarViaje(String id) {
        var v = findById(id);
        if (v.getEstado() != EstadoViaje.PROGRAMADO) throw new RuntimeException("Solo viajes programados");
        v.setEstado(EstadoViaje.EN_RUTA);
        v.setHoraSalidaReal(LocalDateTime.now());
        return viajeRepo.save(v);
    }

    public Viaje finalizarViaje(String id) {
        var v = findById(id);
        if (v.getEstado() != EstadoViaje.EN_RUTA) throw new RuntimeException("Solo viajes en ruta");
        v.setEstado(EstadoViaje.FINALIZADO);
        v.setHoraLlegadaReal(LocalDateTime.now());
        return viajeRepo.save(v);
    }

    public Viaje cancelarViaje(String id) {
        var v = findById(id);
        if (v.getEstado() != EstadoViaje.PROGRAMADO) throw new RuntimeException("Solo viajes programados");
        v.setEstado(EstadoViaje.CANCELADO);
        return viajeRepo.save(v);
    }

    public Viaje actualizarViaje(String id, String busId, String choferId, String auxId, String carril) {
        var v = findById(id);
        if (busId != null) v.setBus(busRepo.findById(busId).orElse(null));
        if (choferId != null) v.setChoferTitular(choferRepo.findById(choferId).orElse(null));
        if (auxId != null) v.setChoferAuxiliar(choferRepo.findById(auxId).orElse(null));
        if (carril != null) v.setCarrilAsignado(carril);
        viajeRepo.save(v);
        var updated = findById(id);
        if (choferId != null) notifyChoferTitular(updated);
        return updated;
    }

    private void notifyChoferTitular(Viaje viaje) {
        var chofer = viaje.getChoferTitular();
        if (chofer == null || chofer.getUsuario() == null) return;
        var pushToken = chofer.getUsuario().getPushToken();
        if (pushToken == null || pushToken.isBlank()) return;
        var ruta = viaje.getHorario().getRuta();
        expoPushService.notifyViajeAsignado(
                pushToken, viaje.getId(),
                ruta.getOrigen().getCiudad(),
                ruta.getDestino().getCiudad(),
                viaje.getFecha(),
                viaje.getHorario().getHoraSalida(),
                viaje.getCarrilAsignado()
        );
    }

    public List<Viaje> generarViajesDelDia(String fecha) {
        var diaSemana = java.time.LocalDate.parse(fecha).getDayOfWeek().getValue();
        var creados = new ArrayList<Viaje>();
        for (var h : horarioRepo.findByActivoTrue()) {
            if (h.getDiasSemana() == null || !h.getDiasSemana().contains(diaSemana)) continue;
            var existentes = viajeRepo.findByHorarioIdAndFecha(h.getId(), fecha);
            if (!existentes.isEmpty()) continue;
            var v = Viaje.builder().horario(h).fecha(fecha).estado(EstadoViaje.PROGRAMADO).build();
            var saved = viajeRepo.save(v);
            for (int i = 1; i <= 44; i++)
                asientoRepo.save(AsientoViaje.builder().viaje(saved).numeroAsiento(i).estado(EstadoAsiento.LIBRE).build());
            creados.add(findById(saved.getId()));
        }
        return creados;
    }
}
