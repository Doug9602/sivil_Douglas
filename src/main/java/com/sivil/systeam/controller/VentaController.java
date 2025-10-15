package com.sivil.systeam.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sivil.systeam.entity.*;
import com.sivil.systeam.repository.*;
import com.sivil.systeam.service.*;
import com.sivil.systeam.dto.VentaTemporalDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpSession;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

@Controller
@RequestMapping("/ventas")
public class VentaController {

    @Autowired
    private LibroRepository libroRepository;

    @Autowired
    private VentaRepository ventaRepository;

    @Autowired
    private DetalleVentaRepository detalleVentaRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private NumeracionFacturaService numeracionService;

    @Autowired
    private VentaService ventaService;

    @Autowired
    private LibroService libroService;

    @GetMapping("/crear")
    public String mostrarFormularioCrearVenta(Model model) {
        Venta venta = new Venta();
        String numeroFactura = numeracionService.generarNumeroFactura(ventaRepository);
        venta.setNumero_factura(numeroFactura);

        model.addAttribute("venta", venta);
        model.addAttribute("libros", libroRepository.findByEstadoAndCantidad_stockGreaterThan(com.sivil.systeam.enums.Estado.activo, 0));
        return "venta/crear-venta";
    }

    @GetMapping("/api/libros/{id}/stock")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> verificarStock(@PathVariable Integer id) {
        Optional<Libro> libroOpt = libroRepository.findById(id);
        if (libroOpt.isPresent()) {
            Map<String, Object> response = new HashMap<>();
            response.put("id", id);
            response.put("titulo", libroOpt.get().getTitulo());
            response.put("stock", libroOpt.get().getCantidad_stock());
            response.put("disponible", libroOpt.get().getCantidad_stock() > 0);
            return ResponseEntity.ok(response);
        }
        return ResponseEntity.notFound().build();
    }

    @GetMapping("/listar")
    public String listarVentas(Model model) {
        List<Venta> ventas = ventaService.listarVentasFinalizadas();
        model.addAttribute("ventas", ventas);
        model.addAttribute("totalVentas", ventas.size());
        model.addAttribute("ventasActivas", 0);
        model.addAttribute("ventasHoy", 0);
        model.addAttribute("promedioVenta", 0);
        return "venta/listar-ventas";
    }

    @GetMapping("/{id}/modificar")
    public String mostrarFormularioModificacion(@PathVariable("id") Integer id, Model model) {
        try {
            Optional<Venta> ventaOpt = ventaService.obtenerVentaPorId(id);
            if (ventaOpt.isEmpty()) {
                model.addAttribute("error", "Venta no encontrada");
                return "redirect:/ventas/listar";
            }

            Venta venta = ventaOpt.get();
            List<Libro> libros = libroService.listarTodosActivosConStock();

            model.addAttribute("venta", venta);
            model.addAttribute("libros", libros);
            model.addAttribute("modoEdicion", true);

            return "venta/modificar-venta";

        } catch (Exception e) {
            e.printStackTrace();
            model.addAttribute("error", "No se pudo cargar la venta para modificar");
            return "redirect:/ventas/listar";
        }
    }

