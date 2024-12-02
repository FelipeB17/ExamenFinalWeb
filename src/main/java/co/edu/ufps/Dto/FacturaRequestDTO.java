package co.edu.ufps.Dto;


import lombok.Data;
import java.util.List;

@Data
public class FacturaRequestDTO {

	
	private double impuesto;
    private ClienteDTO cliente;
    private List<ProductoDTO> productos;
    private List<MedioPagoDTO> medios_pago;
    private VendedorDTO vendedor;
    private CajeroDTO cajero;
	
}
