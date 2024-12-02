package co.edu.ufps.controller;




import co.edu.ufps.Dto.FacturaRequestDTO;
import co.edu.ufps.entities.Compra;
import co.edu.ufps.services.FacturaService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class FacturaController {

    @Autowired
    private FacturaService facturaService;

    @PostMapping("/crear/{tiendaUuid}")
    public ResponseEntity<?> crearFactura(@PathVariable String tiendaUuid, @RequestBody FacturaRequestDTO facturaRequest) {
        try {
            Compra compra = facturaService.procesarFactura(tiendaUuid, facturaRequest);
            
            Map<String, Object> response = new HashMap<>();
            response.put("status", "success");
            response.put("message", "La factura se ha creado correctamente con el número: " + compra.getId());
            
            Map<String, String> data = new HashMap<>();
            data.put("numero", compra.getId().toString());
            data.put("total", compra.getTotal().toString());
            data.put("fecha", compra.getFecha().toString());
            
            response.put("data", data);
            
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("status", "error");
            errorResponse.put("message", e.getMessage());
            errorResponse.put("data", null);
            
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
        }
    }
}