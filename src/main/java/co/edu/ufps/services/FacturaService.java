package co.edu.ufps.services;


import co.edu.ufps.Dto.*;
import co.edu.ufps.entities.*;
import co.edu.ufps.repositories.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import co.edu.ufps.exceptions.FacturaException;
import org.springframework.http.HttpStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class FacturaService {

    @Autowired
    private TiendaRepository tiendaRepository;
    @Autowired
    private ClienteRepository clienteRepository;
    @Autowired
    private ProductoRepository productoRepository;
    @Autowired
    private VendedorRepository vendedorRepository;
    @Autowired
    private CajeroRepository cajeroRepository;
    @Autowired
    private CompraRepository compraRepository;
    @Autowired
    private DetallesCompraRepository detallesCompraRepository;
    @Autowired
    private PagoRepository pagoRepository;
    @Autowired
    private TipoPagoRepository tipoPagoRepository;
    @Autowired
    private TipoDocumentoRepository tipoDocumentoRepository;

    @Transactional
    public Compra procesarFactura(String tiendaUuid, FacturaRequestDTO facturaRequest) {
        validarDatosFactura(facturaRequest);

        Tienda tienda = obtenerTienda(tiendaUuid);
        Cliente cliente = obtenerOCrearCliente(facturaRequest.getCliente());
        Vendedor vendedor = obtenerVendedor(facturaRequest.getVendedor(), tienda);
        Cajero cajero = obtenerCajero(facturaRequest.getCajero(), tienda);

        Compra compra = crearCompra(tienda, cliente, vendedor, cajero, facturaRequest);
        List<DetallesCompra> detallesCompraList = procesarProductos(compra, facturaRequest.getProductos());
        procesarPagos(compra, facturaRequest.getMedios_pago());

        compra = compraRepository.save(compra);
        detallesCompraRepository.saveAll(detallesCompraList);

        return compra;
    }

    private void validarDatosFactura(FacturaRequestDTO facturaRequest) {
    	// Validar cliente
        if (facturaRequest.getCliente() == null) {
            throw new FacturaException("No hay información del cliente", HttpStatus.NOT_FOUND);
        }

        // Validar vendedor
        if (facturaRequest.getVendedor() == null) {
            throw new FacturaException("No hay información del vendedor", HttpStatus.NOT_FOUND);
        }

        // Validar cajero
        if (facturaRequest.getCajero() == null) {
            throw new FacturaException("No hay información del cajero", HttpStatus.NOT_FOUND);
        }

        // Validar productos
        if (facturaRequest.getProductos() == null || facturaRequest.getProductos().isEmpty()) {
            throw new FacturaException("No hay productos asignados para esta compra", HttpStatus.NOT_FOUND);
        }

        // Validar medios de pago
        if (facturaRequest.getMedios_pago() == null || facturaRequest.getMedios_pago().isEmpty()) {
            throw new FacturaException("No hay medios de pagos asignados para esta compra", HttpStatus.NOT_FOUND);
        }
    }

    private Tienda obtenerTienda(String tiendaUuid) {
        Tienda tienda = tiendaRepository.findByUuid(tiendaUuid);
        if (tienda == null) {
            throw new RuntimeException("Tienda no encontrada");
        }
        return tienda;
    }

    private Cliente obtenerOCrearCliente(ClienteDTO clienteDTO) {
        TipoDocumento tipoDocumento = tipoDocumentoRepository.findByNombre(clienteDTO.getTipo_documento());
        if (tipoDocumento == null) {
            throw new RuntimeException("Tipo de documento no válido");
        }

        Cliente cliente = clienteRepository.findByDocumentoAndTipoDocumento(clienteDTO.getDocumento(), tipoDocumento);
        if (cliente == null) {
            cliente = new Cliente();
            cliente.setDocumento(clienteDTO.getDocumento());
            cliente.setNombre(clienteDTO.getNombre());
            cliente.setTipoDocumento(tipoDocumento);
            cliente = clienteRepository.save(cliente);
        }
        return cliente;
    }

    private Vendedor obtenerVendedor(VendedorDTO vendedorDTO, Tienda tienda) {
        Vendedor vendedor = vendedorRepository.findByDocumento(vendedorDTO.getDocumento());
        if (vendedor == null) {
            throw new RuntimeException("El vendedor no existe en la tienda");
        }
        return vendedor;
    }

    private Cajero obtenerCajero(CajeroDTO cajeroDTO, Tienda tienda) {
        Cajero cajero = cajeroRepository.findByToken(cajeroDTO.getToken());
        if (cajero == null) {
            throw new RuntimeException("El token no corresponde a ningún cajero en la tienda");
        }
        if (!cajero.getTienda().getId().equals(tienda.getId())) {
            throw new RuntimeException("El cajero no está asignado a esta tienda");
        }
        return cajero;
    }

    private Compra crearCompra(Tienda tienda, Cliente cliente, Vendedor vendedor, Cajero cajero, FacturaRequestDTO facturaRequest) {
        Compra compra = new Compra();
        compra.setCliente(cliente);
        compra.setTienda(tienda);
        compra.setVendedor(vendedor);
        compra.setCajero(cajero);
        compra.setFecha(LocalDateTime.now());
        compra.setImpuestos(BigDecimal.valueOf(facturaRequest.getImpuesto()));
        return compra;
    }

    private List<DetallesCompra> procesarProductos(Compra compra, List<ProductoDTO> productosDTO) {
        List<DetallesCompra> detallesCompraList = new ArrayList<>();
        BigDecimal total = BigDecimal.ZERO;

        for (ProductoDTO productoDTO : productosDTO) {
            Producto producto = productoRepository.findByReferencia(productoDTO.getReferencia());
            if (producto == null) {
                throw new RuntimeException("La referencia del producto " + productoDTO.getReferencia() + " no existe, por favor revisar los datos");
            }
            if (producto.getCantidad() < productoDTO.getCantidad()) {
                throw new RuntimeException("La cantidad a comprar supera el máximo del producto en tienda");
            }

            DetallesCompra detallesCompra = new DetallesCompra();
            detallesCompra.setCompra(compra);
            detallesCompra.setProducto(producto);
            detallesCompra.setCantidad(productoDTO.getCantidad());
            detallesCompra.setPrecio(producto.getPrecio());
            detallesCompra.setDescuento(BigDecimal.valueOf(productoDTO.getDescuento()));

            BigDecimal subtotal = producto.getPrecio()
                    .multiply(BigDecimal.valueOf(productoDTO.getCantidad()))
                    .subtract(BigDecimal.valueOf(productoDTO.getDescuento()));
            total = total.add(subtotal);

            detallesCompraList.add(detallesCompra);

            producto.setCantidad(producto.getCantidad() - productoDTO.getCantidad());
            productoRepository.save(producto);
        }

        compra.setTotal(total);
        return detallesCompraList;
    }

    private void procesarPagos(Compra compra, List<MedioPagoDTO> mediosPago) {
        BigDecimal totalPagado = BigDecimal.ZERO;
        for (MedioPagoDTO medioPagoDTO : mediosPago) {
            TipoPago tipoPago = tipoPagoRepository.findByNombre(medioPagoDTO.getTipo_pago());
            if (tipoPago == null) {
                throw new RuntimeException("Tipo de pago no permitido en la tienda");
            }

            Pago pago = new Pago();
            pago.setCompra(compra);
            pago.setTipoPago(tipoPago);
            pago.setTarjetaTipo(medioPagoDTO.getTipo_tarjeta());
            pago.setValor(BigDecimal.valueOf(medioPagoDTO.getValor()));
            pago.setCuotas(medioPagoDTO.getCuotas());

            pagoRepository.save(pago);

            totalPagado = totalPagado.add(pago.getValor());
        }

        if (totalPagado.compareTo(compra.getTotal()) != 0) {
            throw new RuntimeException("El valor de la factura no coincide con el valor total de los pagos");
        }
    }
    
    
    private Producto validarProducto(String referencia, int cantidadSolicitada) {
        Producto producto = productoRepository.findByReferencia(referencia);
        if (producto == null) {
            throw new FacturaException(
                "La referencia del producto " + referencia + " no existe, por favor revisar los datos",
                HttpStatus.NOT_FOUND
            );
        }
        if (producto.getCantidad() < cantidadSolicitada) {
            throw new FacturaException(
                "La cantidad a comprar supera el máximo del producto en tienda",
                HttpStatus.FORBIDDEN
            );
        }
        return producto;
    }

    private void validarPago(TipoPago tipoPago, Cajero cajero, Tienda tienda) {
        if (tipoPago == null) {
            throw new FacturaException(
                "Tipo de pago no permitido en la tienda",
                HttpStatus.FORBIDDEN
            );
        }
        if (cajero == null) {
            throw new FacturaException(
                "El token no corresponde a ningún cajero en la tienda",
                HttpStatus.NOT_FOUND
            );
        }
        if (!cajero.getTienda().getId().equals(tienda.getId())) {
            throw new FacturaException(
                "El cajero no está asignado a esta tienda",
                HttpStatus.FORBIDDEN
            );
        }
    }

    private void validarVendedor(Vendedor vendedor) {
        if (vendedor == null) {
            throw new FacturaException(
                "El vendedor no existe en la tienda",
                HttpStatus.NOT_FOUND
            );
        }
    }

    private void validarTotalPagos(BigDecimal totalFactura, BigDecimal totalPagado) {
        if (totalPagado.compareTo(totalFactura) != 0) {
            throw new FacturaException(
                "El valor de la factura no coincide con el valor total de los pagos",
                HttpStatus.FORBIDDEN
            );
        }
    }
}