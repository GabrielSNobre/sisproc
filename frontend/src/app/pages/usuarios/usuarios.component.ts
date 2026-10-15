import { Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ApiService, UsuarioSessao } from '../../core/api.service';

@Component({
  selector: 'app-usuarios', standalone: true, imports: [FormsModule],
  templateUrl: './usuarios.component.html', styleUrl: './usuarios.component.scss'
})
export class UsuariosComponent {
  private readonly api = inject(ApiService);
  readonly usuarios = signal<UsuarioSessao[]>([]);
  readonly carregando = signal(false);
  readonly salvando = signal(false);
  readonly erro = signal('');
  readonly sucesso = signal('');
  readonly formularioAberto = signal(false);
  readonly usuarioSenhaSelecionado = signal<UsuarioSessao | null>(null);
  readonly salvandoSenha = signal(false);
  readonly erroSenha = signal('');
  readonly sucessoSenha = signal('');

  nome = ''; email = ''; senha = ''; perfil = 'USUARIO';
  novaSenhaAdmin = ''; confirmarSenhaAdmin = '';

  constructor() { void this.carregar(); }

  async carregar(): Promise<void> {
    this.carregando.set(true); this.erro.set('');
    try { this.usuarios.set(await this.api.get<UsuarioSessao[]>('/usuarios')); }
    catch (e) { this.erro.set(this.api.mensagemErro(e)); }
    finally { this.carregando.set(false); }
  }

  abrirFormulario(): void { this.limparFormulario(); this.formularioAberto.set(true); }
  cancelar(): void { this.formularioAberto.set(false); this.limparFormulario(); }

  async cadastrar(): Promise<void> {
    this.erro.set(''); this.sucesso.set('');
    if (!this.nome.trim() || !this.email.trim() || !this.senha) { this.erro.set('Preencha nome, e-mail e senha.'); return; }
    if (this.senha.length < 8) { this.erro.set('A senha deve ter pelo menos 8 caracteres.'); return; }
    if (this.senha.length > 72) { this.erro.set('A senha deve ter no máximo 72 caracteres.'); return; }
    this.salvando.set(true);
    try {
      await this.api.post<UsuarioSessao>('/usuarios', { nome: this.nome.trim(), email: this.email.trim(), senha: this.senha, perfil: this.perfil });
      this.sucesso.set('Usuário cadastrado com sucesso.'); this.formularioAberto.set(false); this.limparFormulario(); await this.carregar();
    } catch (e) { this.erro.set(this.api.mensagemErro(e)); }
    finally { this.salvando.set(false); }
  }

  abrirTrocaSenha(usuario: UsuarioSessao): void {
    this.usuarioSenhaSelecionado.set(usuario);
    this.novaSenhaAdmin = '';
    this.confirmarSenhaAdmin = '';
    this.erroSenha.set('');
    this.sucessoSenha.set('');
  }

  fecharTrocaSenha(): void {
    this.usuarioSenhaSelecionado.set(null);
    this.novaSenhaAdmin = '';
    this.confirmarSenhaAdmin = '';
    this.erroSenha.set('');
  }

  async redefinirSenha(): Promise<void> {
    const usuario = this.usuarioSenhaSelecionado();
    this.erroSenha.set(''); this.sucessoSenha.set('');
    if (!usuario) return;
    if (!this.novaSenhaAdmin || !this.confirmarSenhaAdmin) {
      this.erroSenha.set('Informe a nova senha e sua confirmação.'); return;
    }
    if (this.novaSenhaAdmin.length < 8 || this.novaSenhaAdmin.length > 72) {
      this.erroSenha.set('A senha deve ter entre 8 e 72 caracteres.'); return;
    }
    if (this.novaSenhaAdmin !== this.confirmarSenhaAdmin) {
      this.erroSenha.set('A confirmação não corresponde à nova senha.'); return;
    }
    this.salvandoSenha.set(true);
    try {
      await this.api.post<void>(`/usuarios/${usuario.id}/senha`, { novaSenha: this.novaSenhaAdmin });
      this.sucesso.set(`Senha redefinida para ${usuario.nome}.`);
      this.fecharTrocaSenha();
    } catch (e) {
      this.erroSenha.set(this.api.mensagemErro(e));
    } finally {
      this.salvandoSenha.set(false);
    }
  }

  private limparFormulario(): void { this.nome = ''; this.email = ''; this.senha = ''; this.perfil = 'USUARIO'; }
  perfilLabel(perfil: string): string { return ({ ADMINISTRADOR: 'Administrador', GESTOR: 'Gestor', USUARIO: 'Usuário' } as Record<string, string>)[perfil] || perfil; }
}
