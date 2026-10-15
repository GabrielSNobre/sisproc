
package com.sisproc.backend.equipe;

import java.util.List;

import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.sisproc.backend.usuario.Perfil;
import com.sisproc.backend.usuario.Usuario;
import com.sisproc.backend.usuario.UsuarioRepository;

@Service
public class EquipeService {

    private final EquipeRepository equipeRepository;
    private final UsuarioRepository usuarioRepository;
    private final MembroEquipeRepository membroEquipeRepository;

    public EquipeService(
            EquipeRepository equipeRepository,
            UsuarioRepository usuarioRepository,
            MembroEquipeRepository membroEquipeRepository) {

        this.equipeRepository = equipeRepository;
        this.usuarioRepository = usuarioRepository;
        this.membroEquipeRepository = membroEquipeRepository;
    }

    @Transactional
    public Equipe criarEquipe(
            String nome,
            String descricao,
            Long gestorId) {

        exigirAdministrador();

        if (nome == null || nome.isBlank()) {
            throw new IllegalArgumentException(
                    "O nome da equipe é obrigatório.");
        }

        nome = nome.trim();

        if (nome.length() > 120) {
            throw new IllegalArgumentException(
                    "O nome da equipe deve ter no máximo 120 caracteres.");
        }

        if (descricao != null && descricao.length() > 500) {
            throw new IllegalArgumentException(
                    "A descrição deve ter no máximo 500 caracteres.");
        }

        if (equipeRepository.existsByNomeIgnoreCase(nome)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Já existe uma equipe com esse nome.");
        }

        Usuario gestor = usuarioRepository.findById(gestorId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Gestor não encontrado."));

        if (gestor.getPerfil() != Perfil.GESTOR || !gestor.isAtivo()) {
            throw new IllegalArgumentException(
                    "A equipe deve possuir um gestor ativo.");
        }

        Equipe equipe = new Equipe();
        equipe.setNome(nome);
        equipe.setDescricao(
                descricao == null || descricao.isBlank()
                        ? null
                        : descricao.trim());
        equipe.setGestor(gestor);
        equipe.setAtivo(true);

        return equipeRepository.save(equipe);
    }

    @Transactional(readOnly = true)
    public List<Equipe> listarEquipes() {
        exigirAdministrador();

        return equipeRepository.findAll(
                Sort.by(Sort.Direction.ASC, "nome"));
    }

    @Transactional
    public MembroEquipe adicionarMembro(
            Long equipeId,
            Long usuarioId) {

        Usuario ator = obterUsuarioAutenticado();

        Equipe equipe = obterEquipe(equipeId);
        autorizarGestaoDaEquipe(equipe, ator);

        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Usuário não encontrado."));

        if (!usuario.isAtivo()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Não é possível vincular um usuário inativo.");
        }

        if (usuario.getPerfil() != Perfil.USUARIO) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Somente usuários com perfil USUARIO "
                            + "podem ser vinculados.");
        }

        MembroEquipe membro = membroEquipeRepository
                .findByEquipeIdAndUsuarioId(equipeId, usuarioId)
                .orElse(null);

        if (membro != null) {
            if (membro.isAtivo()) {
                throw new ResponseStatusException(
                        HttpStatus.CONFLICT,
                        "O usuário já está vinculado a esta equipe.");
            }

            membro.setAtivo(true);
            return membroEquipeRepository.save(membro);
        }

        membro = new MembroEquipe();
        membro.setEquipe(equipe);
        membro.setUsuario(usuario);
        membro.setAtivo(true);

        return membroEquipeRepository.save(membro);
    }

    @Transactional(readOnly = true)
    public List<MembroEquipe> listarMembros(Long equipeId) {
        Usuario ator = obterUsuarioAutenticado();

        Equipe equipe = obterEquipe(equipeId);
        autorizarGestaoDaEquipe(equipe, ator);

        return membroEquipeRepository
                .findAllByEquipeIdAndAtivoTrue(equipeId);
    }

    @Transactional
    public void removerMembro(
            Long equipeId,
            Long usuarioId) {

        Usuario ator = obterUsuarioAutenticado();

        Equipe equipe = obterEquipe(equipeId);
        autorizarGestaoDaEquipe(equipe, ator);

        MembroEquipe membro = membroEquipeRepository
                .findByEquipeIdAndUsuarioId(equipeId, usuarioId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Vínculo do usuário com a equipe não encontrado."));

        if (!membro.isAtivo()) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "O usuário não está ativo nesta equipe.");
        }

        membro.setAtivo(false);
        membroEquipeRepository.save(membro);
    }

    private Equipe obterEquipe(Long equipeId) {
        return equipeRepository.findById(equipeId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Equipe não encontrada."));
    }

    private Usuario obterUsuarioAutenticado() {
        Authentication autenticacao =
                SecurityContextHolder.getContext().getAuthentication();

        if (autenticacao == null
                || !autenticacao.isAuthenticated()
                || autenticacao.getName() == null
                || "anonymousUser".equals(autenticacao.getName())) {

            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "É necessário estar autenticado.");
        }

        return usuarioRepository.findByEmail(autenticacao.getName())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED,
                        "Usuário autenticado não encontrado."));
    }

    private void autorizarGestaoDaEquipe(
            Equipe equipe,
            Usuario ator) {

        if (ator.getPerfil() == Perfil.ADMINISTRADOR) {
            return;
        }

        if (ator.getPerfil() == Perfil.GESTOR
                && equipe.getGestor().getId().equals(ator.getId())) {
            return;
        }

        throw new ResponseStatusException(
                HttpStatus.FORBIDDEN,
                "Você não tem permissão para gerenciar esta equipe.");
    }

    private void exigirAdministrador() {
        Authentication autenticacao =
                SecurityContextHolder.getContext().getAuthentication();

        if (autenticacao == null
                || !autenticacao.isAuthenticated()
                || autenticacao.getAuthorities().stream()
                        .noneMatch(autoridade ->
                                autoridade.getAuthority()
                                        .equals("ROLE_ADMINISTRADOR"))) {

            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Somente administradores podem executar esta operação.");
        }
    }
}
