import { Component, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { HttpErrorResponse } from '@angular/common/http';
import { AuthService } from '../../core/auth.service';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [FormsModule],
  templateUrl: './login.component.html',
  styleUrl: './login.component.scss'
})
export class LoginComponent {
  email = '';
  senha = '';
  readonly erro = signal('');
  readonly enviando = signal(false);
  readonly mostrarSenha = signal(false);

  constructor(private readonly auth: AuthService) {}

  async entrar(): Promise<void> {
    this.erro.set('');
    if (!this.email.trim() || !this.senha) {
      this.erro.set('Informe seu e-mail e sua senha.');
      return;
    }

    this.enviando.set(true);
    try {
      await this.auth.entrar(this.email, this.senha);
    } catch (e) {
      const erro = e as HttpErrorResponse;
      if (erro.status === 401) {
        this.erro.set('E-mail ou senha inválidos. Confira os dados e tente novamente.');
      } else if (erro.status === 0) {
        this.erro.set('Não foi possível conectar ao servidor. Verifique se o backend está ativo em localhost:8080.');
      } else if (erro.status === 403) {
        this.erro.set('A solicitação foi recusada por segurança (CSRF). Atualize a página e tente novamente.');
      } else {
        this.erro.set(erro.error?.detail || erro.error?.message || 'Não foi possível realizar o login.');
      }
    } finally {
      this.enviando.set(false);
    }
  }
}
