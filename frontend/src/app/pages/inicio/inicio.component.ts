import { Component, computed, inject } from '@angular/core';
import { RouterLink } from '@angular/router';
import { AuthService } from '../../core/auth.service';

@Component({
  selector: 'app-inicio',
  standalone: true,
  imports: [RouterLink],
  templateUrl: './inicio.component.html',
  styleUrl: './inicio.component.scss'
})
export class InicioComponent {
  private readonly auth = inject(AuthService);

  readonly usuario = this.auth.usuario;

  readonly primeiroNome = computed(
    () => this.usuario()?.nome?.trim().split(/\s+/)[0] || 'usuário'
  );
}
