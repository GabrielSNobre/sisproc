package com.sisproc.backend.equipe;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface MembroEquipeRepository
        extends JpaRepository<MembroEquipe, Long> {

    Optional<MembroEquipe> findByEquipeIdAndUsuarioId(
        Long equipeId,
        Long usuarioId
    );

    List<MembroEquipe> findAllByEquipeIdAndAtivoTrue(
        Long equipeId
    );

    List<MembroEquipe> findAllByUsuarioIdAndAtivoTrue(
        Long usuarioId
    );

    boolean existsByEquipeIdAndUsuarioIdAndAtivoTrue(
        Long equipeId,
        Long usuarioId
    );
}
