import { Component, inject } from '@angular/core';
import { ActivatedRoute } from '@angular/router';

@Component({
  selector: 'app-placeholder',
  standalone: true,
  template: `
    <section class="placeholder">
      <div class="eyebrow">{{ etiqueta }}</div>
      <div class="placeholder-icon">▤</div>
      <h1>{{ titulo }}</h1>
      <p>{{ descricao }}</p>
      <div class="status"><span></span> Estrutura inicial criada</div>
      <div class="placeholder-note">
        <strong>Próxima etapa de implementação</strong>
        <p>Esta tela já está integrada à navegação. A funcionalidade será conectada aos endpoints existentes após confirmar o formato dos dados e as permissões do backend.</p>
      </div>
    </section>
  `,
  styles: [`
    :host { display:block; }
    .placeholder { max-width: 720px; padding: 12px 0; }
    .eyebrow { color:#0e7490; font-size:10px; font-weight:800; letter-spacing:.15em; }
    .placeholder-icon { display:grid; width:54px; height:54px; place-items:center; margin-top:27px; border-radius:15px; background:#e3f3f7; color:#0e7490; font-size:27px; }
    h1 { margin:20px 0 8px; color:#173449; font-size:30px; letter-spacing:-.04em; }
    p { color:#728493; font-size:13px; line-height:1.8; }
    .status { display:inline-flex; align-items:center; gap:8px; margin-top:10px; padding:8px 11px; border:1px solid #dbece8; border-radius:20px; background:#f0faf6; color:#33735e; font-size:11px; }
    .status span { width:7px; height:7px; border-radius:50%; background:#10b981; }
    .placeholder-note { margin-top:32px; padding:18px; border:1px solid #e2eaf0; border-radius:12px; background:#fff; }
    .placeholder-note strong { color:#30495c; font-size:12px; }
    .placeholder-note p { margin-bottom:0; font-size:12px; }
  `]
})
export class PlaceholderComponent {
  private readonly route = inject(ActivatedRoute);
  readonly titulo = this.route.snapshot.data['titulo'] ?? 'Área do sistema';
  readonly descricao = this.route.snapshot.data['descricao'] ?? '';
  readonly etiqueta = this.route.snapshot.data['etiqueta'] ?? 'SISPROC';
}
