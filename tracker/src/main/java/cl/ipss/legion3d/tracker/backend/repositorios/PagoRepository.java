package cl.ipss.legion3d.tracker.backend.repositorios;

import cl.ipss.legion3d.tracker.backend.entidades.Pago;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface PagoRepository extends JpaRepository<Pago, Long> {

    @Query("SELECT COALESCE(SUM(p.monto), 0.0) FROM Pago p WHERE p.pedido.id = :pedidoId")
    Double sumarAbonosPorPedido(@Param("pedidoId") Long pedidoId);

    long countByPedidoId(Long pedidoId);
}
