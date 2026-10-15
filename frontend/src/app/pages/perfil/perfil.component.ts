import { Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ApiService, UsuarioSessao } from '../../core/api.service';
import { AuthService } from '../../core/auth.service';

@Component({
  selector: 'app-perfil',
  standalone: true,
  imports: [FormsModule],
  templateUrl: './perfil.component.html',
  styleUrl: './perfil.component.scss',
})
export class PerfilComponent {
  private readonly api = inject(ApiService);
  private readonly auth = inject(AuthService);
  readonly usuario = signal<UsuarioSessao | null>(null);
  readonly carregando = signal(false);
  readonly salvando = signal(false);
  readonly alterandoSenha = signal(false);
  readonly erro = signal('');
  readonly sucesso = signal('');
  readonly erroSenha = signal('');
  readonly sucessoSenha = signal('');
  nome = '';
  email = '';
  senhaAtual = '';
  novaSenha = '';
  confirmarSenha = '';

  constructor() { void this.carregar(); }

  async carregar(): Promise<void> {
    this.carregando.set(true);
    this.erro.set('');
    try {
      const u = await this.api.get<UsuarioSessao>('/usuarios/me');
      this.usuario.set(u);
      this.nome = u.nome;
      this.email = u.email;
    } catch (e) {
      this.erro.set(this.api.mensagemErro(e));
    } finally {
      this.carregando.set(false);
    }
  }

  async salvar(): Promise<void> {
    this.erro.set(''); this.sucesso.set('');
    if (!this.nome.trim() || !this.email.trim()) { this.erro.set('Nome e e-mail são obrigatórios.'); return; }
    this.salvando.set(true);
    try {
      const u = await this.api.patch<UsuarioSessao>('/usuarios/me', { nome: this.nome.trim(), email: this.email.trim() });
      this.usuario.set(u);
      this.auth.usuario.set(u);
      this.sucesso.set('Perfil atualizado com sucesso.');
    } catch (e) {
      this.erro.set(this.api.mensagemErro(e));
    } finally {
      this.salvando.set(false);
    }
  }

  async alterarSenha(): Promise<void> {
    this.erroSenha.set(''); this.sucessoSenha.set('');
    if (!this.senhaAtual || !this.novaSenha || !this.confirmarSenha) {
      this.erroSenha.set('Preencha todos os campos de senha.'); return;
    }
    if (this.novaSenha.length < 8) {
      this.erroSenha.set('A nova senha deve ter pelo menos 8 caracteres.'); return;
    }
    if (this.novaSenha.length > 72) {
      this.erroSenha.set('A nova senha excede o limite de 72 caracteres.'); return;
    }
    if (this.novaSenha !== this.confirmarSenha) {
      this.erroSenha.set('A confirmação não corresponde à nova senha.'); return;
    }
    if (this.senhaAtual === this.novaSenha) {
      this.erroSenha.set('A nova senha deve ser diferente da senha atual.'); return;
    }

    this.alterandoSenha.set(true);
    try {
      await this.api.post<void>('/usuarios/me/senha', {
        senhaAtual: this.senhaAtual,
        novaSenha: this.novaSenha,
      });
      this.senhaAtual = '';
      this.novaSenha = '';
      this.confirmarSenha = '';
      this.sucessoSenha.set('Senha alterada com sucesso.');
    } catch (e) {
      this.erroSenha.set(this.api.mensagemErro(e));
    } finally {
      this.alterandoSenha.set(false);
    }
  }

  perfilLabel(perfil: string): string {
    return ({ ADMINISTRADOR: 'Administrador', GESTOR: 'Gestor', USUARIO: 'Usuário' } as Record<string, string>)[perfil] || perfil;
  }
}
