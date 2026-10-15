package com.sisproc.backend.usuario;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/** Endpoint para o usuário autenticado alterar a própria senha. */
@RestController
@RequestMapping("/api/usuarios/me")
public class TrocaSenhaController {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    public TrocaSenhaController(
            UsuarioRepository usuarioRepository,
            PasswordEncoder passwordEncoder) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @PostMapping("/senha")
    public ResponseEntity<Void> alterarMinhaSenha(
            @Valid @RequestBody AlterarSenhaRequisicao requisicao,
            Authentication autenticacao) {

        if (autenticacao == null
                || !autenticacao.isAuthenticated()
                || autenticacao instanceof AnonymousAuthenticationToken) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "É necessário estar autenticado para alterar a senha.");
        }

        Usuario usuario = usuarioRepository.findByEmail(autenticacao.getName())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED,
                        "Usuário da sessão não encontrado."));

        // Usuario.java utiliza senhaHash, não senha.
        if (!passwordEncoder.matches(requisicao.senhaAtual(), usuario.getSenhaHash())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "A senha atual informada está incorreta.");
        }

        if (requisicao.senhaAtual().equals(requisicao.novaSenha())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "A nova senha deve ser diferente da senha atual.");
        }

        usuario.setSenhaHash(passwordEncoder.encode(requisicao.novaSenha()));
        usuarioRepository.save(usuario);

        return ResponseEntity.noContent().build();
    }

    public record AlterarSenhaRequisicao(
            @NotBlank String senhaAtual,
            @NotBlank @Size(min = 8, max = 72) String novaSenha) {
    }
}
