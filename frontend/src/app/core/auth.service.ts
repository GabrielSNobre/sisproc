import { Injectable, signal } from '@angular/core';
import { Router } from '@angular/router';
import { ApiService, UsuarioSessao } from './api.service';

@Injectable({ providedIn: 'root' })
export class AuthService {
  readonly usuario = signal<UsuarioSessao | null>(null);
  readonly carregando = signal(false);
  private sessaoVerificada = false;
  private verificacaoEmAndamento: Promise<boolean> | null = null;
  private versaoSessao = 0;

  constructor(
    private readonly api: ApiService,
    private readonly router: Router
  ) {}

  async entrar(email: string, senha: string): Promise<void> {
    this.carregando.set(true);
    try {
      const usuario = await this.api.login(email.trim(), senha);
      this.versaoSessao++;
      this.usuario.set(usuario);
      this.sessaoVerificada = true;
      await this.router.navigateByUrl('/inicio');
    } finally {
      this.carregando.set(false);
    }
  }

  verificarSessao(): Promise<boolean> {
    if (this.usuario()) return Promise.resolve(true);
    if (this.verificacaoEmAndamento) return this.verificacaoEmAndamento;
    if (this.sessaoVerificada) return Promise.resolve(false);

    const versaoInicial = this.versaoSessao;
    this.carregando.set(true);

    const requisicao = (async (): Promise<boolean> => {
      try {
        const usuario = await this.api.obterUsuarioAtual();
        if (versaoInicial !== this.versaoSessao) return this.usuario() !== null;
        this.usuario.set(usuario);
        this.sessaoVerificada = true;
        return true;
      } catch {
        if (versaoInicial !== this.versaoSessao) return this.usuario() !== null;
        this.usuario.set(null);
        this.sessaoVerificada = true;
        return false;
      } finally {
        if (versaoInicial === this.versaoSessao) this.carregando.set(false);
      }
    })();

    this.verificacaoEmAndamento = requisicao;
    void requisicao.finally(() => {
      if (this.verificacaoEmAndamento === requisicao) {
        this.verificacaoEmAndamento = null;
      }
    });
    return requisicao;
  }

  async sair(): Promise<void> {
    this.versaoSessao++;
    try {
      await this.api.logout();
    } finally {
      this.usuario.set(null);
      this.sessaoVerificada = true;
      this.carregando.set(false);
      await this.router.navigateByUrl('/login');
    }
  }

  limparSessao(): void {
    this.versaoSessao++;
    this.usuario.set(null);
    this.sessaoVerificada = false;
    this.carregando.set(false);
  }
}
