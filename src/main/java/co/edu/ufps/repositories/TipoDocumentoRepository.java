package co.edu.ufps.repositories;

import co.edu.ufps.entities.TipoDocumento;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TipoDocumentoRepository extends JpaRepository<TipoDocumento, Integer> {
    TipoDocumento findByNombre(String nombre);
}
