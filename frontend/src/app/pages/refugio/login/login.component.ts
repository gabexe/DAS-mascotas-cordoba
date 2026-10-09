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
  styles: [`
    .login-page {
      position: relative;
      min-height: 100vh;
      width: 100%;
      background-color: var(--color-bg-canvas);
      display: flex;
      align-items: center;
      justify-content: center;
      overflow: hidden;
      font-family: var(--font-body);
    }

    /* Fondo orgánico de onda en Celeste Bruma */
    .login-bg-svg {
      position: absolute;
      top: 0;
      left: 0;
      width: 100%;
      height: 100%;
      pointer-events: none;
      z-index: 1;
    }

    .login-layout {
      position: relative;
      z-index: 2;
      width: 100%;
      max-width: 1600px;
      min-height: 100vh;
      padding: 3rem 5vw;
      display: grid;
      grid-template-columns: 1.05fr 1fr;
      align-items: center;
      gap: 3rem;
    }

    /* Columna Izquierda: Formulario e Identidad */
    .login-left-col {
      max-width: 540px;
      display: flex;
      flex-direction: column;
      gap: 2rem;
      z-index: 3;
    }

    .brand-header {
      display: flex;
      flex-direction: column;
      gap: 0.4rem;
    }

    /* Título con gran jerarquía visual según prototipo_1.png */
    .brand-title {
      margin: 0;
      font-family: var(--font-brand);
      font-size: clamp(3.2rem, 5.2vw, 5.4rem);
      font-weight: 700;
      color: var(--color-accent); /* Celeste Principal #0284C7 */
      line-height: 1.02;
      letter-spacing: -0.5px;
    }

    .login-subtitle {
      margin: 0.2rem 0 0 0;
      font-family: var(--font-heading);
      font-size: 1.6rem;
      font-weight: 700;
      color: var(--color-text-main); /* Carbón Cálido */
      letter-spacing: -0.2px;
    }

    .login-form {
      display: flex;
      flex-direction: column;
      gap: 1.5rem;
      margin-top: 0.5rem;
    }

    .field-group {
      display: flex;
      flex-direction: column;
    }

    .login-input {
      width: 100%;
      max-width: 440px;
      padding: 0.8rem 0.2rem;
      background: transparent;
      border: none;
      border-bottom: 2px solid var(--color-border-subtle);
      font-family: var(--font-body);
      font-size: 1.15rem;
      color: var(--color-text-main);
      outline: none;
      transition: border-color 0.2s ease;
    }

    .login-input::placeholder {
      color: var(--color-text-subtle);
      font-size: 1.05rem;
    }

    .login-input:focus {
      border-bottom-color: var(--color-accent);
    }

    .login-error {
      background-color: var(--color-bg-muted);
      border: 1px solid var(--color-border-strong);
      color: #B91C1C;
      padding: 0.65rem 1rem;
      border-radius: var(--radius-sm);
      font-size: 0.9rem;
      font-weight: 600;
      max-width: 440px;
    }

    /* Botón Ingresar al Portal: Píldora Celeste SIN efecto glow */
    .btn-enter {
      margin-top: 0.8rem;
      background-color: var(--color-accent);
      color: var(--color-text-inverse); /* Crema Lienzo, sin blancos puros */
      border: none;
      padding: 0.9rem 2.8rem;
      border-radius: var(--radius-pill);
      font-family: var(--font-heading);
      font-size: 1.1rem;
      font-weight: 700;
      cursor: pointer;
      display: inline-flex;
      align-items: center;
      justify-content: center;
      transition: background-color 0.15s ease, transform 0.1s ease;
      box-shadow: none; /* CERO GLOW */
      max-width: 290px;
    }

    .btn-enter:hover:not(:disabled) {
      background-color: var(--color-accent-hover);
      transform: translateY(-1px);
      box-shadow: none; /* CERO GLOW */
    }

    .btn-enter:disabled {
      background-color: var(--color-text-subtle);
      cursor: not-allowed;
      box-shadow: none;
    }

    /* Acceso de prueba discreto */
    .discreet-demo-area {
      margin-top: 0.5rem;
      display: flex;
      align-items: center;
      gap: 0.45rem;
      font-size: 0.8rem;
      color: var(--color-text-subtle);
    }

    .demo-subtle-label {
      color: var(--color-text-subtle);
      font-size: 0.78rem;
    }

    .demo-text-btn {
      background: transparent;
      border: none;
      padding: 0.1rem 0.2rem;
      font-family: var(--font-body);
      font-size: 0.8rem;
      color: var(--color-text-muted);
      text-decoration: underline;
      cursor: pointer;
      transition: color 0.15s ease;
    }

    .demo-text-btn:hover {
      color: var(--color-accent);
    }

    .demo-sep {
      color: var(--color-border-strong);
    }

    /* Columna Derecha: Huella e Ilustración */
    .login-right-col {
      position: relative;
      display: flex;
      align-items: flex-end;
      justify-content: flex-end;
      height: 100%;
      min-height: 520px;
    }

    /* Huella de perro centrada SIN efecto glow */
    .floating-paw-wrapper {
      position: absolute;
      top: 36%;
      left: -4%;
      transform: rotate(-18deg);
      z-index: 3;
      filter: none; /* CERO GLOW */
    }

    .floating-paw-icon {
      width: 72px;
      height: 72px;
      fill: var(--color-accent);
    }

    .illustration-wrapper {
      position: relative;
      width: 100%;
      max-width: 680px;
      display: flex;
      justify-content: flex-end;
      align-items: flex-end;
    }

    .login-illustration {
      width: 100%;
      max-height: 80vh;
      object-fit: contain;
      filter: none; /* CERO GLOW */
      display: block;
    }

    @media (max-width: 1024px) {
      .login-layout {
        grid-template-columns: 1fr;
        gap: 2.5rem;
        padding: 2.5rem 1.5rem;
        min-height: auto;
      }

      .login-right-col {
        min-height: 380px;
        order: -1;
        justify-content: center;
      }

      .floating-paw-wrapper {
        top: 15%;
        left: 2%;
      }

      .illustration-wrapper {
        max-width: 440px;
      }
    }
  `]
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