    @PostMapping("/{id}/modificar")
    public String actualizarVenta(
            @PathVariable("id") Integer id,
            @ModelAttribute("venta") Venta ventaActualizada,
            @RequestParam Map<String, String> requestParams,
            @RequestParam(value = "descuento_aplicado", required = false, defaultValue = "0") BigDecimal descuentoAplicado,
            Model model) {

        try {
            Optional<Venta> ventaOpt = ventaRepository.findById(id);
            if (ventaOpt.isEmpty()) {
                model.addAttribute("error", "No se encontró la venta.");
                return "redirect:/ventas/listar";
            }

            Venta ventaExistente = ventaOpt.get();

            // Guardar los detalles originales para calcular diferencias de stock
            Map<Integer, Integer> cantidadesOriginales = new HashMap<>();
            for (DetalleVenta detalle : ventaExistente.getDetallesVenta()) {
                cantidadesOriginales.put(detalle.getLibro().getId_libro(), detalle.getCantidad());
            }

            // Actualizar datos del cliente
            ventaExistente.setNombre_cliente(ventaActualizada.getNombre_cliente());
            ventaExistente.setContacto_cliente(ventaActualizada.getContacto_cliente());
            ventaExistente.setIdentificacion_cliente(ventaActualizada.getIdentificacion_cliente());

            BigDecimal subtotal = BigDecimal.ZERO;
            List<DetalleVenta> detallesAEliminar = new ArrayList<>();
            Map<Integer, Integer> cambiosStock = new HashMap<>();

            // Procesar cada detalle de venta
            for (DetalleVenta detalle : ventaExistente.getDetallesVenta()) {
                String detalleId = String.valueOf(detalle.getId_detalle_venta());

                // Verificar si el detalle debe ser eliminado
                String eliminarParam = requestParams.get("eliminar[" + detalleId + "]");
                if ("true".equals(eliminarParam)) {
                    detallesAEliminar.add(detalle);
                    // Al eliminar, devolver el stock al libro
                    int libroId = detalle.getLibro().getId_libro();
                    cambiosStock.put(libroId, cambiosStock.getOrDefault(libroId, 0) + detalle.getCantidad());
                    continue;
                }

                // Actualizar cantidad si existe en los parámetros
                String cantidadParam = requestParams.get("cantidades[" + detalleId + "]");
                if (cantidadParam != null && !cantidadParam.trim().isEmpty()) {
                    try {
                        int cantidadNueva = Integer.parseInt(cantidadParam);
                        int cantidadOriginal = detalle.getCantidad();

                        // Validar stock antes de actualizar
                        Libro libro = detalle.getLibro();
                        int stockDisponible = libro.getCantidad_stock() + cantidadOriginal;

                        if (cantidadNueva > stockDisponible) {
                            model.addAttribute("error", "Stock insuficiente para: " + libro.getTitulo() +
                                    ". Stock disponible: " + stockDisponible);
                            return "redirect:/ventas/" + id + "/modificar";
                        }

                        // Calcular diferencia de stock
                        int diferencia = cantidadOriginal - cantidadNueva;
                        if (diferencia != 0) {
                            int libroId = libro.getId_libro();
                            cambiosStock.put(libroId, cambiosStock.getOrDefault(libroId, 0) + diferencia);
                        }

                        // Actualizar cantidad y subtotal
                        detalle.setCantidad(cantidadNueva);
                        BigDecimal subtotalItem = detalle.getPrecio_unitario().multiply(BigDecimal.valueOf(cantidadNueva));
                        detalle.setSubtotal_item(subtotalItem);

                        subtotal = subtotal.add(subtotalItem);

                        // GUARDAR EL DETALLE ACTUALIZADO EN LA BASE DE DATOS
                        detalleVentaRepository.save(detalle);

                    } catch (NumberFormatException e) {
                        subtotal = subtotal.add(detalle.getSubtotal_item());
                    }
                } else {
                    subtotal = subtotal.add(detalle.getSubtotal_item());
                }
            }

            // Eliminar detalles marcados para eliminación
            for (DetalleVenta detalle : detallesAEliminar) {
                ventaExistente.getDetallesVenta().remove(detalle);
                detalleVentaRepository.delete(detalle);
            }

            // Validar que la venta tenga al menos un detalle
            if (ventaExistente.getDetallesVenta().isEmpty()) {
                model.addAttribute("error", "La venta debe tener al menos un libro.");
                return "redirect:/ventas/" + id + "/modificar";
            }

            // VALIDAR QUE EL DESCUENTO NO SEA MAYOR AL SUBTOTAL
            if (descuentoAplicado.compareTo(subtotal) > 0) {
                model.addAttribute("error", "El descuento no puede ser mayor al subtotal de la venta.");
                return "redirect:/ventas/" + id + "/modificar";
            }

            // VALIDAR QUE EL DESCUENTO NO SEA NEGATIVO
            if (descuentoAplicado.compareTo(BigDecimal.ZERO) < 0) {
                model.addAttribute("error", "El descuento no puede ser negativo.");
                return "redirect:/ventas/" + id + "/modificar";
            }

            // RECALCULAR TOTALES CONSIDERANDO DESCUENTO
            BigDecimal baseImponible = subtotal.subtract(descuentoAplicado);
            BigDecimal impuestos = baseImponible.multiply(new BigDecimal("0.13"));
            BigDecimal total = baseImponible.add(impuestos);

            ventaExistente.setSubtotal(subtotal);
            ventaExistente.setDescuento_aplicado(descuentoAplicado);
            ventaExistente.setImpuestos(impuestos);
            ventaExistente.setTotal(total);

            // ACTUALIZAR STOCK DE LOS LIBROS
            for (Map.Entry<Integer, Integer> entry : cambiosStock.entrySet()) {
                Integer libroId = entry.getKey();
                Integer diferenciaStock = entry.getValue();

                Optional<Libro> libroOpt = libroRepository.findById(libroId);
                if (libroOpt.isPresent()) {
                    Libro libro = libroOpt.get();
                    int nuevoStock = libro.getCantidad_stock() + diferenciaStock;
                    if (nuevoStock < 0) {
                        model.addAttribute("error", "Error: Stock no puede ser negativo para " + libro.getTitulo());
                        return "redirect:/ventas/" + id + "/modificar";
                    }
                    libro.setCantidad_stock(nuevoStock);
                    libroRepository.save(libro);
                }
            }

            // Guardar los cambios de la venta
            ventaRepository.save(ventaExistente);

            model.addAttribute("ok", "Venta actualizada correctamente.");
            return "redirect:/ventas/listar";

        } catch (Exception e) {
            e.printStackTrace();
            model.addAttribute("error", "Error al actualizar la venta: " + e.getMessage());
            return "redirect:/ventas/" + id + "/modificar";
        }
    }

