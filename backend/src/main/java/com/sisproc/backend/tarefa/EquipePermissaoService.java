package com.sisproc.backend.tarefa;

import com.sisproc.backend.equipe.Equipe;
import com.sisproc.backend.equipe.EquipeRepository;
import com.sisproc.backend.equipe.MembroEquipe;
import com.sisproc.backend.equipe.MembroEquipeRepository;
import com.sisproc.backend.usuario.Usuario;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Objects;

/** Centraliza a autorização sobre equipes sem confiar em IDs enviados pelo navegador. */
@Service
public class EquipePermissaoService {
    private final EquipeRepository equipeRepository;
    private final MembroEquipeRepository membroRepository;

    public EquipePermissaoService(EquipeRepository equipeRepository,
                                  MembroEquipeRepository membroRepository) {
        this.equipeRepository = equipeRepository;
        this.membroRepository = membroRepository;
    }

    public Equipe exigirEquipe(Long equipeId) {
        return equipeRepository.findById(equipeId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Equipe não encontrada."));
    }

    public void exigirGestorDaEquipe(Equipe equipe, Usuario gestor) {
        Long idGestor = idGestor(equipe);
        if (!Objects.equals(idGestor, gestor.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Você não é o gestor responsável por esta equipe.");
        }
    }

    public void exigirIntegranteAtivo(Long equipeId, Long usuarioId) {
        boolean encontrado = membroRepository.findAll().stream().anyMatch(membro ->
                Objects.equals(idDaRelacao(membro, "getEquipe", "getEquipeId", "equipe", "equipeId"), equipeId)
                && Objects.equals(idDaRelacao(membro, "getUsuario", "getUsuarioId", "usuario", "usuarioId"), usuarioId)
                && ativo(membro));
        if (!encontrado) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "O responsável selecionado não é integrante ativo desta equipe.");
        }
    }

    private static Long idGestor(Equipe equipe) {
        Object gestor = valor(equipe, "getGestor", "gestor");
        if (gestor != null) return numero(valor(gestor, "getId", "id"));
        return numero(valor(equipe, "getGestorId", "gestorId"));
    }

    private static Long idDaRelacao(Object entidade, String getterRelacao, String getterId,
                                    String campoRelacao, String campoId) {
        Object relacao = valor(entidade, getterRelacao, campoRelacao);
        if (relacao != null) return numero(valor(relacao, "getId", "id"));
        return numero(valor(entidade, getterId, campoId));
    }

    private static boolean ativo(Object membro) {
        Object valor = valor(membro, "isAtivo", "getAtivo", "getAtivo", "ativo");
        return !(valor instanceof Boolean b) || b;
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
}
