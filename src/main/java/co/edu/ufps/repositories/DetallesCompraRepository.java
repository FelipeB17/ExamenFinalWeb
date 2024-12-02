package co.edu.ufps.repositories;


import co.edu.ufps.entities.DetallesCompra;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DetallesCompraRepository extends JpaRepository<DetallesCompra, Integer> {
}