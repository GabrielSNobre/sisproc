package com.sisproc.backend.tarefa;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TarefaRepository extends JpaRepository<Tarefa, Long> {
    List<Tarefa> findByEquipeIdOrderByCriadoEmDesc(Long equipeId);
    List<Tarefa> findByResponsavelIdOrderByCriadoEmDesc(Long responsavelId);
}
