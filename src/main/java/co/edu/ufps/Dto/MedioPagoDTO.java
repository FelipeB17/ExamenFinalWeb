package co.edu.ufps.Dto;

import lombok.Data;

@Data
public class MedioPagoDTO {
    private String tipo_pago;
    private String tipo_tarjeta;
    private Integer cuotas;
    private double valor;
}