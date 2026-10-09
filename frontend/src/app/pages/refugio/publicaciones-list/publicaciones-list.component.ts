import { Component, computed, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { AuthService } from '../../../core/services/auth.service';
import { PublicacionService } from '../../../core/services/publicacion.service';
import { EstadoPublicacion } from '../../../core/models/publicacion.model';

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
        @if (publicacionesFiltradas().length === 0) {
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
  styles: [`
    :host {
      display: block;
      width: 100%;
    }

    .page-wrapper {
      width: 100%;
      display: flex;
      justify-content: center;
      align-items: flex-start;
      padding: 0 0 1.5vh 0;
      box-sizing: border-box;
    }

    /* Contenedor tipo Ventana: 95% horizontal y 90% vertical dejando espacio al header */
    .canvas-card {
      width: 95%;
      height: calc(100vh - 84px);
      max-height: 89vh;
      overflow-y: auto;
      overflow-x: hidden;
      background: var(--color-bg-surface); /* Crema Lienzo #FAF7F2, sin blancos puros */
      border: 1px solid var(--color-border-subtle);
      border-radius: 28px;
      padding: 2.2rem 2.8rem;
      box-shadow: 0 4px 24px rgba(38, 36, 34, 0.04);
      scrollbar-width: thin;
      scrollbar-color: var(--color-border-strong) transparent;
      box-sizing: border-box;
    }

    .canvas-card::-webkit-scrollbar {
      width: 7px;
    }

    .canvas-card::-webkit-scrollbar-thumb {
      background-color: var(--color-border-strong);
      border-radius: var(--radius-pill);
    }

    .canvas-card::-webkit-scrollbar-track {
      background: transparent;
    }

    .page-title {
      margin: 0 0 1.6rem 0;
      font-family: var(--font-heading);
      font-size: 2.1rem;
      font-weight: 700;
      color: var(--color-text-main); /* Carbón Cálido */
      letter-spacing: -0.3px;
    }

    /* Fila de Filtros y Búsqueda */
    .filters-row {
      display: flex;
      justify-content: space-between;
      align-items: center;
      flex-wrap: wrap;
      gap: 1.2rem;
      margin-bottom: 2rem;
    }

    .filter-tabs {
      display: flex;
      align-items: center;
      gap: 0.5rem;
      flex-wrap: wrap;
    }

    .tab-pill {
      background: var(--color-bg-surface);
      border: 1px solid var(--color-border-subtle);
      color: var(--color-text-muted);
      padding: 0.45rem 1.05rem;
      border-radius: var(--radius-pill);
      font-family: var(--font-heading);
      font-size: 0.88rem;
      font-weight: 700;
      cursor: pointer;
      transition: all 0.15s ease;
    }

    .tab-pill:hover {
      background: var(--color-bg-muted);
      color: var(--color-text-main);
    }

    .tab-pill.active {
      background: var(--color-text-main); /* Carbón Cálido */
      color: var(--color-text-inverse); /* Crema Lienzo, sin blancos puros */
      border-color: var(--color-text-main);
    }

    .search-and-action {
      display: flex;
      align-items: center;
      gap: 0.8rem;
      flex-wrap: wrap;
    }

    .search-box {
      position: relative;
    }

    .search-input {
      padding: 0.5rem 1.2rem;
      border: 1px solid var(--color-border-subtle);
      border-radius: var(--radius-pill);
      font-family: var(--font-body);
      font-size: 0.9rem;
      color: var(--color-text-main);
      background: var(--color-bg-surface);
      outline: none;
      width: 220px;
      transition: border-color 0.15s ease, box-shadow 0.15s ease;
    }

    .search-input::placeholder {
      color: var(--color-text-subtle);
    }

    .search-input:focus {
      border-color: var(--color-accent);
      box-shadow: 0 0 0 3px var(--color-accent-soft);
    }

    .btn-create-post {
      background: var(--color-accent);
      color: var(--color-text-inverse); /* Crema Lienzo, sin blancos puros */
      text-decoration: none;
      padding: 0.52rem 1.3rem;
      border-radius: var(--radius-pill);
      font-family: var(--font-heading);
      font-size: 0.9rem;
      font-weight: 700;
      display: inline-flex;
      align-items: center;
      justify-content: center;
      box-shadow: none; /* CERO glow */
      transition: background-color 0.15s ease, transform 0.1s ease;
      white-space: nowrap;
    }

    .btn-create-post:hover {
      background: var(--color-accent-hover);
      transform: translateY(-1px);
    }

    /* Grilla de Publicaciones */
    .pets-grid {
      display: grid;
      grid-template-columns: repeat(4, 1fr);
      gap: 1.5rem;
    }

    @media (max-width: 1280px) {
      .pets-grid {
        grid-template-columns: repeat(3, 1fr);
      }
    }

    @media (max-width: 960px) {
      .pets-grid {
        grid-template-columns: repeat(2, 1fr);
      }
    }

    @media (max-width: 600px) {
      .pets-grid {
        grid-template-columns: 1fr;
      }
      .canvas-card {
        padding: 1.5rem 1rem;
        border-radius: 20px;
      }
    }

    /* Tarjeta de Mascota */
    .pet-card {
      background: var(--color-bg-surface);
      border: 1px solid var(--color-border-subtle);
      border-radius: 18px;
      overflow: hidden;
      display: flex;
      flex-direction: column;
      box-shadow: 0 2px 10px rgba(38, 36, 34, 0.03);
      transition: transform 0.15s ease, box-shadow 0.15s ease;
    }

    .pet-card:hover {
      transform: translateY(-3px);
      box-shadow: 0 8px 24px rgba(38, 36, 34, 0.06);
    }

    .card-image-wrapper {
      position: relative;
      height: 195px;
      background: var(--color-bg-muted);
      overflow: hidden;
    }

    .image-link {
      display: block;
      width: 100%;
      height: 100%;
      text-decoration: none;
    }

    .pet-name-link {
      color: inherit;
      text-decoration: none;
      transition: color 0.15s ease;
    }

    .pet-name-link:hover {
      color: var(--color-accent);
    }

    .pet-image {
      width: 100%;
      height: 100%;
      object-fit: cover;
      display: block;
      transition: filter 0.25s ease, opacity 0.25s ease;
    }

    /* Escala de grises oficial para publicaciones finalizadas según prototipo 2 */
    .pet-image.is-grayscale {
      filter: grayscale(100%);
      opacity: 0.9;
    }

    /* Badge flotante con indicador de punto (dot) */
    .floating-badge {
      position: absolute;
      top: 10px;
      right: 10px;
      background: var(--color-bg-surface);
      border: 1px solid var(--color-border-subtle);
      border-radius: var(--radius-pill);
      padding: 0.25rem 0.65rem;
      display: inline-flex;
      align-items: center;
      gap: 0.4rem;
      box-shadow: 0 2px 8px rgba(38, 36, 34, 0.08);
    }

    .badge-text {
      font-family: var(--font-heading);
      font-size: 0.76rem;
      font-weight: 700;
      color: var(--color-text-main);
    }

    /* Puntos de estado */
    .status-dot {
      width: 7px;
      height: 7px;
      border-radius: 50%;
      display: inline-block;
    }

    .status-dot.dot-activa {
      background-color: var(--dot-activa);
    }

    .status-dot.dot-pausada {
      background-color: var(--dot-pausada);
    }

    .status-dot.dot-finalizada {
      background-color: var(--dot-finalizada);
    }

    /* Cuerpo de la Tarjeta */
    .card-body {
      padding: 1.1rem 1.15rem;
      display: flex;
      flex-direction: column;
      gap: 0.75rem;
      flex: 1;
    }

    .header-line {
      display: flex;
      justify-content: space-between;
      align-items: baseline;
      gap: 0.4rem;
    }

    .pet-name {
      margin: 0;
      font-family: var(--font-heading);
      font-size: 1.35rem;
      font-weight: 700;
      color: var(--color-text-main);
      letter-spacing: -0.2px;
      line-height: 1.1;
    }

    .pet-species-sex {
      font-family: var(--font-body);
      font-size: 0.8rem;
      color: var(--color-text-muted);
      font-weight: 600;
      white-space: nowrap;
    }

    .meta-line {
      display: flex;
      justify-content: space-between;
      align-items: baseline;
      font-family: var(--font-body);
      font-size: 0.84rem;
      color: var(--color-text-muted);
      padding-bottom: 0.5rem;
      border-bottom: 1px solid var(--color-border-subtle);
    }

    .pet-breed {
      font-weight: 600;
    }

    .pet-age {
      color: var(--color-text-subtle);
      white-space: nowrap;
    }

    .info-section {
      display: flex;
      flex-direction: column;
      gap: 0.25rem;
    }

    .section-tag {
      font-family: var(--font-heading);
      font-size: 0.68rem;
      font-weight: 700;
      color: var(--color-text-subtle);
      letter-spacing: 0.5px;
      text-transform: uppercase;
    }

    .description-text {
      margin: 0;
      font-family: var(--font-body);
      font-size: 0.82rem;
      color: var(--color-text-main);
      line-height: 1.4;
      display: -webkit-box;
      -webkit-line-clamp: 3;
      -webkit-box-orient: vertical;
      overflow: hidden;
    }

    .conditions-box {
      background: var(--color-bg-muted); /* Arena Suave */
      border: 1px solid var(--color-border-subtle);
      border-radius: var(--radius-md);
      padding: 0.65rem 0.8rem;
      display: flex;
      flex-direction: column;
      gap: 0.2rem;
    }

    .conditions-text {
      margin: 0;
      font-family: var(--font-body);
      font-size: 0.8rem;
      color: var(--color-text-muted);
      line-height: 1.35;
      display: -webkit-box;
      -webkit-line-clamp: 3;
      -webkit-box-orient: vertical;
      overflow: hidden;
    }

    /* Pie de Tarjeta */
    .card-footer {
      padding: 0.85rem 1.15rem;
      border-top: 1px solid var(--color-border-subtle);
      display: flex;
      gap: 0.6rem;
      align-items: center;
      background: var(--color-bg-surface);
    }

    .btn-action {
      flex: 1;
      background: var(--color-bg-surface);
      border: 1px solid var(--color-border-strong);
      padding: 0.42rem;
      border-radius: var(--radius-pill);
      font-family: var(--font-heading);
      font-size: 0.82rem;
      font-weight: 700;
      color: var(--color-text-main);
      cursor: pointer;
      transition: all 0.15s ease;
      text-align: center;
    }

    .btn-pause:hover {
      background: #FEF3C7;
      border-color: #FCD34D;
      color: #92400E;
    }

    .btn-activate:hover {
      background: #DCFCE7;
      border-color: #86EFAC;
      color: #166534;
    }

    .btn-finalize:hover {
      background: #FEE2E2;
      border-color: #FCA5A5;
      color: #991B1B;
    }

    .finalized-text {
      width: 100%;
      text-align: center;
      font-family: var(--font-heading);
      font-size: 0.82rem;
      color: var(--color-text-subtle);
      font-weight: 600;
      padding: 0.2rem 0;
    }

    .empty-state {
      text-align: center;
      padding: 3rem 1.5rem;
      color: var(--color-text-muted);
      font-size: 0.95rem;
    }

    /* Modal de Confirmación */
    .modal-backdrop {
      position: fixed;
      inset: 0;
      background: rgba(38, 36, 34, 0.45);
      backdrop-filter: blur(3px);
      z-index: 2000;
      display: flex;
      align-items: center;
      justify-content: center;
      padding: 1.5rem;
      animation: fadeIn 0.15s ease-out;
    }

    .modal-box {
      background: var(--color-bg-surface); /* Crema Lienzo */
      border: 1px solid var(--color-border-subtle);
      border-radius: 24px;
      padding: 2.2rem;
      max-width: 440px;
      width: 100%;
      box-shadow: var(--shadow-modal);
      display: flex;
      flex-direction: column;
      gap: 1.2rem;
      animation: scaleIn 0.18s cubic-bezier(0.16, 1, 0.3, 1);
    }

    @keyframes fadeIn {
      from { opacity: 0; }
      to { opacity: 1; }
    }

    @keyframes scaleIn {
      from { opacity: 0; transform: scale(0.95); }
      to { opacity: 1; transform: scale(1); }
    }

    .modal-title {
      margin: 0;
      font-family: var(--font-heading);
      font-size: 1.35rem;
      font-weight: 700;
      color: var(--color-text-main);
    }

    .modal-text {
      margin: 0;
      font-family: var(--font-body);
      font-size: 0.92rem;
      color: var(--color-text-muted);
      line-height: 1.45;
    }

    .modal-actions {
      display: flex;
      justify-content: flex-end;
      gap: 0.8rem;
      margin-top: 0.4rem;
    }

    .btn-modal-cancel {
      background: var(--color-bg-muted);
      border: 1px solid var(--color-border-subtle);
      padding: 0.55rem 1.2rem;
      border-radius: var(--radius-pill);
      font-family: var(--font-heading);
      font-size: 0.88rem;
      font-weight: 700;
      color: var(--color-text-main);
      cursor: pointer;
      transition: all 0.15s ease;
    }

    .btn-modal-cancel:hover {
      background: var(--color-border-subtle);
    }

    .btn-modal-confirm {
      background: #DC2626;
      border: none;
      padding: 0.55rem 1.3rem;
      border-radius: var(--radius-pill);
      font-family: var(--font-heading);
      font-size: 0.88rem;
      font-weight: 700;
      color: var(--color-text-inverse);
      cursor: pointer;
      transition: all 0.15s ease;
    }

    .btn-modal-confirm:hover {
      background: #B91C1C;
    }
  `]
})
export class PublicacionesListComponent {
  private authService = inject(AuthService);
  private pubService = inject(PublicacionService);

