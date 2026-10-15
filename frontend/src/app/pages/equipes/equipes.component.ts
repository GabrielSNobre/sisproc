import { Component, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ApiService, UsuarioSessao } from '../../core/api.service';
import { AuthService } from '../../core/auth.service';

interface Equipe {
  id: number;
  nome: string;
  descricao?: string | null;
  ativo?: boolean;
  gestorId?: number | null;
  gestorNome?: string | null;
  gestorEmail?: string | null;
}

interface Membro {
  id: number;
  usuarioId?: number;
  nome: string;
  email: string;
  perfil?: string;
  ativo?: boolean;
}

interface Tarefa {
  id: number;
  equipeId: number;
  equipeNome?: string;
  responsavelId: number;
  responsavelNome: string;
  titulo: string;
  descricao: string;
  prioridade: 'BAIXA' | 'MEDIA' | 'ALTA' | string;
  prazo?: string | null;
  status: 'PENDENTE' | 'EM_ANDAMENTO' | 'CONCLUIDA' | 'CANCELADA' | string;
  criadoEm?: string;
}

@Component({
  selector: 'app-equipes',
  standalone: true,
  imports: [FormsModule],
  templateUrl: './equipes.component.html',
  styleUrl: './equipes.component.scss',
})
export class EquipesComponent {
  private readonly api = inject(ApiService);
  private readonly auth = inject(AuthService);
  readonly usuario = this.auth.usuario;
  readonly ehAdmin = computed(() => this.usuario()?.perfil === 'ADMINISTRADOR');
  readonly ehGestor = computed(() => this.usuario()?.perfil === 'GESTOR');

  readonly equipes = signal<Equipe[]>([]);
  readonly membros = signal<Membro[]>([]);
  readonly usuariosDisponiveis = signal<UsuarioSessao[]>([]);
  readonly gestoresDisponiveis = signal<UsuarioSessao[]>([]);
  readonly equipeSelecionada = signal<Equipe | null>(null);
  readonly tarefas = signal<Tarefa[]>([]);
  readonly carregando = signal(false);
  readonly carregandoMembros = signal(false);
  readonly carregandoTarefas = signal(false);
  readonly salvando = signal(false);
  readonly erro = signal('');
  readonly sucesso = signal('');
  readonly formularioAberto = signal(false);
  readonly formularioTarefaAberto = signal(false);

  nome = '';
  descricao = '';
  gestorId: number | null = null;
  usuarioId: number | null = null;
  tituloTarefa = '';
  descricaoTarefa = '';
  responsavelTarefaId: number | null = null;
  prioridadeTarefa: 'BAIXA' | 'MEDIA' | 'ALTA' = 'MEDIA';
  prazoTarefa = '';

  constructor() { void this.carregar(); }

  async carregar(): Promise<void> {
    this.carregando.set(true);
    this.erro.set('');
    this.sucesso.set('');
    try {
      // Contrato esperado: o backend filtra a lista para o gestor autenticado.
      // O administrador continua consultando todas as equipes.
      const caminho = this.ehAdmin() ? '/equipes' : '/equipes/minhas';
      const lista = await this.api.get<Equipe[]>(caminho);
      this.equipes.set(lista);

      if (this.ehAdmin()) {
        try {
          const usuarios = await this.api.get<UsuarioSessao[]>('/usuarios');
          this.gestoresDisponiveis.set(usuarios.filter(u => u.ativo && u.perfil === 'GESTOR'));
          this.equipes.update(equipes => equipes.map(eq => {
            const gestor = usuarios.find(u => u.id === eq.gestorId);
            return {
              ...eq,
              gestorNome: eq.gestorNome || gestor?.nome || (eq.gestorId ? `Gestor #${eq.gestorId}` : 'Gestor não informado'),
              gestorEmail: eq.gestorEmail || gestor?.email || '',
            };
          }));
        } catch {
          this.equipes.update(equipes => equipes.map(eq => ({
            ...eq,
            gestorNome: eq.gestorNome || (eq.gestorId ? `Gestor #${eq.gestorId}` : 'Gestor não informado'),
          })));
        }
      }

      const selecionadaAnterior = this.equipeSelecionada()?.id;
      const selecionada = this.equipes().find(eq => eq.id === selecionadaAnterior) ?? this.equipes()[0];
      if (selecionada) await this.selecionar(selecionada);
      else {
        this.equipeSelecionada.set(null);
        this.membros.set([]);
        this.usuariosDisponiveis.set([]);
      }
    } catch (e) {
      const mensagem = this.api.mensagemErro(e);
      this.erro.set(this.ehGestor()
        ? `${mensagem} A tela de gestor consulta GET /api/equipes/minhas; esse endpoint deve retornar apenas as equipes do gestor autenticado.`
        : mensagem);
    } finally {
      this.carregando.set(false);
    }
  }

  abrirFormulario(): void {
    this.nome = '';
    this.descricao = '';
    this.gestorId = null;
    this.erro.set('');
    this.sucesso.set('');
    this.formularioAberto.set(true);
  }

  async criar(): Promise<void> {
    this.erro.set('');
    this.sucesso.set('');
    if (!this.nome.trim()) { this.erro.set('Informe o nome da equipe.'); return; }
    if (!this.gestorId) { this.erro.set('Selecione o gestor responsável.'); return; }
    this.salvando.set(true);
    try {
      await this.api.post('/equipes', { nome: this.nome.trim(), descricao: this.descricao.trim(), gestorId: this.gestorId });
      this.sucesso.set('Equipe criada com sucesso.');
      this.formularioAberto.set(false);
      await this.carregar();
    } catch (e) {
      this.erro.set(this.api.mensagemErro(e));
    } finally {
      this.salvando.set(false);
    }
  }

