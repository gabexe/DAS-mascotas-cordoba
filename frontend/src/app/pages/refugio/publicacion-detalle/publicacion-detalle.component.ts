import { Component, OnInit, inject, signal } from '@angular/core';
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

    </div>
  `,
  styles: [`
    .page-container {
      max-width: 960px;
      margin: 0 auto;
      padding: 1.5rem 1.2rem;
    }
    .header {
      margin-bottom: 1.2rem;
    }
    .back-link {
      font-family: var(--font-heading);
      font-size: 0.95rem;
      font-weight: 700;
      color: var(--color-accent);
      text-decoration: none;
      transition: color 0.15s ease;
    }
    .back-link:hover {
      color: var(--color-accent-hover);
    }
    .card {
      background: var(--color-bg-surface);
      border: 1px solid var(--color-border-subtle);
      border-radius: 24px;
      overflow: hidden;
      box-shadow: 0 4px 20px rgba(38, 36, 34, 0.04);
    }
    .grid {
      display: grid;
      grid-template-columns: 360px 1fr;
    }
    .media {
      padding: 1.8rem;
      border-right: 1px solid var(--color-border-subtle);
      display: flex;
      flex-direction: column;
      gap: 1.2rem;
      background: var(--color-bg-muted);
    }
    .img {
      width: 100%;
      height: 260px;
      object-fit: cover;
      border-radius: 16px;
      border: 1px solid var(--color-border-subtle);
      transition: filter 0.25s ease, opacity 0.25s ease;
    }
    .img.is-grayscale {
      filter: grayscale(100%);
      opacity: 0.9;
    }
    .status-box {
      display: flex;
      flex-direction: column;
      gap: 0.8rem;
    }
    .status-badge {
      font-family: var(--font-heading);
      font-size: 0.8rem;
      font-weight: 700;
      padding: 0.3rem 0.8rem;
      border-radius: var(--radius-pill);
      align-self: flex-start;
    }
    .status-activa { background: #DCFCE7; color: #166534; border: 1px solid #86EFAC; }
    .status-pausada { background: #FEF3C7; color: #92400E; border: 1px solid #FCD34D; }
    .status-finalizada { background: var(--color-bg-canvas); color: var(--color-text-muted); border: 1px solid var(--color-border-subtle); }
    .action-buttons {
      display: flex;
      gap: 0.5rem;
    }
    .btn-state {
      flex: 1;
      padding: 0.5rem;
      border-radius: var(--radius-pill);
      border: 1px solid var(--color-border-strong);
      background: var(--color-bg-surface);
      font-family: var(--font-heading);
      font-size: 0.82rem;
      cursor: pointer;
      font-weight: 700;
      color: var(--color-text-main);
      transition: all 0.15s ease;
    }
    .btn-pause:hover { background: #FEF3C7; border-color: #FCD34D; }
    .btn-activate:hover { background: #DCFCE7; border-color: #86EFAC; }
    .btn-finalize:hover { background: #FEE2E2; border-color: #FCA5A5; color: #991B1B; }
    .finalized-text { font-family: var(--font-heading); font-size: 0.82rem; color: var(--color-text-subtle); font-weight: 600; text-align: center; }
    .info {
      padding: 2rem;
      display: flex;
      flex-direction: column;
      gap: 1.4rem;
    }
    .title {
      margin: 0;
      font-family: var(--font-heading);
      font-size: 1.8rem;
      font-weight: 700;
      color: var(--color-text-main);
    }
    .subtitle {
      margin: 0;
      font-family: var(--font-body);
      font-size: 0.95rem;
      color: var(--color-text-muted);
    }
    .meta-grid {
      display: grid;
      grid-template-columns: 1fr 1fr;
      gap: 1rem;
      padding: 1rem 0;
      border-top: 1px solid var(--color-border-subtle);
      border-bottom: 1px solid var(--color-border-subtle);
    }
    .meta-item {
      display: flex;
      flex-direction: column;
      gap: 0.2rem;
    }
    .label {
      font-family: var(--font-body);
      font-size: 0.75rem;
      color: var(--color-text-subtle);
      text-transform: uppercase;
      font-weight: 700;
      letter-spacing: 0.4px;
    }
    .val {
      font-family: var(--font-body);
      font-size: 0.95rem;
      color: var(--color-text-main);
      font-weight: 600;
    }
    .section {
      display: flex;
      flex-direction: column;
      gap: 0.3rem;
    }
    .text {
      margin: 0;
      font-family: var(--font-body);
      font-size: 0.92rem;
      color: var(--color-text-muted);
      line-height: 1.5;
    }
    .not-found {
      text-align: center;
      padding: 3rem;
      background: var(--color-bg-surface);
      border: 1px solid var(--color-border-subtle);
      border-radius: 16px;
      color: var(--color-text-main);
    }
    .btn-link {
      color: var(--color-accent);
      font-family: var(--font-heading);
      font-weight: 700;
      text-decoration: underline;
    }
    @media (max-width: 680px) {
      .grid { grid-template-columns: 1fr; }
      .media { border-right: none; border-bottom: 1px solid var(--color-border-subtle); }
    }
  `]
})
export class PublicacionDetalleComponent implements OnInit {
  private route = inject(ActivatedRoute);
  private pubService = inject(PublicacionService);

  nroPublicacion = Number(this.route.snapshot.paramMap.get('id'));
  publicacion = signal<PublicacionAdopcion | null>(null);
  loading = signal(true);
  errorMessage = signal<string | null>(null);

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
    const pub = this.publicacion();
    if (pub) {
      if (nuevoEstado === 'Finalizada') {
        const confirmar = confirm('¿Desea finalizar esta publicación? La mascota ya no estará disponible para adopción y la acción es irreversible.');
        if (!confirmar) return;
      }
      this.errorMessage.set(null);
      this.pubService.cambiarEstado(pub.nro_publicacion, nuevoEstado).subscribe({
        next: actualizada => this.publicacion.set(actualizada),
        error: err => this.errorMessage.set(mensajeDeError(err, 'No se pudo cambiar el estado de la publicación.'))
      });
    }
  }
}
