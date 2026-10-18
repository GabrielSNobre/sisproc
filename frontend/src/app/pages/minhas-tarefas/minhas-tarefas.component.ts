import { Component, computed, inject, signal } from '@angular/core';
import { ApiService } from '../../core/api.service';
import { AuthService } from '../../core/auth.service';

type Status = 'PENDENTE' | 'EM_ANDAMENTO' | 'CONCLUIDA' | 'CANCELADA';
interface Tarefa {
  id: number; equipeId: number; equipeNome: string; responsavelId: number;
  responsavelNome: string; titulo: string; descricao: string;
  prioridade: string; prazo: string | null; status: Status; criadoEm: string;
}

@Component({
  selector: 'app-minhas-tarefas',
  standalone: true,
  templateUrl: './minhas-tarefas.component.html',
  styleUrl: './minhas-tarefas.component.scss',
})
export class MinhasTarefasComponent {
  private readonly api = inject(ApiService);
  readonly usuario = inject(AuthService).usuario;
  readonly tarefas = signal<Tarefa[]>([]);
  readonly carregando = signal(false);
  readonly salvandoId = signal<number | null>(null);
  readonly erro = signal('');
  readonly sucesso = signal('');
  readonly filtro = signal<'TODAS' | Status>('TODAS');
  readonly tarefasFiltradas = computed(() => this.tarefas().filter(t => this.filtro() === 'TODAS' || t.status === this.filtro()));
  readonly pendentes = computed(() => this.tarefas().filter(t => t.status === 'PENDENTE').length);
  readonly andamento = computed(() => this.tarefas().filter(t => t.status === 'EM_ANDAMENTO').length);
  readonly concluidas = computed(() => this.tarefas().filter(t => t.status === 'CONCLUIDA').length);
  readonly statusOpcoes: { valor: Status; texto: string }[] = [
    { valor: 'PENDENTE', texto: 'Pendente' },
    { valor: 'EM_ANDAMENTO', texto: 'Em andamento' },
    { valor: 'CONCLUIDA', texto: 'Concluída' },
    { valor: 'CANCELADA', texto: 'Cancelada' },
  ];

  constructor() { void this.carregar(); }

  async carregar(): Promise<void> {
    this.carregando.set(true);
    this.erro.set('');
    try {
      this.tarefas.set(await this.api.get<Tarefa[]>('/tarefas/minhas'));
    } catch (e) {
      this.erro.set(this.api.mensagemErro(e));
    } finally {
      this.carregando.set(false);
    }
  }

  async alterarStatus(tarefa: Tarefa, novoStatus: Status): Promise<void> {
    if (novoStatus === tarefa.status || this.salvandoId() !== null) return;
    this.salvandoId.set(tarefa.id);
    this.erro.set('');
    this.sucesso.set('');
    try {
      const atualizada = await this.api.patch<Tarefa>(`/tarefas/${tarefa.id}/status`, { status: novoStatus });
      this.tarefas.update(lista => lista.map(t => t.id === tarefa.id ? atualizada : t));
      this.sucesso.set('Situação da responsabilidade atualizada.');
    } catch (e) {
      this.erro.set(this.api.mensagemErro(e));
    } finally {
      this.salvandoId.set(null);
    }
  }

  statusTexto(status: string): string {
    return this.statusOpcoes.find(o => o.valor === status)?.texto ?? status;
  }
  prioridadeTexto(prioridade: string): string {
    return ({ BAIXA: 'Baixa', MEDIA: 'Média', ALTA: 'Alta' } as Record<string, string>)[prioridade] ?? prioridade;
  }
}