  async selecionar(equipe: Equipe): Promise<void> {
    this.equipeSelecionada.set(equipe);
    this.erro.set('');
    this.sucesso.set('');
    this.membros.set([]);
    this.usuariosDisponiveis.set([]);
    this.carregandoMembros.set(true);
    try {
      this.membros.set(await this.api.get<Membro[]>(`/equipes/${equipe.id}/membros`));
      if (this.ehGestor() || this.ehAdmin()) {
        await this.carregarTarefas(equipe.id);
      }
      if (this.ehAdmin()) {
        const todos = await this.api.get<UsuarioSessao[]>('/usuarios');
        const idsMembros = new Set(this.membros().map(m => m.usuarioId ?? m.id));
        this.usuariosDisponiveis.set(todos.filter(u => u.ativo && u.perfil === 'USUARIO' && !idsMembros.has(u.id)));
      }
    } catch (e) {
      this.erro.set(this.api.mensagemErro(e));
    } finally {
      this.carregandoMembros.set(false);
    }
  }

  async adicionar(): Promise<void> {
    const equipe = this.equipeSelecionada();
    if (!equipe || !this.usuarioId) return;
    this.erro.set(''); this.sucesso.set(''); this.salvando.set(true);
    try {
      await this.api.post(`/equipes/${equipe.id}/membros`, { usuarioId: this.usuarioId });
      this.sucesso.set('Integrante vinculado à equipe.');
      this.usuarioId = null;
      await this.selecionar(equipe);
    } catch (e) {
      this.erro.set(this.api.mensagemErro(e));
    } finally {
      this.salvando.set(false);
    }
  }

  async remover(membro: Membro): Promise<void> {
    const equipe = this.equipeSelecionada();
    if (!equipe || !this.ehAdmin()) return;
    const id = membro.usuarioId ?? membro.id;
    if (!confirm(`Remover ${membro.nome} da equipe?`)) return;
    this.erro.set(''); this.sucesso.set('');
    try {
      await this.api.delete<void>(`/equipes/${equipe.id}/membros/${id}`);
      this.sucesso.set('Integrante removido da equipe.');
      await this.selecionar(equipe);
    } catch (e) {
      this.erro.set(this.api.mensagemErro(e));
    }
  }

  async carregarTarefas(equipeId: number): Promise<void> {
    this.carregandoTarefas.set(true);
    try {
      this.tarefas.set(await this.api.get<Tarefa[]>(`/tarefas/equipe/${equipeId}`));
    } catch (e) {
      this.tarefas.set([]);
      this.erro.set(this.api.mensagemErro(e));
    } finally {
      this.carregandoTarefas.set(false);
    }
  }

  abrirAtribuicao(): void {
    this.tituloTarefa = '';
    this.descricaoTarefa = '';
    this.responsavelTarefaId = null;
    this.prioridadeTarefa = 'MEDIA';
    this.prazoTarefa = '';
    this.formularioTarefaAberto.set(true);
    this.erro.set('');
    this.sucesso.set('');
  }

  async adicionarRascunhoTarefa(): Promise<void> {
    const equipe = this.equipeSelecionada();
    const membro = this.membros().find(m =>
      (m.usuarioId ?? m.id) === this.responsavelTarefaId && m.ativo !== false);
    if (!equipe) { this.erro.set('Selecione uma equipe antes de atribuir uma tarefa.'); return; }
    if (!this.tituloTarefa.trim()) { this.erro.set('Informe o título da tarefa.'); return; }
    if (!membro || !this.responsavelTarefaId) { this.erro.set('Selecione um integrante ativo como responsável.'); return; }

    this.erro.set('');
    this.sucesso.set('');
    this.salvando.set(true);
    try {
      await this.api.post<Tarefa>('/tarefas', {
        equipeId: equipe.id,
        responsavelId: this.responsavelTarefaId,
        titulo: this.tituloTarefa.trim(),
        descricao: this.descricaoTarefa.trim(),
        prioridade: this.prioridadeTarefa,
        prazo: this.prazoTarefa || null
      });
      this.formularioTarefaAberto.set(false);
      this.sucesso.set('Tarefa criada e atribuída com sucesso.');
      await this.carregarTarefas(equipe.id);
    } catch (e) {
      this.erro.set(this.api.mensagemErro(e));
    } finally {
      this.salvando.set(false);
    }
  }

  async atualizarStatus(tarefa: Tarefa, status: Tarefa['status']): Promise<void> {
    this.erro.set('');
    this.sucesso.set('');
    try {
      await this.api.patch<Tarefa>(`/tarefas/${tarefa.id}/status`, { status });
      const equipe = this.equipeSelecionada();
      if (equipe) await this.carregarTarefas(equipe.id);
      this.sucesso.set('Status da tarefa atualizado.');
    } catch (e) {
      this.erro.set(this.api.mensagemErro(e));
    }
  }

  prioridadeLabel(prioridade: string): string {
    return ({ BAIXA: 'Baixa', MEDIA: 'Média', ALTA: 'Alta' } as Record<string, string>)[prioridade] || prioridade;
  }

  statusLabel(status: string): string {
    return ({
      PENDENTE: 'Pendente',
      EM_ANDAMENTO: 'Em andamento',
      CONCLUIDA: 'Concluída',
      CANCELADA: 'Cancelada'
    } as Record<string, string>)[status] || status;
  }

  perfilLabel(perfil?: string): string {
    if (!perfil) return 'Perfil não informado';
    return ({ ADMINISTRADOR: 'Administrador', GESTOR: 'Gestor', USUARIO: 'Usuário' } as Record<string, string>)[perfil] || perfil;
  }
}
