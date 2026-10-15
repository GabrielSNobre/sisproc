package com.sisproc.backend.usuario;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/** Permite exclusivamente a um administrador redefinir a senha de uma conta. */
@RestController
@RequestMapping("/api/usuarios")
public class AdminRedefinirSenhaController {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    public AdminRedefinirSenhaController(
            UsuarioRepository usuarioRepository,
            PasswordEncoder passwordEncoder) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @PostMapping("/{usuarioId}/senha")
    public ResponseEntity<Void> redefinirSenhaDeUsuario(
            @PathVariable Long usuarioId,
            @Valid @RequestBody RedefinirSenhaRequisicao requisicao,
            Authentication autenticacao) {

        if (autenticacao == null
                || !autenticacao.isAuthenticated()
                || autenticacao instanceof AnonymousAuthenticationToken) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                    "É necessário estar autenticado para redefinir uma senha.");
        }

        Usuario administrador = usuarioRepository.findByEmail(autenticacao.getName())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                        "Usuário da sessão não encontrado."));

        // A autorização é conferida no banco, não apenas pelo que o frontend exibe.
        if (administrador.getPerfil() != Perfil.ADMINISTRADOR) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Somente administradores podem redefinir senhas de outros usuários.");
        }

        Usuario usuarioAlvo = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Usuário não encontrado."));

        usuarioAlvo.setSenhaHash(passwordEncoder.encode(requisicao.novaSenha()));
        usuarioRepository.save(usuarioAlvo);

        return ResponseEntity.noContent().build();
    }

    public record RedefinirSenhaRequisicao(
            @NotBlank @Size(min = 8, max = 72) String novaSenha) {
    }
}
