package com.bustrack.mscore.terminales;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
@Transactional
public class TerminalService {

    private final TerminalRepository repo;

    public TerminalService(TerminalRepository repo) { this.repo = repo; }

    public List<Terminal> findAll() { return repo.findAll(); }

    public Terminal findById(String id) {
        return repo.findById(id).orElseThrow(() -> new RuntimeException("Terminal no encontrada"));
    }

    public Terminal create(Terminal terminal) {
        return repo.save(terminal);
    }

    public Terminal update(String id, Terminal data) {
        var t = findById(id);
        if (data.getNombre() != null) t.setNombre(data.getNombre());
        if (data.getCiudad() != null) t.setCiudad(data.getCiudad());
        if (data.getDireccion() != null) t.setDireccion(data.getDireccion());
        return repo.save(t);
    }
}
