import { Component, ElementRef, ViewChild, inject, signal } from '@angular/core';
import { DatePipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute } from '@angular/router';
import { ApiService } from '../../core/api.service';

type Documento = {
  id: number; tarefaId: number; equipeOrigemId: number; equipeOrigemNome: string;
  equipeDestinoId: number | null; equipeDestinoNome: string | null;
  autorId: number; autorNome: string; tipo: string; assunto: string; conteudo: string; criadoEm: string;
};
type Resposta = { id: number; documentoId: number; autorId: number; autorNome: string; conteudo: string; criadoEm: string };
type Movimentacao = {
  tipoEvento: 'DOCUMENTO' | 'RESPOSTA'; id: number; documentoId: number; tarefaId: number;
  tipoDocumento: string; assunto: string; conteudo: string; autorId: number; autorNome: string;
  equipeAutoraId: number | null; equipeAutoraNome: string | null;
  equipeDestinoId: number | null; equipeDestinoNome: string | null; criadoEm: string;
};
type Equipe = { id: number; nome: string };

@Component({
  selector: 'app-processos', standalone: true, imports: [FormsModule, DatePipe],
  templateUrl: './processos.component.html', styleUrl: './processos.component.scss'
})
export class ProcessosComponent {
  @ViewChild('editorDocumento') editorDocumento?: ElementRef<HTMLDivElement>;
  @ViewChild('editorResposta') editorResposta?: ElementRef<HTMLDivElement>;
  private api = inject(ApiService);
  private route = inject(ActivatedRoute);
  readonly documentos = signal<Documento[]>([]);
  readonly linhaDoTempo = signal<Movimentacao[]>([]);
  readonly recebidos = signal<Documento[]>([]);
  readonly respostas = signal<Resposta[]>([]);
  readonly selecionado = signal<Documento | null>(null);
  readonly erro = signal(''); readonly sucesso = signal(''); readonly carregando = signal(false);
  readonly tipos = ['ACAO', 'MINUTA', 'DESPACHO', 'PARECER', 'SOLICITACAO', 'OUTRO'];
  tarefaId = '';
  tipo = 'ACAO'; assunto = ''; conteudo = ''; equipeDestinoId = '';
  respostaTexto = '';
  equipes: Equipe[] = [];
  equipesErro = '';
  constructor() {
    const id = this.route.snapshot.queryParamMap.get('tarefaId');
    if (id && /^\d+$/.test(id)) this.tarefaId = id;
    void this.iniciar();
  }
  async iniciar() {
    await this.carregarRecebidos();
    if (this.tarefaId) await this.carregarTarefa();
    // Endpoint de listagem completa é administrativo; falhas aqui não impedem o uso.
    try { this.equipes = await this.api.get<Equipe[]>('/processos/equipes-disponiveis'); }
    catch (e) { this.equipes = []; this.equipesErro = this.api.mensagemErro(e); }
  }
  formatar(editor: 'documento' | 'resposta', comando: string, valor?: string) {
    const elemento = editor === 'documento' ? this.editorDocumento?.nativeElement : this.editorResposta?.nativeElement;
    if (!elemento) return;
    elemento.focus();
    document.execCommand(comando, false, valor);
    this.atualizarEditor(editor);
  }
  atualizarEditor(editor: 'documento' | 'resposta') {
    const elemento = editor === 'documento' ? this.editorDocumento?.nativeElement : this.editorResposta?.nativeElement;
    if (!elemento) return;
    if (editor === 'documento') this.conteudo = elemento.innerHTML;
    else this.respostaTexto = elemento.innerHTML;
  }
  private temTexto(html: string): boolean {
    const el = document.createElement('div');
    el.innerHTML = html;
    return !!el.textContent?.trim();
  }
  private limparEditor(editor: 'documento' | 'resposta') {
    const elemento = editor === 'documento' ? this.editorDocumento?.nativeElement : this.editorResposta?.nativeElement;
    if (elemento) elemento.innerHTML = '';
  }
  private idTarefa(): number | null {
    const id = Number(this.tarefaId);
    return Number.isSafeInteger(id) && id > 0 ? id : null;
  }
  async carregarTarefa() {
    const id = this.idTarefa();
    if (id === null) { this.erro.set('Informe o ID numérico de uma tarefa.'); return; }
    this.erro.set(''); this.carregando.set(true);
    try {
      const [docs, eventos] = await Promise.all([
        this.api.get<Documento[]>(`/processos/tarefa/${id}`),
        this.api.get<Movimentacao[]>(`/processos/tarefa/${id}/linha-do-tempo`)
      ]);
      this.documentos.set(docs); this.linhaDoTempo.set(eventos);
    }
    catch (e) { this.erro.set(this.api.mensagemErro(e)); }
    finally { this.carregando.set(false); }
  }
  async carregarRecebidos() {
    try { this.recebidos.set(await this.api.get<Documento[]>('/processos/recebidos')); }
    catch (e) { this.erro.set(this.api.mensagemErro(e)); }
  }
  async criar() {
    const id = this.idTarefa();
    if (id === null || !this.assunto.trim() || !this.temTexto(this.conteudo)) {
      this.erro.set('Preencha a tarefa, o assunto e o conteúdo.'); return;
    }
    this.erro.set(''); this.sucesso.set('');
    const destino = this.equipeDestinoId.trim() ? Number(this.equipeDestinoId) : null;
    if (destino !== null && (!Number.isSafeInteger(destino) || destino <= 0)) {
      this.erro.set('O ID da equipe destinatária deve ser um número positivo.'); return;
    }
    try {
      await this.api.post<Documento>('/processos', {
        tarefaId: id, tipo: this.tipo, assunto: this.assunto.trim(), conteudo: this.conteudo.trim(), equipeDestinoId: destino
      });
      this.assunto = ''; this.conteudo = ''; this.equipeDestinoId = ''; this.limparEditor('documento');
      this.sucesso.set('Documento registrado no histórico da tarefa.');
      await Promise.all([this.carregarTarefa(), this.carregarRecebidos()]);
    } catch (e) { this.erro.set(this.api.mensagemErro(e)); }
  }
  async abrirDocumento(id: number) {
    const doc = this.documentos().find(d => d.id === id);
    if (doc) await this.abrir(doc);
  }
  async abrir(doc: Documento) {
    this.selecionado.set(doc); this.respostas.set([]); this.erro.set('');
    try { this.respostas.set(await this.api.get<Resposta[]>(`/processos/${doc.id}/respostas`)); }
    catch (e) { this.erro.set(this.api.mensagemErro(e)); }
  }
  async responder() {
    const doc = this.selecionado();
    if (!doc || !this.temTexto(this.respostaTexto)) return;
    this.erro.set('');
    try {
      await this.api.post<Resposta>(`/processos/${doc.id}/respostas`, { conteudo: this.respostaTexto.trim() });
      this.respostaTexto = ''; this.limparEditor('resposta');
      await this.abrir(doc);
      if (this.idTarefa() === doc.tarefaId) await this.carregarTarefa();
      await this.carregarRecebidos();
      this.sucesso.set('Resposta registrada.');
    } catch (e) { this.erro.set(this.api.mensagemErro(e)); }
  }
}
