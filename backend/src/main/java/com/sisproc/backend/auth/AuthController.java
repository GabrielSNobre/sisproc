package com.sisproc.backend.auth;

import java.util.Map;

import com.sisproc.backend.usuario.Perfil;
import com.sisproc.backend.usuario.Usuario;
import com.sisproc.backend.usuario.UsuarioRepository;
import com.sisproc.backend.usuario.UsuarioService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final UsuarioService usuarioService;
    private final UsuarioRepository usuarioRepository;

    private final HttpSessionSecurityContextRepository contextRepository =
            new HttpSessionSecurityContextRepository();

    public AuthController(
            AuthenticationManager authenticationManager,
            UsuarioService usuarioService,
            UsuarioRepository usuarioRepository) {
        this.authenticationManager = authenticationManager;
        this.usuarioService = usuarioService;
        this.usuarioRepository = usuarioRepository;
    }

    /**
     * Obtém o token CSRF para as requisições protegidas.
     */
    @GetMapping("/csrf")
    public Map<String, String> obterCsrf(CsrfToken csrfToken) {
        return Map.of("token", csrfToken.getToken());
    }

    /**
     * Cadastra o administrador inicial se o banco estiver vazio.
     */
    @PostMapping("/primeiro-administrador")
    public ResponseEntity<UsuarioResposta> cadastrarPrimeiroAdministrador(
            @Valid @RequestBody PrimeiroAdministradorRequisicao requisicao) {

        Usuario usuario;

        try {
            usuario = usuarioService.cadastrarPrimeiroAdministrador(
                    requisicao.nome(),
                    requisicao.email(),
                    requisicao.senha()
            );
        } catch (IllegalStateException e) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "O cadastro inicial já foi realizado."
            );
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    e.getMessage()
            );
        }

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(UsuarioResposta.de(usuario));
    }

    /**
     * Autentica o usuário e persiste o contexto de segurança na sessão.
     */
    @PostMapping("/login")
    public UsuarioResposta login(
            @Valid @RequestBody LoginRequisicao requisicao,
            HttpServletRequest request,
            HttpServletResponse response) {

        Authentication autenticacao;

        try {
            autenticacao = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            requisicao.email(),
                            requisicao.senha()
                    )
            );
        } catch (AuthenticationException e) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "E-mail ou senha inválidos."
            );
        }

        SecurityContext contexto =
                SecurityContextHolder.createEmptyContext();

        contexto.setAuthentication(autenticacao);
        SecurityContextHolder.setContext(contexto);

        contextRepository.saveContext(contexto, request, response);

        Usuario usuario = usuarioRepository
                .findByEmail(autenticacao.getName())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED,
                        "Usuário não encontrado."
                ));

        return UsuarioResposta.de(usuario);
    }

    /**
     * Retorna os dados do usuário da sessão atual.
     */
    @GetMapping("/me")
    public UsuarioResposta usuarioAtual(Authentication autenticacao) {
        if (autenticacao == null
                || !autenticacao.isAuthenticated()) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED
            );
        }

        Usuario usuario = usuarioRepository
                .findByEmail(autenticacao.getName())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED
                ));

        return UsuarioResposta.de(usuario);
    }

    /**
     * Encerra a sessão autenticada.
     */
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
            HttpServletRequest request,
            HttpServletResponse response,
            Authentication autenticacao) {

        new SecurityContextLogoutHandler()
                .logout(request, response, autenticacao);

        return ResponseEntity.noContent().build();
    }

    public record PrimeiroAdministradorRequisicao(
            @NotBlank
            @Size(max = 150)
            String nome,

            @NotBlank
            @Email
            @Size(max = 254)
            String email,

            @NotBlank
            @Size(min = 8, max = 72)
            String senha
    ) {
    }

    public record LoginRequisicao(
            @NotBlank
            @Email
            @Size(max = 254)
            String email,

            @NotBlank
            String senha
    ) {
    }

    public record UsuarioResposta(
            Long id,
            String nome,
            String email,
            Perfil perfil,
            boolean ativo
    ) {
        public static UsuarioResposta de(Usuario usuario) {
            return new UsuarioResposta(
                    usuario.getId(),
                    usuario.getNome(),
                    usuario.getEmail(),
                    usuario.getPerfil(),
                    usuario.isAtivo()
            );
        }
    }
}
