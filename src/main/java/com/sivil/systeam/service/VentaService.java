package com.sivil.systeam.service;

import com.sivil.systeam.entity.Venta;
import com.sivil.systeam.enums.EstadoVenta;
import com.sivil.systeam.repository.VentaRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class VentaService {

    private final VentaRepository ventaRepository;

    @Autowired
    public VentaService(VentaRepository ventaRepository) {
        this.ventaRepository = ventaRepository;
    }

    // Listar ventas finalizadas
    public List<Venta> listarVentasFinalizadas() {
        return ventaRepository.findByEstadoOrderByFechaVentaDesc(EstadoVenta.activa);

    }

    // Obtener venta por ID
    public Optional<Venta> obtenerVentaPorId(Integer id) {
        return ventaRepository.findById(id);
    }

    // Guardar o actualizar venta
    public Venta guardarVenta(Venta venta) {
        return ventaRepository.save(venta);
    }
}
