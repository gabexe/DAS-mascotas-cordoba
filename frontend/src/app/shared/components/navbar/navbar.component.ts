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
  styleUrl: './navbar.component.css'
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
