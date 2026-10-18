package com.sisproc.backend.usuario;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.PatchMapping;
@RestController
@RequestMapping("/api/usuarios")
public class UsuarioController {

    private final UsuarioService usuarioService;

    public UsuarioController(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    @PostMapping
    public ResponseEntity<UsuarioResposta> criarUsuario(
            @Valid @RequestBody CriarUsuarioRequisicao requisicao) {

        Usuario usuario = usuarioService.cadastrarUsuario(
            requisicao.nome(),
            requisicao.email(),
            requisicao.senha(),
            requisicao.perfil()
        );

        return ResponseEntity
            .status(HttpStatus.CREATED)
            .body(UsuarioResposta.de(usuario));
    }

    @GetMapping
    public List<UsuarioResposta> listarUsuarios() {
        return usuarioService.listarUsuarios()
            .stream()
            .map(UsuarioResposta::de)
            .toList();
    }

    @PatchMapping("/{usuarioId}/perfil")
    public UsuarioResposta alterarPerfilUsuario(
            @org.springframework.web.bind.annotation.PathVariable Long usuarioId,
            @Valid @RequestBody AlterarPerfilRequisicao requisicao) {
        return UsuarioResposta.de(usuarioService.alterarPerfilUsuario(usuarioId, requisicao.perfil()));
    }

    public record AlterarPerfilRequisicao(@NotNull Perfil perfil) {}

    public record CriarUsuarioRequisicao(
        @NotBlank(message = "O nome é obrigatório.")
        @Size(max = 150, message = "O nome deve ter até 150 caracteres.")
        String nome,

        @NotBlank(message = "O e-mail é obrigatório.")
        @Email(message = "Informe um e-mail válido.")
        @Size(max = 254, message = "O e-mail deve ter até 254 caracteres.")
        String email,

        @NotBlank(message = "A senha é obrigatória.")
        @Size(min = 8, max = 72,
              message = "A senha deve ter entre 8 e 72 caracteres.")
        String senha,

        @NotNull(message = "O perfil é obrigatório.")
        Perfil perfil
    ) {}


	@GetMapping("/me")
	public UsuarioResposta obterMeuPerfil() {
		return UsuarioResposta.de(
				usuarioService.obterMeuPerfil()
				);
	}

	@PatchMapping("/me")
	public UsuarioResposta atualizarMeuPerfil(
			@Valid @RequestBody AtualizarPerfilRequisicao requisicao) {

			Usuario usuario = usuarioService.atualizarMeuPerfil(
					requisicao.nome(),
					requisicao.email()
					);

			return UsuarioResposta.de(usuario);
			}

	public record AtualizarPerfilRequisicao(
			@NotBlank(message = "O nome é obrigatório.")
			@Size(max = 150, message = "O nome deve ter até 150 caracteres.")
			String nome,

			@NotBlank(message = "O e-mail é obrigatório.")
			@Email(message = "Informe um e-mail válido.")
			@Size(max = 254, message = "O e-mail deve ter até 254 caracteres.")
			String email
			) {}

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
