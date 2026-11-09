import { Component, HostListener, OnInit, inject, signal } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { mensajeDeError } from '../../../core/http/api-error';
import { PublicacionService } from '../../../core/services/publicacion.service';
import { EstadoPublicacion, PublicacionAdopcion } from '../../../core/models/publicacion.model';

@Component({
  selector: 'app-publicacion-detalle',
  standalone: true,
  imports: [RouterLink],
  template: `
    <div class="page-container">
      
      <div class="header">
        <a routerLink="/publicaciones" class="back-link">← Volver a Publicaciones</a>
      </div>

      @if (publicacion() && errorMessage()) {
        <div class="alert-banner alert-banner-error" role="alert" aria-live="assertive">
          <div class="alert-banner-content">
            <span class="alert-banner-icon" aria-hidden="true">⚠️</span>
            <span class="alert-banner-text">{{ errorMessage() }}</span>
          </div>
          <button 
            type="button" 
            class="btn-alert-dismiss" 
            (click)="errorMessage.set(null)" 
            aria-label="Cerrar notificación de error">
            ✕
          </button>
        </div>
      }

      @if (loading()) {
        <div class="not-found"><p>Cargando publicación…</p></div>
      } @else if (!publicacion()) {
        <div class="not-found">
          <p>{{ errorMessage() || 'Publicación no encontrada.' }}</p>
          <a routerLink="/publicaciones" class="btn-link">Volver al listado</a>
        </div>
      } @else {
        <div class="card">
          <div class="grid">
            
            <div class="media">
              <img [src]="publicacion()?.foto" [alt]="publicacion()?.mascota?.nombre" class="img" [class.is-grayscale]="publicacion()?.estado_publicacion === 'Finalizada'" />
              <div class="status-box">
                <span class="status-badge" [class]="'status-' + publicacion()?.estado_publicacion?.toLowerCase()">
                  {{ publicacion()?.estado_publicacion }}
                </span>
                
                <div class="action-buttons">
                  @if (publicacion()?.estado_publicacion === 'Activa') {
                    <button (click)="cambiarEstado('Pausada')" class="btn-state btn-pause">
                      Pausar
                    </button>
                    <button (click)="cambiarEstado('Finalizada')" class="btn-state btn-finalize">
                      Finalizar
                    </button>
                  }
                  @if (publicacion()?.estado_publicacion === 'Pausada') {
                    <button (click)="cambiarEstado('Activa')" class="btn-state btn-activate">
                      Activar
                    </button>
                    <button (click)="cambiarEstado('Finalizada')" class="btn-state btn-finalize">
                      Finalizar
                    </button>
                  }
                  @if (publicacion()?.estado_publicacion === 'Finalizada') {
                    <span class="finalized-text">Publicación finalizada</span>
                  }
                </div>
              </div>
            </div>

            <div class="info">
              <h1 class="title">{{ publicacion()?.mascota?.nombre }}</h1>
              <p class="subtitle">{{ publicacion()?.mascota?.especie }} · {{ publicacion()?.mascota?.sexo }}</p>

              <div class="meta-grid">
                <div class="meta-item">
                  <span class="label">Raza</span>
                  <span class="val">{{ publicacion()?.mascota?.raza || 'Mestizo' }}</span>
                </div>
                <div class="meta-item">
                  <span class="label">Año de Nacimiento</span>
                  <span class="val">{{ publicacion()?.mascota?.anio_nacimiento }}</span>
                </div>
                <div class="meta-item">
                  <span class="label">Microchip</span>
                  <span class="val">{{ publicacion()?.mascota?.microchip || 'No posee' }}</span>
                </div>
                <div class="meta-item">
                  <span class="label">Fecha de Publicación</span>
                  <span class="val">{{ publicacion()?.fecha_publicacion }}</span>
                </div>
              </div>

              <div class="section">
                <span class="label">Descripción</span>
                <p class="text">{{ publicacion()?.caracteristicas_mascota }}</p>
              </div>

              <div class="section">
                <span class="label">Condiciones de Adopción</span>
                <p class="text">{{ publicacion()?.condicion_adopcion }}</p>
              </div>
            </div>

          </div>
        </div>
      }

      <!-- Modal de Confirmación para Finalizar Publicación -->
      @if (showConfirmModal()) {
        <div class="modal-backdrop" (click)="cancelarConfirmacion()">
          <div 
            class="modal-box" 
            (click)="$event.stopPropagation()" 
            role="dialog" 
            aria-modal="true" 
            aria-labelledby="modalConfirmTitle"
            aria-describedby="modalConfirmDesc">
            
            <h3 class="modal-title" id="modalConfirmTitle">¿Finalizar publicación?</h3>
            <p class="modal-text" id="modalConfirmDesc">
              La mascota se marcará como <strong>Finalizada</strong> y su fotografía pasará a escala de grises. Ya no estará disponible para adopción y la acción es irreversible.
            </p>
            
            <div class="modal-actions">
              <button type="button" (click)="cancelarConfirmacion()" class="btn-modal-cancel">
                Cancelar
              </button>
              <button type="button" (click)="confirmarCambioEstado()" class="btn-modal-confirm">
                Sí, Finalizar
              </button>
            </div>
          </div>
        </div>
      }

    </div>
  `,
  styleUrl: './publicacion-detalle.component.css'
})
export class PublicacionDetalleComponent implements OnInit {
  private route = inject(ActivatedRoute);
  private pubService = inject(PublicacionService);

  nroPublicacion = Number(this.route.snapshot.paramMap.get('id'));
  publicacion = signal<PublicacionAdopcion | null>(null);
  loading = signal(true);
  errorMessage = signal<string | null>(null);
  showConfirmModal = signal<boolean>(false);
  pendingStateChange = signal<EstadoPublicacion | null>(null);

  @HostListener('document:keydown.escape')
  onEscapeKey(): void {
    if (this.showConfirmModal()) {
      this.cancelarConfirmacion();
    }
  }

  ngOnInit(): void {
    this.pubService.obtenerPublicacion(this.nroPublicacion).subscribe({
      next: pub => {
        this.publicacion.set(pub);
        this.loading.set(false);
      },
      error: err => {
        this.errorMessage.set(mensajeDeError(err, 'Publicación no encontrada.'));
        this.loading.set(false);
      }
    });
  }

  cambiarEstado(nuevoEstado: EstadoPublicacion): void {
    if (nuevoEstado === 'Finalizada') {
      this.pendingStateChange.set(nuevoEstado);
      this.showConfirmModal.set(true);
      return;
    }
    this.ejecutarCambioEstado(nuevoEstado);
  }

  cancelarConfirmacion(): void {
    this.showConfirmModal.set(false);
    this.pendingStateChange.set(null);
  }

  confirmarCambioEstado(): void {
    const estado = this.pendingStateChange();
    this.showConfirmModal.set(false);
    this.pendingStateChange.set(null);
    if (estado) {
      this.ejecutarCambioEstado(estado);
    }
  }

  private ejecutarCambioEstado(nuevoEstado: EstadoPublicacion): void {
    const pub = this.publicacion();
    if (!pub) return;
    this.errorMessage.set(null);
    this.pubService.cambiarEstado(pub.nro_publicacion, nuevoEstado).subscribe({
      next: actualizada => this.publicacion.set(actualizada),
      error: err => this.errorMessage.set(mensajeDeError(err, 'No se pudo cambiar el estado de la publicación.'))
    });
  }
}
