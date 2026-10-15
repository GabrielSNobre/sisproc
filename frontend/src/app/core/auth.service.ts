import { Injectable, signal } from '@angular/core';
import { Router } from '@angular/router';
import { ApiService, UsuarioSessao } from './api.service';

@Injectable({ providedIn: 'root' })
export class AuthService {
  readonly usuario = signal<UsuarioSessao | null>(null);
  readonly carregando = signal(false);
  private sessaoVerificada = false;

  constructor(
    private readonly api: ApiService,
    private readonly router: Router
  ) {}

  async entrar(email: string, senha: string): Promise<void> {
    this.carregando.set(true);
    try {
      const usuario = await this.api.login(email.trim(), senha);
      this.usuario.set(usuario);
      this.sessaoVerificada = true;
      await this.router.navigateByUrl('/inicio');
    } finally {
      this.carregando.set(false);
    }
  }

  async verificarSessao(): Promise<boolean> {
    if (this.usuario()) return true;
    if (this.sessaoVerificada) return false;

    this.carregando.set(true);
    try {
      const usuario = await this.api.obterUsuarioAtual();
      this.usuario.set(usuario);
      this.sessaoVerificada = true;
      return true;
    } catch {
      this.usuario.set(null);
      this.sessaoVerificada = true;
      return false;
    } finally {
      this.carregando.set(false);
    }
  }

  async sair(): Promise<void> {
    try {
      await this.api.logout();
    } finally {
      this.usuario.set(null);
      this.sessaoVerificada = true;
      await this.router.navigateByUrl('/login');
    }
  }

  limparSessao(): void {
    this.usuario.set(null);
    this.sessaoVerificada = false;
  }
}
