package co.edu.ufps.services;



import co.edu.ufps.Dto.*;
import co.edu.ufps.entities.*;
import co.edu.ufps.repositories.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
        // 1. Validar y obtener la tienda
        Tienda tienda = tiendaRepository.findByUuid(tiendaUuid);
        if (tienda == null) {
            throw new RuntimeException("Tienda no encontrada");
        }

        // 2. Validar y obtener o crear el cliente
        Cliente cliente = obtenerOCrearCliente(facturaRequest.getCliente());

        // 3. Validar y obtener el vendedor
        Vendedor vendedor = vendedorRepository.findByDocumento(facturaRequest.getVendedor().getDocumento());
        if (vendedor == null) {
            throw new RuntimeException("El vendedor no existe en la tienda");
        }

        // 4. Validar y obtener el cajero
        Cajero cajero = cajeroRepository.findByToken(facturaRequest.getCajero().getToken());
        if (cajero == null) {
            throw new RuntimeException("El token no corresponde a ningún cajero en la tienda");
        }
        if (!cajero.getTienda().getId().equals(tienda.getId())) {
            throw new RuntimeException("El cajero no está asignado a esta tienda");
        }

        // 5. Crear la compra
        Compra compra = new Compra();
        compra.setCliente(cliente);
        compra.setTienda(tienda);
        compra.setVendedor(vendedor);
        compra.setCajero(cajero);
        compra.setFecha(LocalDateTime.now());
        compra.setImpuestos(BigDecimal.valueOf(facturaRequest.getImpuesto()));

        // 6. Procesar los productos y crear los detalles de compra
        List<DetallesCompra> detallesCompraList = new ArrayList<>();
        BigDecimal total = BigDecimal.ZERO;

        for (ProductoDTO productoDTO : facturaRequest.getProductos()) {
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

            // Actualizar la cantidad del producto
            producto.setCantidad(producto.getCantidad() - productoDTO.getCantidad());
            productoRepository.save(producto);
        }

        compra.setTotal(total);
        compra = compraRepository.save(compra);

        // Guardar los detalles de compra
        detallesCompraRepository.saveAll(detallesCompraList);

        // 7. Procesar los pagos
        procesarPagos(compra, facturaRequest.getMedios_pago());

        return compra;
    }

    private Cliente obtenerOCrearCliente(ClienteDTO clienteDTO) {
        Cliente cliente = clienteRepository.findByDocumentoAndTipoDocumento_Nombre(clienteDTO.getDocumento(), clienteDTO.getTipo_documento());
        if (cliente == null) {
            TipoDocumento tipoDocumento = tipoDocumentoRepository.findByNombre(clienteDTO.getTipo_documento());
            if (tipoDocumento == null) {
                throw new RuntimeException("Tipo de documento no válido");
            }
            cliente = new Cliente();
            cliente.setDocumento(clienteDTO.getDocumento());
            cliente.setNombre(clienteDTO.getNombre());
            cliente.setTipoDocumento(tipoDocumento);
            cliente = clienteRepository.save(cliente);
        }
        return cliente;
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
}
