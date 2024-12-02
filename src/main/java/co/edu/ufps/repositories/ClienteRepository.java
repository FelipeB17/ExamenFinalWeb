package co.edu.ufps.repositories;

import co.edu.ufps.entities.Cliente;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ClienteRepository extends JpaRepository<Cliente, Integer> {
    Cliente findByDocumentoAndTipoDocumento_Nombre(String documento, String tipoDocumentoNombre);
}
