import { Injectable } from '@angular/core';
import { HttpClient, HttpErrorResponse, HttpHeaders } from '@angular/common/http';
import { firstValueFrom } from 'rxjs';

export interface UsuarioSessao {
  id: number;
  nome: string;
  email: string;
  perfil: 'ADMINISTRADOR' | 'GESTOR' | 'USUARIO' | string;
  ativo: boolean;
}

interface CsrfResposta { token: string; }

@Injectable({ providedIn: 'root' })
export class ApiService {
  readonly baseUrl = 'http://localhost:8080/api';
  constructor(private readonly http: HttpClient) {}

  async obterCsrf(): Promise<string> {
    const resposta = await firstValueFrom(this.http.get<CsrfResposta>(`${this.baseUrl}/auth/csrf`, { withCredentials: true }));
    return resposta.token;
  }

  async login(email: string, senha: string): Promise<UsuarioSessao> {
    const token = await this.obterCsrf();
    return firstValueFrom(this.http.post<UsuarioSessao>(`${this.baseUrl}/auth/login`, { email, senha }, {
      withCredentials: true, headers: new HttpHeaders({ 'X-XSRF-TOKEN': token })
    }));
  }

  async obterUsuarioAtual(): Promise<UsuarioSessao> {
    return this.get<UsuarioSessao>('/auth/me');
  }

  async logout(): Promise<void> {
    await this.post<void>('/auth/logout', {});
  }

  get<T>(caminho: string): Promise<T> {
    return firstValueFrom(this.http.get<T>(`${this.baseUrl}${caminho}`, { withCredentials: true }));
  }

  async post<T>(caminho: string, corpo: unknown): Promise<T> {
    const token = await this.obterCsrf();
    return firstValueFrom(this.http.post<T>(`${this.baseUrl}${caminho}`, corpo, {
      withCredentials: true, headers: new HttpHeaders({ 'X-XSRF-TOKEN': token })
    }));
  }

  async patch<T>(caminho: string, corpo: unknown): Promise<T> {
    const token = await this.obterCsrf();
    return firstValueFrom(this.http.patch<T>(`${this.baseUrl}${caminho}`, corpo, {
      withCredentials: true, headers: new HttpHeaders({ 'X-XSRF-TOKEN': token })
    }));
  }

  async delete<T>(caminho: string): Promise<T> {
    const token = await this.obterCsrf();
    return firstValueFrom(this.http.delete<T>(`${this.baseUrl}${caminho}`, {
      withCredentials: true, headers: new HttpHeaders({ 'X-XSRF-TOKEN': token })
    }));
  }

  mensagemErro(erro: unknown): string {
    if (erro instanceof HttpErrorResponse) {
      if (erro.status === 0) return 'Não foi possível conectar ao backend. Confira se o Spring Boot está ativo em localhost:8080.';
      if (erro.status === 401) return 'Sua sessão expirou ou você não está autenticado. Entre novamente no SISPROC.';
      if (erro.status === 403) return 'Seu perfil não tem permissão para realizar esta operação.';
      const corpo = erro.error as { detail?: string; message?: string; error?: string; title?: string } | null;
      return corpo?.detail || corpo?.message || corpo?.error || corpo?.title || `A operação falhou (HTTP ${erro.status}).`;
    }
    return 'Ocorreu um erro inesperado. Tente novamente.';
  }
}
