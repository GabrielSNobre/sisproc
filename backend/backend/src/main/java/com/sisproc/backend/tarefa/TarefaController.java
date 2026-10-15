package com.sisproc.backend.tarefa;

import com.sisproc.backend.equipe.Equipe;
import com.sisproc.backend.usuario.Perfil;
import com.sisproc.backend.usuario.Usuario;
import com.sisproc.backend.usuario.UsuarioRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

@RestController
@RequestMapping("/api/tarefas")
public class TarefaController {
    private static final Set<String> PRIORIDADES = Set.of("BAIXA", "MEDIA", "ALTA");
    private static final Set<String> STATUS = Set.of("PENDENTE", "EM_ANDAMENTO", "CONCLUIDA", "CANCELADA");

    private final TarefaRepository tarefaRepository;
    private final UsuarioRepository usuarioRepository;
    private final EquipePermissaoService permissaoEquipe;

    public TarefaController(TarefaRepository tarefaRepository,
                            UsuarioRepository usuarioRepository,
                            EquipePermissaoService permissaoEquipe) {
        this.tarefaRepository = tarefaRepository;
        this.usuarioRepository = usuarioRepository;
        this.permissaoEquipe = permissaoEquipe;
    }

    /** Cria uma tarefa persistida, atribuída a integrante ativo da equipe do gestor. */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TarefaResposta criar(@Valid @RequestBody CriarTarefaRequisicao req,
                                Authentication authentication) {
        Usuario atual = usuarioAutenticado(authentication);
        if (atual.getPerfil() != Perfil.GESTOR && atual.getPerfil() != Perfil.ADMINISTRADOR) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Somente gestor ou administrador pode atribuir tarefas.");
        }

        Equipe equipe = permissaoEquipe.exigirEquipe(req.equipeId());
        if (atual.getPerfil() == Perfil.GESTOR) permissaoEquipe.exigirGestorDaEquipe(equipe, atual);

        Usuario responsavel = usuarioRepository.findById(req.responsavelId())
                .filter(Usuario::isAtivo)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "O usuário responsável não existe ou está inativo."));

        permissaoEquipe.exigirIntegranteAtivo(req.equipeId(), responsavel.getId());
        String prioridade = req.prioridade() == null || req.prioridade().isBlank()
                ? "MEDIA" : req.prioridade().toUpperCase();
        if (!PRIORIDADES.contains(prioridade)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Prioridade inválida.");
        }

        Tarefa tarefa = new Tarefa(req.equipeId(), responsavel.getId(), atual.getId(),
                req.titulo().trim(), req.descricao() == null ? "" : req.descricao().trim(),
                prioridade, req.prazo());
        return TarefaResposta.de(tarefaRepository.save(tarefa), responsavel.getNome(), equipeNome(equipe));
    }

    /** Lista as tarefas da equipe; apenas o gestor responsável ou administrador pode consultar. */
    @GetMapping("/equipe/{equipeId}")
    public List<TarefaResposta> listarDaEquipe(@PathVariable Long equipeId,
                                               Authentication authentication) {
        Usuario atual = usuarioAutenticado(authentication);
        Equipe equipe = permissaoEquipe.exigirEquipe(equipeId);
        if (atual.getPerfil() == Perfil.GESTOR) permissaoEquipe.exigirGestorDaEquipe(equipe, atual);
        else if (atual.getPerfil() != Perfil.ADMINISTRADOR)
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Acesso restrito ao gestor da equipe.");

        return tarefaRepository.findByEquipeIdOrderByCriadoEmDesc(equipeId).stream()
                .map(t -> TarefaResposta.de(t, nomeUsuario(t.getResponsavelId()), equipeNome(equipe)))
                .toList();
    }

    /** Lista as tarefas atribuídas ao usuário autenticado. */
    @GetMapping("/minhas")
    public List<TarefaResposta> minhas(Authentication authentication) {
        Usuario atual = usuarioAutenticado(authentication);
        return tarefaRepository.findByResponsavelIdOrderByCriadoEmDesc(atual.getId()).stream()
                .map(t -> TarefaResposta.de(t, atual.getNome(), nomeEquipe(t.getEquipeId())))
                .toList();
    }

    /** O responsável pode atualizar o status da própria tarefa; gestor da equipe/admin também. */
    @PatchMapping("/{id}/status")
    public TarefaResposta atualizarStatus(@PathVariable Long id,
                                          @Valid @RequestBody AtualizarStatusRequisicao req,
                                          Authentication authentication) {
        Usuario atual = usuarioAutenticado(authentication);
        Tarefa tarefa = tarefaRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Tarefa não encontrada."));
        if (!STATUS.contains(req.status())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Status inválido.");
        }

        boolean pode = atual.getPerfil() == Perfil.ADMINISTRADOR
                || atual.getId().equals(tarefa.getResponsavelId());
        if (atual.getPerfil() == Perfil.GESTOR) {
            Equipe equipe = permissaoEquipe.exigirEquipe(tarefa.getEquipeId());
            try {
                permissaoEquipe.exigirGestorDaEquipe(equipe, atual);
                pode = true;
            } catch (ResponseStatusException ignored) { }
        }
        if (!pode) throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Você não pode alterar esta tarefa.");

        tarefa.setStatus(req.status());
        Equipe equipe = permissaoEquipe.exigirEquipe(tarefa.getEquipeId());
        return TarefaResposta.de(tarefaRepository.save(tarefa),
                nomeUsuario(tarefa.getResponsavelId()), equipeNome(equipe));
    }

    private Usuario usuarioAutenticado(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Autenticação necessária.");
        }
        return usuarioRepository.findByEmail(authentication.getName())
                .filter(Usuario::isAtivo)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                        "Usuário não encontrado ou inativo."));
    }

    private String nomeUsuario(Long id) {
        return usuarioRepository.findById(id).map(Usuario::getNome).orElse("Usuário #" + id);
    }

    private String nomeEquipe(Long id) {
        try { return equipeNome(permissaoEquipe.exigirEquipe(id)); }
        catch (RuntimeException ex) { return "Equipe #" + id; }
    }

    private String equipeNome(Equipe equipe) {
        try {
            return String.valueOf(equipe.getClass().getMethod("getNome").invoke(equipe));
        } catch (ReflectiveOperationException ex) {
            return "Equipe";
        }
    }

    public record CriarTarefaRequisicao(
            @NotNull Long equipeId,
            @NotNull Long responsavelId,
            @NotBlank @Size(max = 160) String titulo,
            @Size(max = 2000) String descricao,
            String prioridade,
            LocalDate prazo) { }

    public record AtualizarStatusRequisicao(@NotBlank String status) { }

    public record TarefaResposta(Long id, Long equipeId, String equipeNome,
                                 Long responsavelId, String responsavelNome,
                                 Long criadoPorId, String titulo, String descricao,
                                 String prioridade, LocalDate prazo, String status,
                                 LocalDateTime criadoEm) {
        static TarefaResposta de(Tarefa t, String responsavelNome, String equipeNome) {
            return new TarefaResposta(t.getId(), t.getEquipeId(), equipeNome,
                    t.getResponsavelId(), responsavelNome, t.getCriadoPorId(),
                    t.getTitulo(), t.getDescricao(), t.getPrioridade(), t.getPrazo(),
                    t.getStatus(), t.getCriadoEm());
        }
    }
}
