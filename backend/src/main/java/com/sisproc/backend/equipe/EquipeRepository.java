package com.sisproc.backend.equipe;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface EquipeRepository extends JpaRepository<Equipe, Long> {

    boolean existsByNomeIgnoreCase(String nome);

    List<Equipe> findAllByOrderByNomeAsc();
}
