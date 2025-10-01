package com.sivil.systeam.repository;

import com.sivil.systeam.entity.Venta;
import com.sivil.systeam.enums.EstadoVenta;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface VentaRepository extends JpaRepository<Venta, Integer> {

    List<Venta> findByEstado(EstadoVenta estado);

    @Query("SELECT COUNT(v) > 0 FROM Venta v WHERE v.numero_factura = :numeroFactura")
    boolean existsByNumero_Factura(@Param("numeroFactura") String numeroFactura);

    @Query("SELECT v.numero_factura FROM Venta v WHERE v.numero_factura LIKE :prefijo% ORDER BY v.numero_factura DESC")
    List<String> findTopByNumeroFacturaStartingWith(@Param("prefijo") String prefijo);


    @Query("SELECT v FROM Venta v WHERE v.estado = :estado ORDER BY v.fecha_venta DESC")
    List<Venta> findByEstadoOrderByFechaVentaDesc(@Param("estado") EstadoVenta estado);



}