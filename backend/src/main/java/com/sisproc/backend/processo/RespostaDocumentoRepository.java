package com.sisproc.backend.processo;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface RespostaDocumentoRepository extends JpaRepository<RespostaDocumento, Long> {
    List<RespostaDocumento> findByDocumentoIdOrderByCriadoEmAscIdAsc(Long documentoId);
}
