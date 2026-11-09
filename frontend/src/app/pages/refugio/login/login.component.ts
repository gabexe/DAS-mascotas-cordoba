import { Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { AuthService } from '../../../core/services/auth.service';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [FormsModule],
  template: `
    <div class="login-page">
      <!-- Curva de fondo en Celeste Bruma, fiel a prototipo_1.png -->
      <svg class="login-bg-svg" viewBox="0 0 1920 1080" preserveAspectRatio="none" aria-hidden="true">
        <path 
          d="M 1111 0 C 1108.3 13.3, 1098.0 53.3, 1095 80 C 1092.0 106.7, 1091.5 133.3, 1093 160 C 1094.5 186.7, 1097.8 213.3, 1104 240 C 1110.2 266.7, 1118.5 293.3, 1130 320 C 1141.5 346.7, 1186.2 370.0, 1173 400 C 1159.8 430.0, 1095.5 466.7, 1051 500 C 1006.5 533.3, 948.0 566.7, 906 600 C 864.0 633.3, 830.5 666.7, 799 700 C 767.5 733.3, 740.8 766.7, 717 800 C 693.2 833.3, 673.3 866.7, 656 900 C 638.7 933.3, 623.8 970.0, 613 1000 C 602.2 1030.0, 594.7 1066.7, 591 1080 L 1920 1080 L 1920 0 Z" 
          fill="var(--color-accent-soft)" 
        />
      </svg>

      <div class="login-layout">
        <!-- Columna Izquierda: Identidad y Formulario -->
        <div class="login-left-col">
          <header class="brand-header">
            <h1 class="brand-title">Mascotas Córdoba</h1>
            <h2 class="login-subtitle">Iniciar Sesión</h2>
          </header>

          <form (ngSubmit)="onSubmit()" class="login-form">
            <div class="field-group">
              <input 
                type="text" 
                id="cuil" 
                name="cuil"
                [(ngModel)]="cuil" 
                placeholder="Ingrese su CUIL" 
                required
                autocomplete="username"
                class="login-input"
              />
            </div>

            <div class="field-group">
              <input 
                type="password" 
                id="clave" 
                name="clave"
                [(ngModel)]="clave" 
                placeholder="••••••••" 
                required
                autocomplete="current-password"
                class="login-input"
              />
            </div>

            @if (errorMessage()) {
              <div class="login-error" role="alert">
                {{ errorMessage() }}
              </div>
            }

            <button type="submit" [disabled]="loading()" class="btn-enter" id="btnIngresar">
              @if (loading()) {
                <span>Ingresando...</span>
              } @else {
                <span>Ingresar al Portal</span>
              }
            </button>
          </form>

          <!-- Acceso rápido discreto (sin cajas llamativas ni sombras) -->
          <div class="discreet-demo-area">
            <span class="demo-subtle-label">Acceso de prueba:</span>
            <button type="button" (click)="usarCredencialesDemo('20-12345678-9', '1234')" class="demo-text-btn">
              Huellitas Cordobesas
            </button>
            <span class="demo-sep">·</span>
            <button type="button" (click)="usarCredencialesDemo('27-12345678-4', '1234')" class="demo-text-btn">
              Patitas del Sur
            </button>
          </div>
        </div>

        <!-- Columna Derecha / Centro: Huella e Ilustración -->
        <div class="login-right-col">
          <!-- Huella celeste flotante en el centro sin efecto de glow -->
          <div class="floating-paw-wrapper" aria-hidden="true">
            <svg class="floating-paw-icon" viewBox="0 0 24 24">
              <ellipse cx="12" cy="15.8" rx="4.6" ry="3.9" />
              <circle cx="6.2" cy="10" r="2.3" />
              <circle cx="9.8" cy="6.6" r="2.2" />
              <circle cx="14.2" cy="6.6" r="2.2" />
              <circle cx="17.8" cy="10" r="2.3" />
            </svg>
          </div>

          <!-- Ilustración apoyada en la base inferior sin glow -->
          <div class="illustration-wrapper">
            <img 
              src="assets/ilustracion-login.png" 
              alt="Perro y gato celebrando adopción en Mascotas Córdoba" 
              class="login-illustration"
            />
          </div>
        </div>
      </div>
    </div>
  `,
  styleUrl: './login.component.css'
})
export class LoginComponent {
  private authService = inject(AuthService);
  private router = inject(Router);

  cuil = '';
  clave = '';
  loading = signal(false);
  errorMessage = signal<string | null>(null);

  usarCredencialesDemo(cuil: string, clave: string): void {
    this.cuil = cuil;
    this.clave = clave;
    this.errorMessage.set(null);
  }

  async onSubmit(): Promise<void> {
    if (!this.cuil.trim() || !this.clave.trim()) {
      this.errorMessage.set('Por favor complete todos los campos');
      return;
    }

    this.loading.set(true);
    this.errorMessage.set(null);

    try {
      await this.authService.login(this.cuil, this.clave);
      this.router.navigate(['/publicaciones']);
    } catch (err: any) {
      this.errorMessage.set(err.message || 'Credenciales incorrectas');
    } finally {
      this.loading.set(false);
    }
  }
}
