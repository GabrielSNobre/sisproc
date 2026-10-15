import { Component, inject, signal } from '@angular/core';
import { Router, RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { AuthService } from './core/auth.service';

@Component({ selector: 'app-root', standalone: true, imports: [RouterOutlet, RouterLink, RouterLinkActive], templateUrl: './app.html', styleUrl: './app.scss' })
export class App {
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);
  readonly usuario = this.auth.usuario;
  readonly menuAberto = signal(false);
  get estaNoLogin(): boolean { return this.router.url.startsWith('/login'); }
  async sair(): Promise<void> { await this.auth.sair(); }
}
