package com.sisproc.backend.equipe;

import com.sisproc.backend.usuario.Perfil;
import com.sisproc.backend.usuario.Usuario;
import com.sisproc.backend.usuario.UsuarioRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Objects;

/**
 * Endpoint aditivo para a lista de equipes do gestor autenticado.
 * Não substitui o EquipeController existente.
 */
@RestController
@RequestMapping("/api/equipes")
public class EquipeGestorController {
    private final EquipeRepository equipeRepository;
    private final UsuarioRepository usuarioRepository;

    public EquipeGestorController(EquipeRepository equipeRepository,
                                  UsuarioRepository usuarioRepository) {
        this.equipeRepository = equipeRepository;
        this.usuarioRepository = usuarioRepository;
    }

    @GetMapping("/minhas")
    public List<EquipeResumo> minhas(Authentication authentication) {
        Usuario atual = usuarioAutenticado(authentication);
        if (atual.getPerfil() != Perfil.GESTOR) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Somente gestores podem consultar suas equipes por este endpoint.");
        }

        Long idGestor = atual.getId();
        return equipeRepository.findAll().stream()
                .filter(equipe -> Objects.equals(idGestor, gestorId(equipe)))
                .map(this::resumo)
                .toList();
    }

    private Usuario usuarioAutenticado(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Autenticação necessária.");
        }
        return usuarioRepository.findByEmail(authentication.getName())
                .filter(Usuario::isAtivo)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED, "Usuário autenticado não encontrado ou inativo."));
    }

    private EquipeResumo resumo(Equipe equipe) {
        Object gestor = valor(equipe, "getGestor", "gestor");
        Long id = numero(valor(equipe, "getId", "id"));
        Long idGestor = gestor == null
                ? numero(valor(equipe, "getGestorId", "gestorId"))
                : numero(valor(gestor, "getId", "id"));
        String gestorNome = gestor == null ? texto(valor(equipe, "getGestorNome", "gestorNome"))
                : texto(valor(gestor, "getNome", "nome"));
        String gestorEmail = gestor == null ? texto(valor(equipe, "getGestorEmail", "gestorEmail"))
                : texto(valor(gestor, "getEmail", "email"));
        return new EquipeResumo(
                id,
                texto(valor(equipe, "getNome", "nome")),
                texto(valor(equipe, "getDescricao", "descricao")),
                booleano(valor(equipe, "isAtivo", "getAtivo", "ativo"), true),
                idGestor,
                gestorNome,
                gestorEmail
        );
    }

    private Long gestorId(Equipe equipe) {
        Object gestor = valor(equipe, "getGestor", "gestor");
        if (gestor != null) return numero(valor(gestor, "getId", "id"));
        return numero(valor(equipe, "getGestorId", "gestorId"));
    }

    private static Object valor(Object alvo, String... nomes) {
        if (alvo == null) return null;
        for (String nome : nomes) {
            try {
                Method m = alvo.getClass().getMethod(nome);
                return m.invoke(alvo);
            } catch (ReflectiveOperationException ignored) { }
            try {
                Field f = alvo.getClass().getDeclaredField(nome);
                f.setAccessible(true);
                return f.get(alvo);
            } catch (ReflectiveOperationException ignored) { }
        }
        return null;
    }

    private static Long numero(Object valor) {
        if (valor instanceof Number n) return n.longValue();
        if (valor != null) {
            try { return Long.parseLong(valor.toString()); }
            catch (NumberFormatException ignored) { }
        }
        return null;
    }

    private static String texto(Object valor) {
        return valor == null ? null : valor.toString();
    }

    private static boolean booleano(Object valor, boolean padrao) {
        return valor instanceof Boolean b ? b : padrao;
    }

    public record EquipeResumo(Long id, String nome, String descricao, boolean ativo,
                               Long gestorId, String gestorNome, String gestorEmail) { }
}
