import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { AuthService } from '../../../core/services/auth.service';
import { mensajeDeError } from '../../../core/http/api-error';
import { PublicacionService } from '../../../core/services/publicacion.service';
import { EstadoPublicacion, PublicacionAdopcion } from '../../../core/models/publicacion.model';

@Component({
  selector: 'app-publicaciones-list',
  standalone: true,
  imports: [RouterLink],
  template: `
    <div class="page-wrapper">
      
      <!-- Contenedor Ventana: Ocupa 95% horizontal y 90% vertical según especificación -->
      <div class="canvas-card" role="region" aria-label="Ventana de Publicaciones de Adopción">
        
        <!-- Título Principal -->
        <h1 class="page-title">Publicaciones de Adopción</h1>

        <!-- Fila de Filtros y Búsqueda -->
        <div class="filters-row">
          
          <!-- Pestañas de Filtro por Estado (Píldoras) -->
          <div class="filter-tabs" role="tablist">
            <button 
              type="button" 
              role="tab"
              [attr.aria-selected]="filtroEstado() === 'TODAS'"
              (click)="filtroEstado.set('TODAS')" 
              [class.active]="filtroEstado() === 'TODAS'" 
              class="tab-pill">
              Todas ({{ totalCount() }})
            </button>
            <button 
              type="button" 
              role="tab"
              [attr.aria-selected]="filtroEstado() === 'Activa'"
              (click)="filtroEstado.set('Activa')" 
              [class.active]="filtroEstado() === 'Activa'" 
              class="tab-pill">
              Activas ({{ activasCount() }})
            </button>
            <button 
              type="button" 
              role="tab"
              [attr.aria-selected]="filtroEstado() === 'Pausada'"
              (click)="filtroEstado.set('Pausada')" 
              [class.active]="filtroEstado() === 'Pausada'" 
              class="tab-pill">
              Pausadas ({{ pausadasCount() }})
            </button>
            <button 
              type="button" 
              role="tab"
              [attr.aria-selected]="filtroEstado() === 'Finalizada'"
              (click)="filtroEstado.set('Finalizada')" 
              [class.active]="filtroEstado() === 'Finalizada'" 
              class="tab-pill">
              Finalizadas ({{ finalizadasCount() }})
            </button>
          </div>

          <!-- Búsqueda y Botón de Publicar a la derecha -->
          <div class="search-and-action">
            <div class="search-box">
              <input 
                type="text" 
                placeholder="Buscar mascota..." 
                (input)="onSearchInput($event)"
                class="search-input"
                id="searchMascotaInput"
              />
            </div>

            <!-- Botón + Publicar Mascota -->
            <a routerLink="/publicaciones/nueva" class="btn-create-post" id="btnNuevaPublicacion">
              + Publicar Mascota
            </a>
          </div>

        </div>

        <!-- Grilla de Mascotas -->
        @if (errorMessage()) {
          <div class="empty-state" role="alert">
            <p>{{ errorMessage() }}</p>
            <button type="button" class="btn-modal-cancel" (click)="cargar()">Reintentar</button>
          </div>
        }

        @if (loading()) {
          <div class="empty-state"><p>Cargando publicaciones…</p></div>
        } @else if (publicacionesFiltradas().length === 0) {
          <div class="empty-state">
            <p>No se encontraron publicaciones que coincidan con la búsqueda o filtro seleccionado.</p>
          </div>
        } @else {
          <div class="pets-grid">
            @for (pub of publicacionesFiltradas(); track pub.nro_publicacion) {
              <article class="pet-card">
                
                <!-- Imagen con badge de estado flotante -->
                <div class="card-image-wrapper">
                  <a [routerLink]="['/publicaciones', pub.nro_publicacion]" class="image-link" [title]="'Ver detalle de ' + (pub.mascota?.nombre || 'mascota')">
                    <img 
                      [src]="pub.foto" 
                      [alt]="'Foto de ' + (pub.mascota?.nombre || 'mascota')" 
                      class="pet-image" 
                      [class.is-grayscale]="pub.estado_publicacion === 'Finalizada'"
                    />
                  </a>
                  
                  <!-- Badge con Dot Indicador de color -->
                  <div class="floating-badge">
                    <span class="status-dot" [class]="'dot-' + pub.estado_publicacion.toLowerCase()"></span>
                    <span class="badge-text">{{ pub.estado_publicacion }}</span>
                  </div>
                </div>

                <!-- Contenido de la Tarjeta -->
                <div class="card-body">
                  
                  <!-- Fila 1: Nombre y Especie • Sexo -->
                  <div class="header-line">
                    <h2 class="pet-name">
                      <a [routerLink]="['/publicaciones', pub.nro_publicacion]" class="pet-name-link">
                        {{ pub.mascota?.nombre }}
                      </a>
                    </h2>
                    <span class="pet-species-sex">{{ pub.mascota?.especie }} · {{ pub.mascota?.sexo }}</span>
                  </div>

                  <!-- Fila 2: Raza y Edad Aprox -->
                  <div class="meta-line">
                    <span class="pet-breed">{{ pub.mascota?.raza || 'Mestizo' }}</span>
                    <span class="pet-age">{{ calcularEdad(pub.mascota?.anio_nacimiento) }}</span>
                  </div>

                  <!-- Sección Descripción -->
                  <div class="info-section">
                    <span class="section-tag">DESCRIPCIÓN</span>
                    <p class="description-text">{{ pub.caracteristicas_mascota }}</p>
                  </div>

                  <!-- Sección Condiciones de Adopción -->
                  <div class="conditions-box">
                    <span class="section-tag">CONDICIONES DE ADOPCIÓN</span>
                    <p class="conditions-text">{{ pub.condicion_adopcion }}</p>
                  </div>

                </div>

                <!-- Pie de Tarjeta / Botones de Transición de Estados -->
                <footer class="card-footer">
                  @if (pub.estado_publicacion === 'Activa') {
                    <button 
                      type="button" 
                      (click)="cambiarEstado(pub.nro_publicacion, 'Pausada')" 
                      class="btn-action btn-pause">
                      Pausar
                    </button>
                    <button 
                      type="button" 
                      (click)="solicitarFinalizar(pub.nro_publicacion)" 
                      class="btn-action btn-finalize">
                      Finalizar
                    </button>
                  }

                  @if (pub.estado_publicacion === 'Pausada') {
                    <button 
                      type="button" 
                      (click)="cambiarEstado(pub.nro_publicacion, 'Activa')" 
                      class="btn-action btn-activate">
                      Activar
                    </button>
                    <button 
                      type="button" 
                      (click)="solicitarFinalizar(pub.nro_publicacion)" 
                      class="btn-action btn-finalize">
                      Finalizar
                    </button>
                  }

                  @if (pub.estado_publicacion === 'Finalizada') {
                    <div class="finalized-text">
                      Publicación finalizada
                    </div>
                  }
                </footer>

              </article>
            }
          </div>
        }

      </div>

      <!-- Modal de Confirmación para Finalizar Publicación -->
      @if (publicacionAFinalizar() !== null) {
        <div class="modal-backdrop" (click)="cancelarFinalizar()">
          <div class="modal-box" (click)="$event.stopPropagation()" role="dialog" aria-modal="true">
            <h3 class="modal-title">¿Finalizar publicación?</h3>
            <p class="modal-text">
              La mascota se marcará como <strong>Finalizada</strong> y su fotografía pasará a escala de grises. Ya no estará disponible para nuevos adoptantes.
            </p>
            <div class="modal-actions">
              <button type="button" (click)="cancelarFinalizar()" class="btn-modal-cancel">
                Cancelar
              </button>
              <button type="button" (click)="ejecutarFinalizar()" class="btn-modal-confirm">
                Sí, Finalizar
              </button>
            </div>
          </div>
        </div>
      }

    </div>
  `,
  styleUrl: './publicaciones-list.component.css'
})
export class PublicacionesListComponent implements OnInit {
  private authService = inject(AuthService);
  private pubService = inject(PublicacionService);

