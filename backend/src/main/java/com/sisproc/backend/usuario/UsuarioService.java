package com.sisproc.backend.usuario;

import java.util.Locale;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import org.springframework.data.domain.Sort;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UsuarioService {

	private final UsuarioRepository usuarioRepository;
	private final PasswordEncoder passwordEncoder;

	public UsuarioService(
			UsuarioRepository usuarioRepository,
			PasswordEncoder passwordEncoder) {
		this.usuarioRepository = usuarioRepository;
		this.passwordEncoder = passwordEncoder;
			}

	/**
	 * Cria o primeiro administrador somente se não houver usuários.
	 */
	@Transactional
	public Usuario cadastrarPrimeiroAdministrador(
			String nome,
			String email,
			String senha) {

		if (usuarioRepository.count() > 0) {
			throw new IllegalStateException(
					"O primeiro administrador já foi cadastrado.");
		}

		return criarUsuario(nome, email, senha, Perfil.ADMINISTRADOR);
			}

	/**
	 * Cadastra novos usuários mediante autorização administrativa.
	 */
	@Transactional
	public Usuario cadastrarUsuario(
			String nome,
			String email,
			String senha,
			Perfil perfil) {

		exigirAdministradorAutenticado();

		if (perfil == null) {
			throw new IllegalArgumentException(
					"O perfil do usuário é obrigatório.");
		}

		return criarUsuario(nome, email, senha, perfil);
			}

	private Usuario criarUsuario(
			String nome,
			String email,
			String senha,
			Perfil perfil) {

		String nomeNormalizado = normalizarNome(nome);
		String emailNormalizado = normalizarEmail(email);
		validarSenha(senha);

		if (usuarioRepository.existsByEmail(emailNormalizado)) {
			throw new IllegalArgumentException(
					"Já existe um usuário cadastrado com esse e-mail.");
		}

		Usuario usuario = new Usuario();
		usuario.setNome(nomeNormalizado);
		usuario.setEmail(emailNormalizado);
		usuario.setSenhaHash(passwordEncoder.encode(senha));
		usuario.setPerfil(perfil);
		usuario.setAtivo(true);

		return usuarioRepository.save(usuario);
			}

	private String normalizarNome(String nome) {
		if (nome == null || nome.isBlank()) {
			throw new IllegalArgumentException(
					"O nome do usuário é obrigatório.");
		}

		String nomeNormalizado = nome.trim();

		if (nomeNormalizado.length() > 150) {
			throw new IllegalArgumentException(
					"O nome deve ter no máximo 150 caracteres.");
		}

		return nomeNormalizado;
	}

	private String normalizarEmail(String email) {
		if (email == null || email.isBlank()) {
			throw new IllegalArgumentException(
					"O e-mail é obrigatório.");
		}

		String emailNormalizado = email.trim().toLowerCase(Locale.ROOT);

		if (emailNormalizado.length() > 254
				|| !emailNormalizado.matches(
					"^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")) {
			throw new IllegalArgumentException(
					"Informe um e-mail válido.");
					}

		return emailNormalizado;
	}

	private void validarSenha(String senha) {
		if (senha == null || senha.length() < 8) {
			throw new IllegalArgumentException(
					"A senha deve ter pelo menos 8 caracteres.");
		}
	}

	private void exigirAdministradorAutenticado() {
		Authentication autenticacao =
			SecurityContextHolder.getContext().getAuthentication();

		boolean administradorAutenticado =
			autenticacao != null
			&& autenticacao.isAuthenticated()
			&& !(autenticacao instanceof AnonymousAuthenticationToken)
			&& autenticacao.getAuthorities().stream()
			.anyMatch(autoridade ->
					autoridade.getAuthority()
					.equals("ROLE_ADMINISTRADOR"));

		if (!administradorAutenticado) {
			throw new org.springframework.security.access.AccessDeniedException(
					"Somente administradores podem cadastrar usuários.");
		}
	}

	public List<Usuario> listarUsuarios() {
		exigirAdministradorAutenticado();

		return usuarioRepository.findAll(
				Sort.by(Sort.Direction.ASC, "nome")
				);
	}

	public Usuario obterMeuPerfil() {
		return obterUsuarioAutenticado();
	}

	public Usuario atualizarMeuPerfil(
			String nome,
			String email) {

		Usuario usuario = obterUsuarioAutenticado();

		String nomeNormalizado = normalizarNome(nome);
		String emailNormalizado = normalizarEmail(email);

		boolean emailPertenceAOutroUsuario =
			usuarioRepository.findByEmail(emailNormalizado)
			.filter(outro -> !outro.getId().equals(usuario.getId()))
			.isPresent();

		if (emailPertenceAOutroUsuario) {
			throw new IllegalArgumentException(
					"Este e-mail já está cadastrado."
					);
		}

		usuario.setNome(nomeNormalizado);
		usuario.setEmail(emailNormalizado);

		return usuarioRepository.save(usuario);
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
					"É necessário estar autenticado."
					);
				}

		return usuarioRepository.findByEmail(autenticacao.getName())
			.orElseThrow(() -> new ResponseStatusException(
						HttpStatus.UNAUTHORIZED,
						"Usuário autenticado não encontrado."
						));
	}

}
