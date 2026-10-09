import { Component, ElementRef, HostListener, inject, signal } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { AuthService } from '../../../core/services/auth.service';

@Component({
  selector: 'app-navbar',
  standalone: true,
  imports: [RouterLink],
  template: `
    <header class="navbar">
      <div class="navbar-container">
        
        <!-- Logo Institucional: Huella + Tipografía DynaPuff -->
        <a [routerLink]="authService.isAuthenticated() ? '/publicaciones' : '/login'" class="brand-link" title="Mascotas Córdoba">
          <svg class="brand-paw-icon" viewBox="0 0 24 24" fill="currentColor">
            <ellipse cx="12" cy="15.5" rx="4.5" ry="3.8" />
            <circle cx="6.5" cy="10" r="2.2" />
            <circle cx="10" cy="6.8" r="2.1" />
            <circle cx="14" cy="6.8" r="2.1" />
            <circle cx="17.5" cy="10" r="2.2" />
          </svg>
          <span class="brand-text">Mascotas Córdoba</span>
        </a>

        <!-- Área de Usuario y Perfil -->
        <div class="user-menu-area">
          @if (authService.isAuthenticated()) {
            <button 
              type="button" 
              class="avatar-btn" 
              (click)="toggleMenu($event)" 
              [attr.aria-expanded]="menuOpen()"
              title="Cuenta de Refugio"
              id="userMenuButton">
              <!-- Ícono de Perfil oficial: icono-perfil.png -->
              <img 
                src="assets/icono-perfil.png" 
                alt="Perfil de Usuario" 
                class="avatar-img"
              />
            </button>

            <!-- Menú Desplegable con Espacios Vacíos y Diseño Respirado -->
            @if (menuOpen()) {
              <div class="dropdown-menu" (click)="$event.stopPropagation()" role="dialog" aria-label="Información de Cuenta">
                
                <div class="dropdown-header">
                  <div class="shelter-badge">
                    <span class="badge-dot" aria-hidden="true"></span>
                    <span>Refugio Habilitado</span>
                  </div>
                  <h3 class="shelter-name">
                    {{ authService.currentRefugio()?.razon_social || 'Asociación Protectora Huellitas Cordobesas' }}
                  </h3>
                </div>

                <div class="dropdown-body">
                  <div class="account-info-group">
                    <span class="info-label">CUIL</span>
                    <span class="info-value">{{ authService.currentUser()?.cuil }}</span>
                  </div>

                  <div class="account-info-group">
                    <span class="info-label">Correo Electrónico</span>
                    <span class="info-value info-email">{{ authService.currentUser()?.correo }}</span>
                  </div>

                  @if (authService.currentRefugio()?.habilitacion_municipal) {
                    <div class="account-info-group">
                      <span class="info-label">Habilitación Municipal</span>
                      <span class="info-value info-code">{{ authService.currentRefugio()?.habilitacion_municipal }}</span>
                    </div>
                  }
                </div>
                
                <div class="dropdown-divider"></div>

                <div class="dropdown-footer">
                  <button type="button" (click)="cerrarSesion()" class="btn-logout" id="logoutButton">
                    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" class="logout-icon" aria-hidden="true">
                      <path d="M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4" />
                      <polyline points="16 17 21 12 16 7" />
                      <line x1="21" y1="12" x2="9" y2="12" />
                    </svg>
                    <span>Cerrar Sesión</span>
                  </button>
                </div>
              </div>
            }
          } @else {
            <a routerLink="/login" class="login-link">
              Iniciar Sesión
            </a>
          }
        </div>

      </div>
    </header>
  `,
  styles: [`
    .navbar {
      background-color: var(--color-bg-canvas);
      padding: 0.85rem 0;
      width: 100%;
    }

    .navbar-container {
      width: 95%;
      margin: 0 auto;
      display: flex;
      align-items: center;
      justify-content: space-between;
    }

    /* Logo de la marca: lleva al inicio / login */
    .brand-link {
      display: inline-flex;
      align-items: center;
      gap: 0.75rem;
      text-decoration: none;
      color: var(--color-text-main);
      user-select: none;
      transition: opacity 0.15s ease, transform 0.1s ease;
    }

    .brand-link:hover {
      opacity: 0.85;
      transform: translateY(-1px);
    }

    .brand-paw-icon {
      width: 32px;
      height: 32px;
      color: var(--color-text-main);
      display: inline-block;
      flex-shrink: 0;
    }

    .brand-text {
      font-family: var(--font-brand);
      font-size: 1.65rem;
      font-weight: 700;
      letter-spacing: 0.2px;
      color: var(--color-text-main);
      line-height: 1;
    }

    /* Área de usuario */
    .user-menu-area {
      position: relative;
    }

    .avatar-btn {
      background: transparent;
      border: none;
      cursor: pointer;
      padding: 0;
      display: flex;
      align-items: center;
      justify-content: center;
      border-radius: 50%;
      transition: transform 0.15s ease, opacity 0.15s ease;
    }

    .avatar-btn:hover {
      transform: scale(1.08);
      opacity: 0.85;
    }

    .avatar-img {
      width: 42px;
      height: 42px;
      object-fit: contain;
      display: block;
      border-radius: 50%;
    }

    .login-link {
      text-decoration: none;
      font-family: var(--font-heading);
      color: var(--color-accent);
      font-weight: 700;
      font-size: 0.95rem;
      padding: 0.45rem 1.2rem;
      border-radius: var(--radius-pill);
      border: 1.5px solid var(--color-accent);
      transition: all 0.15s ease;
    }

    .login-link:hover {
      background: var(--color-accent);
      color: var(--color-text-inverse);
    }

    /* Menú Desplegable con Espacios Vacíos y Respiración Visual */
    .dropdown-menu {
      position: absolute;
      top: calc(100% + 14px);
      right: 0;
      width: 370px;
      background: var(--color-bg-surface); /* Crema Lienzo #FAF7F2, sin blancos puros */
      border: 1px solid var(--color-border-subtle);
      border-radius: 24px;
      box-shadow: var(--shadow-modal);
      padding: 2rem 2.2rem;
      z-index: 1000;
      display: flex;
      flex-direction: column;
      gap: 1.6rem;
      animation: menuFadeIn 0.18s cubic-bezier(0.16, 1, 0.3, 1);
    }

    @keyframes menuFadeIn {
      from {
        opacity: 0;
        transform: translateY(-8px) scale(0.97);
      }
      to {
        opacity: 1;
        transform: translateY(0) scale(1);
      }
    }

    .dropdown-header {
      display: flex;
      flex-direction: column;
      gap: 0.75rem;
    }

    .shelter-badge {
      display: inline-flex;
      align-items: center;
      gap: 0.5rem;
      align-self: flex-start;
      font-family: var(--font-heading);
      font-size: 0.75rem;
      font-weight: 700;
      color: var(--color-accent);
      background-color: var(--color-accent-soft);
      padding: 0.3rem 0.85rem;
      border-radius: var(--radius-pill);
      text-transform: uppercase;
      letter-spacing: 0.5px;
    }

    .badge-dot {
      width: 7px;
      height: 7px;
      border-radius: 50%;
      background-color: var(--color-accent);
    }

    .shelter-name {
      margin: 0;
      font-family: var(--font-heading);
      font-size: 1.25rem;
      font-weight: 700;
      color: var(--color-text-main);
      line-height: 1.35;
    }

    /* Cuerpo con espacio y legibilidad clara */
    .dropdown-body {
      display: flex;
      flex-direction: column;
      gap: 1.1rem;
    }

    .account-info-group {
      display: flex;
      flex-direction: column;
      gap: 0.2rem;
    }

    .info-label {
      font-family: var(--font-body);
      font-size: 0.78rem;
      color: var(--color-text-subtle);
      text-transform: uppercase;
      letter-spacing: 0.4px;
      font-weight: 700;
    }

    .info-value {
      font-family: var(--font-body);
      font-size: 0.96rem;
      color: var(--color-text-main);
      font-weight: 600;
      word-break: break-all;
    }

    .info-email {
      color: var(--color-text-muted);
    }

    .info-code {
      font-family: monospace;
      font-size: 0.85rem;
      color: var(--color-text-muted);
    }

    .dropdown-divider {
      height: 1px;
      background-color: var(--color-border-subtle);
      width: 100%;
      margin: 0.2rem 0;
    }

    .dropdown-footer {
      display: flex;
      flex-direction: column;
    }

    /* Botón Cerrar Sesión: limpio, amigable y bien espaciado */
    .btn-logout {
      width: 100%;
      display: inline-flex;
      align-items: center;
      justify-content: center;
      gap: 0.65rem;
      background-color: var(--color-bg-muted);
      border: 1px solid var(--color-border-subtle);
      padding: 0.75rem 1.4rem;
      border-radius: var(--radius-pill);
      font-family: var(--font-heading);
      font-size: 0.92rem;
      font-weight: 700;
      color: var(--color-text-main);
      cursor: pointer;
      transition: all 0.15s ease;
    }

    .btn-logout:hover {
      background-color: #FEE2E2;
      border-color: #FECACA;
      color: #B91C1C;
    }

    .logout-icon {
      width: 18px;
      height: 18px;
    }

    @media (max-width: 600px) {
      .dropdown-menu {
        width: calc(100vw - 2rem);
        right: -1rem;
        padding: 1.5rem;
      }
    }
  `]
})
export class NavbarComponent {
  authService = inject(AuthService);
  private router = inject(Router);
  private elementRef = inject(ElementRef);

  menuOpen = signal(false);

  toggleMenu(event: MouseEvent): void {
    event.stopPropagation();
    this.menuOpen.update(v => !v);
  }

  @HostListener('document:click', ['$event'])
  onDocumentClick(event: MouseEvent): void {
    if (this.menuOpen() && !this.elementRef.nativeElement.contains(event.target)) {
      this.menuOpen.set(false);
    }
  }

  cerrarSesion(): void {
    this.menuOpen.set(false);
    this.authService.logout();
    this.router.navigate(['/login']);
  }
}