    @PostMapping("/crear")
    public String procesarFormularioCrearVenta(
            @ModelAttribute("venta") Venta venta,
            @RequestParam("librosData") String librosDataJson,
            @RequestParam(value = "descuento_aplicado", required = false, defaultValue = "0") BigDecimal descuentoAplicado,
            HttpSession session,
            Model model) {

        try {
            List<LibroVentaRequest> detallesRequest = objectMapper.readValue(
                    librosDataJson,
                    new TypeReference<List<LibroVentaRequest>>() {}
            );

            if (detallesRequest.isEmpty()) {
                model.addAttribute("error", "Debe agregar al menos un libro para realizar la venta.");
                model.addAttribute("libros", libroRepository.findByEstadoAndCantidad_stockGreaterThan(com.sivil.systeam.enums.Estado.activo, 0));
                return "venta/crear-venta";
            }

            Usuario vendedor = (Usuario) model.getAttribute("currentUser");
            if (vendedor == null) {
                model.addAttribute("error", "No se pudo identificar al vendedor. Por favor inicie sesión nuevamente.");
                model.addAttribute("libros", libroRepository.findByEstadoAndCantidad_stockGreaterThan(com.sivil.systeam.enums.Estado.activo, 0));
                return "venta/crear-venta";
            }

            for (LibroVentaRequest detalleRequest : detallesRequest) {
                Optional<Libro> libroOpt = libroRepository.findById(detalleRequest.getId());
                if (libroOpt.isEmpty()) {
                    model.addAttribute("error", "Libro no encontrado: " + detalleRequest.getId());
                    model.addAttribute("libros", libroRepository.findByEstadoAndCantidad_stockGreaterThan(com.sivil.systeam.enums.Estado.activo, 0));
                    return "venta/crear-venta";
                }

                Libro libro = libroOpt.get();
                if (libro.getCantidad_stock() < detalleRequest.getCantidad()) {
                    model.addAttribute("error", "Stock insuficiente para: " + libro.getTitulo() +
                            ". Stock disponible: " + libro.getCantidad_stock() +
                            ". Cantidad solicitada: " + detalleRequest.getCantidad());
                    model.addAttribute("libros", libroRepository.findByEstadoAndCantidad_stockGreaterThan(com.sivil.systeam.enums.Estado.activo, 0));
                    return "venta/crear-venta";
                }
            }

            BigDecimal subtotalVenta = BigDecimal.ZERO;
            for (LibroVentaRequest detalleRequest : detallesRequest) {
                BigDecimal subtotalItem = detalleRequest.getPrecio().multiply(BigDecimal.valueOf(detalleRequest.getCantidad()));
                subtotalVenta = subtotalVenta.add(subtotalItem);
            }

            // VALIDAR QUE EL DESCUENTO NO SEA MAYOR AL SUBTOTAL
            if (descuentoAplicado.compareTo(subtotalVenta) > 0) {
                model.addAttribute("error", "El descuento no puede ser mayor al subtotal de la venta.");
                model.addAttribute("libros", libroRepository.findByEstadoAndCantidad_stockGreaterThan(com.sivil.systeam.enums.Estado.activo, 0));
                return "venta/crear-venta";
            }

            // VALIDAR QUE EL DESCUENTO NO SEA NEGATIVO
            if (descuentoAplicado.compareTo(BigDecimal.ZERO) < 0) {
                model.addAttribute("error", "El descuento no puede ser negativo.");
                model.addAttribute("libros", libroRepository.findByEstadoAndCantidad_stockGreaterThan(com.sivil.systeam.enums.Estado.activo, 0));
                return "venta/crear-venta";
            }

            // CALCULAR BASE IMPONIBLE CONSIDERANDO DESCUENTO
            BigDecimal baseImponible = subtotalVenta.subtract(descuentoAplicado);
            BigDecimal impuestos = baseImponible.multiply(new BigDecimal("0.13"));
            BigDecimal totalVenta = baseImponible.add(impuestos);

            VentaTemporalDTO ventaTemporal = new VentaTemporalDTO();
            ventaTemporal.setNumeroFactura(numeracionService.generarNumeroFactura(ventaRepository));
            ventaTemporal.setVendedor(vendedor);
            ventaTemporal.setNombreCliente(venta.getNombre_cliente());
            ventaTemporal.setContactoCliente(venta.getContacto_cliente());
            ventaTemporal.setIdentificacionCliente(venta.getIdentificacion_cliente());
            ventaTemporal.setSubtotal(subtotalVenta);
            ventaTemporal.setDescuentoAplicado(descuentoAplicado);
            ventaTemporal.setImpuestos(impuestos);
            ventaTemporal.setTotal(totalVenta);
            ventaTemporal.setTipoPago(com.sivil.systeam.enums.MetodoPago.tarjeta);
            ventaTemporal.setEstado(com.sivil.systeam.enums.EstadoVenta.activa);
            ventaTemporal.setFechaVenta(LocalDateTime.now());

            List<VentaTemporalDTO.DetalleVentaTemporalDTO> detallesTemporal = new ArrayList<>();
            for (LibroVentaRequest detalleRequest : detallesRequest) {
                VentaTemporalDTO.DetalleVentaTemporalDTO detalleTemporal =
                        new VentaTemporalDTO.DetalleVentaTemporalDTO(
                                detalleRequest.getId(),
                                detalleRequest.getTitulo(),
                                detalleRequest.getCantidad(),
                                detalleRequest.getPrecio(),
                                detalleRequest.getPrecio().multiply(BigDecimal.valueOf(detalleRequest.getCantidad()))
                        );
                detallesTemporal.add(detalleTemporal);
            }
            ventaTemporal.setDetallesVenta(detallesTemporal);

            session.setAttribute("ventaPendiente", ventaTemporal);

            return "redirect:/pago/tarjeta?monto=" + totalVenta + "&ventaPendiente=true";

        } catch (Exception e) {
            e.printStackTrace();
            model.addAttribute("error", "Error al procesar la venta: " + e.getMessage());
            model.addAttribute("libros", libroRepository.findByEstadoAndCantidad_stockGreaterThan(com.sivil.systeam.enums.Estado.activo, 0));
            return "venta/crear-venta";
        }
    }

    // Clase interna para mapear el JSON del frontend
    public static class LibroVentaRequest {
        private Integer id;
        private String titulo;
        private BigDecimal precio;
        private Integer cantidad;

        public Integer getId() { return id; }
        public void setId(Integer id) { this.id = id; }

        public String getTitulo() { return titulo; }
        public void setTitulo(String titulo) { this.titulo = titulo; }

        public BigDecimal getPrecio() { return precio; }
        public void setPrecio(BigDecimal precio) { this.precio = precio; }

        public Integer getCantidad() { return cantidad; }
        public void setCantidad(Integer cantidad) { this.cantidad = cantidad; }
    }
}