  refugio = computed(() => this.authService.currentRefugio());
  filtroEstado = signal<string>('TODAS');
  busqueda = signal<string>('');
  publicacionAFinalizar = signal<number | null>(null);

  publicaciones = signal<PublicacionAdopcion[]>([]);
  loading = signal(true);
  errorMessage = signal<string | null>(null);

  ngOnInit(): void {
    this.cargar();
  }

  cargar(): void {
    this.loading.set(true);
    this.errorMessage.set(null);
    this.pubService.listar().subscribe({
      next: (data) => {
        this.publicaciones.set(data);
        this.loading.set(false);
      },
      error: (err) => {
        this.errorMessage.set(mensajeDeError(err, 'No se pudieron cargar las publicaciones.'));
        this.loading.set(false);
      }
    });
  }

  totalCount = computed(() => this.publicaciones().length);
  activasCount = computed(() => this.publicaciones().filter(p => p.estado_publicacion === 'Activa').length);
  pausadasCount = computed(() => this.publicaciones().filter(p => p.estado_publicacion === 'Pausada').length);
  finalizadasCount = computed(() => this.publicaciones().filter(p => p.estado_publicacion === 'Finalizada').length);

  publicacionesFiltradas = computed(() => {
    let list = this.publicaciones();
    const filtro = this.filtroEstado();
    if (filtro !== 'TODAS') {
      list = list.filter(p => p.estado_publicacion === filtro);
    }
    const q = this.busqueda().toLowerCase().trim();
    if (q) {
      list = list.filter(p => 
        p.mascota?.nombre?.toLowerCase().includes(q) || 
        p.mascota?.raza?.toLowerCase().includes(q) ||
        p.mascota?.especie?.toLowerCase().includes(q)
      );
    }
    return list;
  });

  onSearchInput(event: Event): void {
    const input = event.target as HTMLInputElement;
    this.busqueda.set(input.value);
  }

  calcularEdad(año?: number): string {
    if (!año) return '';
    const actual = new Date().getFullYear();
    const edad = actual - año;
    if (edad <= 0) return 'Cachorro';
    if (edad === 1) return '1 año aprox.';
    return `${edad} años aprox.`;
  }

  cambiarEstado(nro: number, nuevoEstado: EstadoPublicacion): void {
    this.errorMessage.set(null);
    this.pubService.cambiarEstado(nro, nuevoEstado).subscribe({
      next: (actualizada) => {
        this.publicaciones.update(lista =>
          lista.map(p => (p.nro_publicacion === nro ? actualizada : p))
        );
      },
      error: (err) => {
        this.errorMessage.set(mensajeDeError(err, 'No se pudo cambiar el estado de la publicación.'));
      }
    });
  }

  solicitarFinalizar(nro: number): void {
    this.publicacionAFinalizar.set(nro);
  }

  cancelarFinalizar(): void {
    this.publicacionAFinalizar.set(null);
  }

  ejecutarFinalizar(): void {
    const nro = this.publicacionAFinalizar();
    if (nro !== null) {
      this.cambiarEstado(nro, 'Finalizada');
      this.publicacionAFinalizar.set(null);
    }
  }
}