  refugio = computed(() => this.authService.currentRefugio());
  filtroEstado = signal<string>('TODAS');
  busqueda = signal<string>('');
  publicacionAFinalizar = signal<number | null>(null);

  allPublicaciones = this.pubService.getPublicaciones();

  publicacionesRefugio = computed(() => {
    const currentRefugioId = this.refugio()?.id_refugio;
    if (!currentRefugioId) return [];
    return this.allPublicaciones().filter(p => p.id_refugio === currentRefugioId);
  });

  totalCount = computed(() => this.publicacionesRefugio().length);
  activasCount = computed(() => this.publicacionesRefugio().filter(p => p.estado_publicacion === 'Activa').length);
  pausadasCount = computed(() => this.publicacionesRefugio().filter(p => p.estado_publicacion === 'Pausada').length);
  finalizadasCount = computed(() => this.publicacionesRefugio().filter(p => p.estado_publicacion === 'Finalizada').length);

  publicacionesFiltradas = computed(() => {
    let list = this.publicacionesRefugio();
    const filtro = this.filtroEstado();
    if (filtro !== 'TODAS') {
      list = list.filter(p => p.estado_publicacion === filtro);
    }
    const q = this.busqueda().toLowerCase().trim();
    if (q) {
      list = list.filter(p => 
        p.mascota?.nombre.toLowerCase().includes(q) || 
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
    this.pubService.cambiarEstado(nro, nuevoEstado);
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
      this.pubService.cambiarEstado(nro, 'Finalizada');
      this.publicacionAFinalizar.set(null);
    }
  }
}
