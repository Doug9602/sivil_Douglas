package com.sivil.systeam.entity;

import com.sivil.systeam.enums.EstadoVenta;
import com.sivil.systeam.enums.MetodoPago;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "ventas")
public class Venta {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id_venta;

    @Column(unique = true, nullable = false, length = 50)
    private String numero_factura;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_vendedor", nullable = false,
            foreignKey = @ForeignKey(name = "fk_ventas_vendedor"))
    private Usuario vendedor;

    @Column(nullable = false, length = 150)
    private String nombre_cliente;

    @Column(length = 100)
    private String contacto_cliente;

    @Column(length = 50)
    private String identificacion_cliente;

    @Column(precision = 10, scale = 2, nullable = false)
    private BigDecimal subtotal;

    @Column(precision = 10, scale = 2)
    private BigDecimal descuento_aplicado = BigDecimal.ZERO;

    @Column(name = "impuestos", precision = 10, scale = 2)
    private BigDecimal impuestos = BigDecimal.ZERO;

    @Column(precision = 10, scale = 2, nullable = false)
    private BigDecimal total;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MetodoPago tipo_pago;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado")
    private EstadoVenta estado = EstadoVenta.activa;

    @Column(columnDefinition = "TEXT")
    private String motivo_inactivacion;

    @Column(name = "fecha_venta", insertable = false, updatable = false)
    private LocalDateTime fecha_venta;

    @Column(insertable = false, updatable = false)
    private LocalDateTime fecha_ultima_actualizacion;

    // Relaciones
    @OneToMany(mappedBy = "venta", cascade = CascadeType.ALL,
            fetch = FetchType.LAZY, orphanRemoval = true)
    private List<DetalleVenta> detallesVenta;

    @OneToMany(mappedBy = "venta", cascade = CascadeType.ALL,
            fetch = FetchType.LAZY, orphanRemoval = true)
    private List<Pago> pagos;

    // Constructores
    public Venta() {}

    public Venta(String numero_factura, Usuario vendedor, String nombre_cliente,
                 BigDecimal subtotal, BigDecimal total, MetodoPago tipo_pago) {
        this.numero_factura = numero_factura;
        this.vendedor = vendedor;
        this.nombre_cliente = nombre_cliente;
        this.subtotal = subtotal;
        this.total = total;
        this.tipo_pago = tipo_pago;
    }

    // Getters y Setters
    public Integer getId_venta() { return id_venta; }
    public void setId_venta(Integer id_venta) { this.id_venta = id_venta; }

    public String getNumero_factura() { return numero_factura; }
    public void setNumero_factura(String numero_factura) { this.numero_factura = numero_factura; }

    public Usuario getVendedor() { return vendedor; }
    public void setVendedor(Usuario vendedor) { this.vendedor = vendedor; }

    public String getNombre_cliente() { return nombre_cliente; }
    public void setNombre_cliente(String nombre_cliente) { this.nombre_cliente = nombre_cliente; }

    public String getContacto_cliente() { return contacto_cliente; }
    public void setContacto_cliente(String contacto_cliente) { this.contacto_cliente = contacto_cliente; }

    public String getIdentificacion_cliente() { return identificacion_cliente; }
    public void setIdentificacion_cliente(String identificacion_cliente) { this.identificacion_cliente = identificacion_cliente; }

    public BigDecimal getSubtotal() { return subtotal; }
    public void setSubtotal(BigDecimal subtotal) { this.subtotal = subtotal; }

    public BigDecimal getDescuento_aplicado() { return descuento_aplicado; }
    public void setDescuento_aplicado(BigDecimal descuento_aplicado) { this.descuento_aplicado = descuento_aplicado; }

    public BigDecimal getImpuestos() { return impuestos; }
    public void setImpuestos(BigDecimal impuestos) { this.impuestos = impuestos; }

    public BigDecimal getTotal() { return total; }
    public void setTotal(BigDecimal total) { this.total = total; }

    public MetodoPago getTipo_pago() { return tipo_pago; }
    public void setTipo_pago(MetodoPago tipo_pago) { this.tipo_pago = tipo_pago; }

    public EstadoVenta getEstado() { return estado; }
    public void setEstado(EstadoVenta estado) { this.estado = estado; }

    public String getMotivo_inactivacion() { return motivo_inactivacion; }
    public void setMotivo_inactivacion(String motivo_inactivacion) { this.motivo_inactivacion = motivo_inactivacion; }

    public LocalDateTime getFecha_venta() { return fecha_venta; }
    public void setFecha_venta(LocalDateTime fecha_venta) { this.fecha_venta = fecha_venta; }

    public LocalDateTime getFecha_ultima_actualizacion() { return fecha_ultima_actualizacion; }
    public void setFecha_ultima_actualizacion(LocalDateTime fecha_ultima_actualizacion) { this.fecha_ultima_actualizacion = fecha_ultima_actualizacion; }

    public List<DetalleVenta> getDetallesVenta() { return detallesVenta; }
    public void setDetallesVenta(List<DetalleVenta> detallesVenta) { this.detallesVenta = detallesVenta; }

    public List<Pago> getPagos() { return pagos; }
    public void setPagos(List<Pago> pagos) { this.pagos = pagos; }
}