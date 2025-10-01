package com.sivil.systeam.service;

import com.sivil.systeam.entity.Libro;
import com.sivil.systeam.repository.LibroRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class LibroService {

    private final LibroRepository libroRepository;

    @Autowired
    public LibroService(LibroRepository libroRepository) {
        this.libroRepository = libroRepository;
    }

    // Trae todos los libros activos con stock > 0
    public List<Libro> listarTodosActivosConStock() {
        return libroRepository.findByEstadoAndCantidad_stockGreaterThan(
                com.sivil.systeam.enums.Estado.activo, 0);
    }
}
