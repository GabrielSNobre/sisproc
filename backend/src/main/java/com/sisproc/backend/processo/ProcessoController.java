package com.sisproc.backend.processo;

import com.sisproc.backend.equipe.*;
import com.sisproc.backend.tarefa.*;
import com.sisproc.backend.usuario.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import java.time.LocalDateTime;
import java.util.*;

/** Documentos imutáveis ligados a tarefas, com respostas de auditoria e encaminhamento entre equipes. */
@RestController
@RequestMapping("/api/processos")
public class ProcessoController {
    private static final Set<String> TIPOS = Set.of("ACAO", "MINUTA", "DESPACHO", "PARECER", "SOLICITACAO", "OUTRO");
    private final DocumentoProcessoRepository documentos;
    private final RespostaDocumentoRepository respostas;
    private final TarefaRepository tarefas;
    private final EquipeRepository equipes;
    private final MembroEquipeRepository membros;
    private final UsuarioRepository usuarios;

    public ProcessoController(DocumentoProcessoRepository documentos, RespostaDocumentoRepository respostas,
            TarefaRepository tarefas, EquipeRepository equipes, MembroEquipeRepository membros, UsuarioRepository usuarios) {
        this.documentos = documentos; this.respostas = respostas; this.tarefas = tarefas;
        this.equipes = equipes; this.membros = membros; this.usuarios = usuarios;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Transactional
    public DocumentoDto criar(@Valid @RequestBody NovoDocumento req, Authentication auth) {
        Usuario autor = atual(auth);
        Tarefa tarefa = tarefa(req.tarefaId());
        Long origem = tarefa.getEquipeId();
		if (!podeAtuar(origem, autor)) {
			throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Sem permissão para documentar esta tarefa.");
        }
        Long destino = req.equipeDestinoId();
        if (destino != null) {
            if (Objects.equals(destino, origem)) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A equipe destinatária deve ser diferente da origem.");
            Equipe equipeDestino = equipe(destino);
            if (!equipeDestino.isAtivo()) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Equipe destinatária inativa.");
        }
        String tipo = req.tipo().trim().toUpperCase(Locale.ROOT);
        if (!TIPOS.contains(tipo)) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Tipo de documento inválido.");
        DocumentoProcesso doc = documentos.save(new DocumentoProcesso(tarefa.getId(), origem, destino, autor.getId(),
                tipo, req.assunto().trim(), req.conteudo().trim()));
        return dto(doc);
    }

	@GetMapping("/tarefa/{tarefaId}")
	@Transactional(readOnly = true)
	public List<DocumentoDto> porTarefa(
			@PathVariable Long tarefaId,
			Authentication auth) {

		Usuario usuario = atual(auth);

		tarefa(tarefaId); // Confirma a existência da tarefa.

		return documentos
			.findByTarefaIdOrderByCriadoEmAscIdAsc(tarefaId)
			.stream()
			.filter(doc -> podeVisualizarDocumento(doc, usuario))
			.map(this::dto)
			.toList();
			}

	/** Histórico único da tarefa: documentos e respostas ordenados por data real da movimentação. */
	@GetMapping("/tarefa/{tarefaId}/linha-do-tempo")
	@Transactional(readOnly = true)
    public List<MovimentacaoDto> linhaDoTempo(@PathVariable Long tarefaId, Authentication auth) {
        Usuario usuario = atual(auth);
        Tarefa tarefa = tarefa(tarefaId);
        boolean acessoOrigem = podeVerEquipe(tarefa.getEquipeId(), usuario)
                || Objects.equals(tarefa.getResponsavelId(), usuario.getId());
        List<MovimentacaoDto> eventos = new ArrayList<>();
        for (DocumentoProcesso doc : documentos.findByTarefaIdOrderByCriadoEmAscIdAsc(tarefaId)) {
            if (!acessoOrigem && (doc.getEquipeDestinoId() == null
                    || !podeVerEquipe(doc.getEquipeDestinoId(), usuario))) continue;
            eventos.add(new MovimentacaoDto("DOCUMENTO", doc.getId(), doc.getId(), doc.getTarefaId(),
                    doc.getTipo(), doc.getAssunto(), doc.getConteudo(), doc.getAutorId(),
                    nomeUsuario(doc.getAutorId()), doc.getEquipeOrigemId(), nomeEquipe(doc.getEquipeOrigemId()),
                    doc.getEquipeDestinoId(), doc.getEquipeDestinoId() == null ? null : nomeEquipe(doc.getEquipeDestinoId()),
                    doc.getCriadoEm()));
            for (RespostaDocumento resposta : respostas.findByDocumentoIdOrderByCriadoEmAscIdAsc(doc.getId())) {
                Long equipeAutora = equipeDaResposta(doc, resposta.getAutorId());
                eventos.add(new MovimentacaoDto("RESPOSTA", resposta.getId(), doc.getId(), doc.getTarefaId(),
                        doc.getTipo(), doc.getAssunto(), resposta.getConteudo(), resposta.getAutorId(),
                        nomeUsuario(resposta.getAutorId()), equipeAutora,
                        equipeAutora == null ? null : nomeEquipe(equipeAutora),
                        doc.getEquipeDestinoId(), doc.getEquipeDestinoId() == null ? null : nomeEquipe(doc.getEquipeDestinoId()),
                        resposta.getCriadoEm()));
            }
        }
        eventos.sort(Comparator.comparing(MovimentacaoDto::criadoEm)
                .thenComparing(MovimentacaoDto::tipoEvento)
                .thenComparing(MovimentacaoDto::id));
        return eventos;
    }

    /** Não presume que a resposta é da equipe destinatária: identifica o vínculo do autor. */
    private Long equipeDaResposta(DocumentoProcesso doc, Long autorId) {
        if (doc.getEquipeDestinoId() != null && pertenceEquipe(doc.getEquipeDestinoId(), autorId))
            return doc.getEquipeDestinoId();
        if (pertenceEquipe(doc.getEquipeOrigemId(), autorId)) return doc.getEquipeOrigemId();
        return null;
    }

    private boolean pertenceEquipe(Long equipeId, Long usuarioId) {
        return membros.existsByEquipeIdAndUsuarioIdAndAtivoTrue(equipeId, usuarioId)
                || equipes.findById(equipeId).map(e -> Objects.equals(e.getGestor().getId(), usuarioId)).orElse(false);
    }

    public record MovimentacaoDto(String tipoEvento, Long id, Long documentoId, Long tarefaId,
            String tipoDocumento, String assunto, String conteudo, Long autorId, String autorNome,
            Long equipeAutoraId, String equipeAutoraNome, Long equipeDestinoId,
            String equipeDestinoNome, LocalDateTime criadoEm) {}

    /** LOV de equipes para usuários autenticados, sem expor dados pessoais. */
    @GetMapping("/equipes-disponiveis")
    @Transactional(readOnly = true)
    public List<EquipeOpcaoDto> equipesDisponiveis(Authentication auth) {
        atual(auth);
        return equipes.findAllByOrderByNomeAsc().stream()
                .filter(Equipe::isAtivo)
                .map(e -> new EquipeOpcaoDto(e.getId(), e.getNome()))
                .toList();
    }

    public record EquipeOpcaoDto(Long id, String nome) {}

    @GetMapping("/recebidos")
    @Transactional(readOnly = true)
    public List<DocumentoDto> recebidos(Authentication auth) {
        Usuario usuario = atual(auth);
        // Consulta limitada a equipes em que o usuário participa ou exerce gestão.
        Set<Long> equipesAcessiveis = new HashSet<>();
        for (MembroEquipe m : membros.findAllByUsuarioIdAndAtivoTrue(usuario.getId())) {
            if (m.getEquipe().isAtivo()) equipesAcessiveis.add(m.getEquipe().getId());
        }
        if (usuario.getPerfil() == Perfil.GESTOR) {
            for (Equipe e : equipes.findByGestorIdAndAtivoTrueOrderByNomeAsc(usuario.getId())) equipesAcessiveis.add(e.getId());
        }
        if (usuario.getPerfil() == Perfil.ADMINISTRADOR) {
            for (Equipe e : equipes.findAll()) if (e.isAtivo()) equipesAcessiveis.add(e.getId());
        }
        return equipesAcessiveis.stream().flatMap(id -> documentos.findByEquipeDestinoIdOrderByCriadoEmDescIdDesc(id).stream())
                .sorted(Comparator.comparing(DocumentoProcesso::getCriadoEm).reversed().thenComparing(DocumentoProcesso::getId, Comparator.reverseOrder()))
                .map(this::dto).toList();
    }

    @GetMapping("/{documentoId}/respostas")
    @Transactional(readOnly = true)
    public List<RespostaDto> listarRespostas(@PathVariable Long documentoId, Authentication auth) {
        DocumentoProcesso doc = documento(documentoId);
        exigirVisibilidade(doc, atual(auth));
        return respostas.findByDocumentoIdOrderByCriadoEmAscIdAsc(documentoId).stream().map(this::dto).toList();
    }

    @PostMapping("/{documentoId}/respostas")
    @ResponseStatus(HttpStatus.CREATED)
    @Transactional
    public RespostaDto responder(@PathVariable Long documentoId, @Valid @RequestBody NovaResposta req, Authentication auth) {
        Usuario usuario = atual(auth);
        DocumentoProcesso doc = documento(documentoId);
        exigirVisibilidade(doc, usuario);
        // Somente participantes autorizados das equipes envolvidas podem responder.
        RespostaDocumento resposta = respostas.save(new RespostaDocumento(doc.getId(), usuario.getId(), req.conteudo().trim()));
        return dto(resposta);
    }

	private void exigirVisibilidade(
			DocumentoProcesso doc,
			Usuario usuario) {

		if (!podeVisualizarDocumento(doc, usuario)) {
			throw new ResponseStatusException(
					HttpStatus.FORBIDDEN,
					"Você não possui acesso a este documento."
					);
		}
			}

	private boolean podeVisualizarDocumento(
			DocumentoProcesso doc,
			Usuario usuario) {

		if (usuario.getPerfil() == Perfil.ADMINISTRADOR) {
			return true;
		}

		boolean emissora = podeVerEquipe(
				doc.getEquipeOrigemId(),
				usuario
				);

		boolean destinataria =
			doc.getEquipeDestinoId() != null
        && podeVerEquipe(
            doc.getEquipeDestinoId(),
            usuario
        );

    return emissora || destinataria;
}
    private boolean podeAtuar(Long equipeId, Usuario usuario) { return podeVerEquipe(equipeId, usuario); }
    private boolean podeVerEquipe(Long equipeId, Usuario usuario) {
        if (usuario.getPerfil() == Perfil.ADMINISTRADOR) return true;
        Equipe equipe = equipe(equipeId);
        if (usuario.getPerfil() == Perfil.GESTOR && Objects.equals(equipe.getGestor().getId(), usuario.getId())) return true;
        return membros.existsByEquipeIdAndUsuarioIdAndAtivoTrue(equipeId, usuario.getId());
    }
    private Usuario atual(Authentication auth) {
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getName()))
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Autenticação necessária.");
        return usuarios.findByEmail(auth.getName()).filter(Usuario::isAtivo)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Usuário inativo ou inexistente."));
    }
    private Equipe equipe(Long id) {
        return equipes.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Equipe não encontrada."));
    }
    private Tarefa tarefa(Long id) {
        return tarefas.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Tarefa não encontrada."));
    }
    private DocumentoProcesso documento(Long id) {
        return documentos.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Documento não encontrado."));
    }
    private String nomeUsuario(Long id) { return usuarios.findById(id).map(Usuario::getNome).orElse("Usuário #" + id); }
    private String nomeEquipe(Long id) { return equipes.findById(id).map(Equipe::getNome).orElse("Equipe #" + id); }
    private DocumentoDto dto(DocumentoProcesso d) {
        return new DocumentoDto(d.getId(), d.getTarefaId(), d.getEquipeOrigemId(), nomeEquipe(d.getEquipeOrigemId()),
                d.getEquipeDestinoId(), d.getEquipeDestinoId() == null ? null : nomeEquipe(d.getEquipeDestinoId()),
                d.getAutorId(), nomeUsuario(d.getAutorId()), d.getTipo(), d.getAssunto(), d.getConteudo(), d.getCriadoEm());
    }
    private RespostaDto dto(RespostaDocumento r) {
        return new RespostaDto(r.getId(), r.getDocumentoId(), r.getAutorId(), nomeUsuario(r.getAutorId()), r.getConteudo(), r.getCriadoEm());
    }
    public record NovoDocumento(@NotNull @Positive Long tarefaId, @NotBlank String tipo,
            @NotBlank @Size(max=160) String assunto, @NotBlank @Size(max=8000) String conteudo,
            @Positive Long equipeDestinoId) {}
    public record NovaResposta(@NotBlank @Size(max=8000) String conteudo) {}
    public record DocumentoDto(Long id, Long tarefaId, Long equipeOrigemId, String equipeOrigemNome,
            Long equipeDestinoId, String equipeDestinoNome, Long autorId, String autorNome,
            String tipo, String assunto, String conteudo, LocalDateTime criadoEm) {}
    public record RespostaDto(Long id, Long documentoId, Long autorId, String autorNome, String conteudo, LocalDateTime criadoEm) {}
}
