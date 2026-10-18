package com.sisproc.backend.processo;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface DocumentoProcessoRepository extends JpaRepository<DocumentoProcesso, Long> {
    List<DocumentoProcesso> findByTarefaIdOrderByCriadoEmAscIdAsc(Long tarefaId);
    List<DocumentoProcesso> findByEquipeDestinoIdOrderByCriadoEmDescIdDesc(Long equipeId);
}
