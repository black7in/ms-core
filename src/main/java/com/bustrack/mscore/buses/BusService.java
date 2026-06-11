package com.bustrack.mscore.buses;

import com.bustrack.mscore.common.enums.EstadoBus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
@Transactional
public class BusService {

    private final BusRepository repo;

    public BusService(BusRepository repo) { this.repo = repo; }

    public List<Bus> findAll(EstadoBus estado) {
        return estado != null ? repo.findByEstadoMecanico(estado) : repo.findAll();
    }

    public Bus findById(String id) {
        return repo.findById(id).orElseThrow(() -> new RuntimeException("Bus no encontrado"));
    }

    public Bus create(Bus bus) { return repo.save(bus); }

    public Bus update(String id, Bus data) {
        var b = findById(id);
        if (data.getPlaca() != null) b.setPlaca(data.getPlaca());
        if (data.getMarca() != null) b.setMarca(data.getMarca());
        if (data.getModelo() != null) b.setModelo(data.getModelo());
        if (data.getAnio() != null) b.setAnio(data.getAnio());
        if (data.getCapacidad() != null) b.setCapacidad(data.getCapacidad());
        if (data.getEstadoMecanico() != null) b.setEstadoMecanico(data.getEstadoMecanico());
        return repo.save(b);
    }

    public Bus cambiarEstado(String id, EstadoBus estado) {
        var b = findById(id);
        b.setEstadoMecanico(estado);
        return repo.save(b);
    }
}
