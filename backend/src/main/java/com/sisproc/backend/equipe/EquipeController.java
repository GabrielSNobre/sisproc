
package com.sisproc.backend.equipe;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

@RestController
@RequestMapping("/api/equipes")
@Validated
public class EquipeController {

    private final EquipeService equipeService;

    public EquipeController(EquipeService equipeService) {
        this.equipeService = equipeService;
    }

    @PostMapping
    public EquipeResposta criarEquipe(
            @Valid @RequestBody CriarEquipeRequisicao requisicao) {

        Equipe equipe = equipeService.criarEquipe(
                requisicao.nome(),
                requisicao.descricao(),
                requisicao.gestorId());

        return converterEquipe(equipe);
    }

    @GetMapping
    public List<EquipeResposta> listarEquipes() {
        return equipeService.listarEquipes()
                .stream()
                .map(EquipeController::converterEquipe)
                .toList();
    }

    @PostMapping("/{id}/membros")
    public MembroResposta adicionarMembro(
            @PathVariable @Positive Long id,
            @Valid @RequestBody AdicionarMembroRequisicao requisicao) {

        MembroEquipe membro = equipeService.adicionarMembro(
                id,
                requisicao.usuarioId());

        return converterMembro(membro);
    }

    @GetMapping("/{id}/membros")
    public List<MembroResposta> listarMembros(
            @PathVariable @Positive Long id) {

        return equipeService.listarMembros(id)
                .stream()
                .map(EquipeController::converterMembro)
                .toList();
    }


    @DeleteMapping("/{id}/membros/{usuarioId}")
    public void removerMembro(
            @PathVariable @Positive Long id,
            @PathVariable @Positive Long usuarioId) {

        equipeService.removerMembro(id, usuarioId);
    }

    private static EquipeResposta converterEquipe(Equipe equipe) {
        return new EquipeResposta(
                equipe.getId(),
                equipe.getNome(),
                equipe.getDescricao(),
                equipe.isAtivo(),
                equipe.getCriadoEm(),
                equipe.getGestor().getId(),
                equipe.getGestor().getNome(),
                equipe.getGestor().getEmail());
    }

    private static MembroResposta converterMembro(MembroEquipe membro) {
        return new MembroResposta(
                membro.getId(),
                membro.getEquipe().getId(),
                membro.getUsuario().getId(),
                membro.getUsuario().getNome(),
                membro.getUsuario().getEmail(),
                membro.isAtivo(),
                membro.getVinculadoEm());
    }

    public record CriarEquipeRequisicao(
            @NotNull @Size(min = 1, max = 120) String nome,
            @Size(max = 500) String descricao,
            @NotNull @Positive Long gestorId) {
    }

    public record AdicionarMembroRequisicao(
            @NotNull @Positive Long usuarioId) {
    }

    public record EquipeResposta(
            Long id,
            String nome,
            String descricao,
            boolean ativo,
            LocalDateTime criadoEm,
            Long gestorId,
            String nomeGestor,
            String emailGestor) {
    }

    public record MembroResposta(
            Long id,
            Long equipeId,
            Long usuarioId,
            String nomeUsuario,
            String emailUsuario,
            boolean ativo,
            LocalDateTime vinculadoEm) {
    }
}
