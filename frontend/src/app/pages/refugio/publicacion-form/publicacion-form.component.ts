import { Component, ElementRef, ViewChild, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { AuthService } from '../../../core/services/auth.service';
import { PublicacionService } from '../../../core/services/publicacion.service';
import { NuevaPublicacionDTO } from '../../../core/models/publicacion.model';

@Component({
  selector: 'app-publicacion-form',
  standalone: true,
  imports: [FormsModule, RouterLink],
  template: `
    <div class="page-wrapper">
      
      <!-- Contenedor Ventana: Ocupa 95% horizontal y 90% vertical según especificación -->
      <div class="canvas-card" role="region" aria-label="Ventana de Alta de Publicación">
        
        <!-- Enlace volver y Título de Pantalla -->
        <header class="form-header">
          <a routerLink="/publicaciones" class="back-link">
            ← Volver a Publicaciones
          </a>
          <h1 class="page-title">Nueva Mascota para Adopción</h1>
        </header>

        <form (ngSubmit)="guardarPublicacion()" class="pet-creation-layout">
          
          <!-- Columna Izquierda: Datos del Formulario -->
          <div class="form-left-col">
            
            <!-- Bloque 1: Datos de la Mascota -->
            <section class="form-section">
              <h2 class="section-title">Datos de la Mascota</h2>

              <div class="fields-grid-2">
                
                <div class="form-field">
                  <label for="nombre" class="field-label">Nombre</label>
                  <input 
                    type="text" 
                    id="nombre" 
                    name="nombre" 
                    [(ngModel)]="formData.nombre" 
                    placeholder="Nombre de la mascota" 
                    required 
                    class="field-input"
                  />
                </div>

                <div class="form-field">
                  <label for="especie" class="field-label">Especie</label>
                  <select 
                    id="especie" 
                    name="especie" 
                    [(ngModel)]="formData.especie" 
                    required 
                    class="field-input select-input">
                    <option value="Perro">Perro</option>
                    <option value="Gato">Gato</option>
                    <option value="Otro">Otro</option>
                  </select>
                </div>

                <div class="form-field">
                  <label for="sexo" class="field-label">Sexo</label>
                  <select 
                    id="sexo" 
                    name="sexo" 
                    [(ngModel)]="formData.sexo" 
                    required 
                    class="field-input select-input">
                    <option value="Macho">Macho</option>
                    <option value="Hembra">Hembra</option>
                  </select>
                </div>

                <div class="form-field">
                  <label for="anio" class="field-label">Año de Nacimiento (Aprox.)</label>
                  <input 
                    type="number" 
                    id="anio" 
                    name="anio_nacimiento" 
                    [(ngModel)]="formData.anio_nacimiento" 
                    min="2005" 
                    max="2026" 
                    required 
                    class="field-input"
                  />
                </div>

                <div class="form-field">
                  <label for="raza" class="field-label">Raza</label>
                  <input 
                    type="text" 
                    id="raza" 
                    name="raza" 
                    [(ngModel)]="formData.raza" 
                    placeholder="Raza o mestizo" 
                    class="field-input"
                  />
                </div>

                <div class="form-field">
                  <label for="microchip" class="field-label">Microchip (Opcional)</label>
                  <input 
                    type="text" 
                    id="microchip" 
                    name="microchip" 
                    [(ngModel)]="formData.microchip" 
                    placeholder="Número si posee" 
                    class="field-input"
                  />
                </div>

              </div>
            </section>

            <!-- Bloque 2: Detalles de la Publicación -->
            <section class="form-section">
              <h2 class="section-title">Detalles de la Publicación</h2>

              <div class="form-field">
                <label for="caracteristicas" class="field-label">Descripción y Características</label>
                <textarea 
                  id="caracteristicas" 
                  name="caracteristicas_mascota" 
                  [(ngModel)]="formData.caracteristicas_mascota" 
                  rows="3" 
                  required 
                  placeholder="Comportamiento, energía, sociabilidad con otros animales o niños..." 
                  class="field-input textarea-input"></textarea>
              </div>

              <div class="form-field">
                <label for="condiciones" class="field-label">Condiciones de Adopción</label>
                <textarea 
                  id="condiciones" 
                  name="condicion_adopcion" 
                  [(ngModel)]="formData.condicion_adopcion" 
                  rows="3" 
                  required 
                  placeholder="Requisitos para el adoptante (patio cerrado, seguimiento, castración)..." 
                  class="field-input textarea-input"></textarea>
              </div>
            </section>

          </div>

          <!-- Columna Derecha: Ilustración de Snoopy / Carga de Foto y Botones -->
          <div class="form-right-col">
            
            <!-- Caja de carga de foto con Snoopy en 25% de opacidad según requerimiento -->
            <div 
              class="photo-dropzone-card"
              [class.is-dragover]="isDragging()"
              (dragover)="onDragOver($event)"
              (dragleave)="onDragLeave($event)"
              (drop)="onDrop($event)"
              (click)="triggerFileInput()"
              id="photoDropzone">
              
              <!-- Input de archivo oculto -->
              <input 
                type="file" 
                #fileInput 
                (change)="onFileSelected($event)" 
                accept="image/*" 
                class="hidden-file-input" 
              />

              @if (fotoPreview()) {
                <!-- Previsualización de la foto seleccionada (100% visible) -->
                <div class="photo-preview-wrapper" (click)="$event.stopPropagation()">
                  <img [src]="fotoPreview()" alt="Previsualización de la mascota" class="photo-preview-img" />
                  <div class="photo-preview-actions">
                    <button type="button" (click)="triggerFileInput()" class="btn-change-photo">
                      Cambiar foto
                    </button>
                    <button type="button" (click)="quitarFoto($event)" class="btn-remove-photo">
                      Quitar
                    </button>
                  </div>
                </div>
              } @else {
                <!-- Estado inicial fiel a prototipo 3: Texto e Ilustración al 25% de opacidad -->
                <div class="photo-empty-content">
                  <h3 class="photo-prompt-title">Agrega una foto aqui</h3>
                  
                  <div class="snoopy-illustration-container">
                    <img 
                      src="assets/ilustracion-foto.png" 
                      alt="Ilustración Snoopy selfie para agregar foto" 
                      class="snoopy-photo-img" 
                    />
                  </div>

                  <p class="photo-drag-hint">
                    Haz clic o arrastra una imagen aquí
                  </p>

                  <button 
                    type="button" 
                    (click)="toggleUrlInput($event)" 
                    class="btn-toggle-url">
                    {{ showUrlInput() ? 'Cerrar ingreso por URL' : 'O ingresar URL web' }}
                  </button>

                  @if (showUrlInput()) {
                    <div class="url-input-box" (click)="$event.stopPropagation()">
                      <input 
                        type="url" 
                        [(ngModel)]="formData.foto" 
                        (input)="onUrlInput()"
                        name="fotoUrl" 
                        placeholder="https://ejemplo.com/foto.jpg" 
                        class="field-input url-field"
                      />
                    </div>
                  }
                </div>
              }

            </div>

            <!-- Botones de Acción: Cancelar y Publicar Mascota -->
            <div class="actions-row">
              <a routerLink="/publicaciones" class="btn-cancel" id="btnCancelar">
                Cancelar
              </a>
              <button 
                type="submit" 
                [disabled]="saving()" 
                class="btn-publish" 
                id="btnPublicarMascota">
                @if (saving()) {
                  <span>Publicando...</span>
                } @else {
                  <span>Publicar Mascota</span>
                }
              </button>
            </div>

          </div>

        </form>

      </div>
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

    /* Contenedor tipo Ventana: Idéntico al de publicaciones (95% x 90% scrollable) */
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

    .form-header {
      margin-bottom: 2rem;
    }

    .back-link {
      display: inline-block;
      font-family: var(--font-heading);
      font-size: 0.95rem;
      font-weight: 700;
      color: var(--color-accent);
      text-decoration: none;
      margin-bottom: 0.6rem;
      transition: color 0.15s ease, transform 0.1s ease;
    }

    .back-link:hover {
      color: var(--color-accent-hover);
      transform: translateX(-2px);
    }

    .page-title {
      margin: 0;
      font-family: var(--font-heading);
      font-size: 2.1rem;
      font-weight: 700;
      color: var(--color-text-main);
      letter-spacing: -0.3px;
    }

    /* Layout en 2 Columnas */
    .pet-creation-layout {
      display: grid;
      grid-template-columns: minmax(0, 1.15fr) minmax(0, 0.95fr);
      gap: 3.5rem;
      align-items: start;
    }

    /* Columna Izquierda */
    .form-left-col {
      display: flex;
      flex-direction: column;
      gap: 1.8rem;
    }

    .form-section {
      display: flex;
      flex-direction: column;
      gap: 1rem;
    }

    .section-title {
      margin: 0;
      font-family: var(--font-heading);
      font-size: 1.1rem;
      font-weight: 700;
      color: var(--color-text-main);
    }

    .fields-grid-2 {
      display: grid;
      grid-template-columns: 1fr 1fr;
      gap: 1.1rem;
    }

    .form-field {
      display: flex;
      flex-direction: column;
      gap: 0.35rem;
    }

    .field-label {
      font-family: var(--font-body);
      font-size: 0.9rem;
      font-weight: 600;
      color: var(--color-text-main);
    }

    .field-input {
      padding: 0.65rem 0.95rem;
      background: var(--color-bg-surface);
      border: 1px solid var(--color-border-strong);
      border-radius: var(--radius-md);
      font-family: var(--font-body);
      font-size: 0.95rem;
      color: var(--color-text-main);
      outline: none;
      transition: border-color 0.15s ease, box-shadow 0.15s ease;
    }

    .field-input::placeholder {
      color: var(--color-text-subtle);
    }

    .field-input:focus {
      border-color: var(--color-accent);
      box-shadow: 0 0 0 3px var(--color-accent-soft);
    }

    .select-input {
      cursor: pointer;
      appearance: auto;
    }

    .textarea-input {
      resize: vertical;
      min-height: 85px;
      line-height: 1.45;
    }

    /* Columna Derecha */
    .form-right-col {
      display: flex;
      flex-direction: column;
      gap: 1.6rem;
    }

    /* Tarjeta de Carga de Foto con Snoopy */
    .photo-dropzone-card {
      background-color: var(--color-bg-muted); /* Arena Suave */
      border: 2px dashed var(--color-border-strong);
      border-radius: 26px;
      padding: 2.2rem 1.8rem;
      min-height: 440px;
      display: flex;
      flex-direction: column;
      align-items: center;
      justify-content: center;
      text-align: center;
      cursor: pointer;
      transition: all 0.2s ease;
      position: relative;
      user-select: none;
    }

    .photo-dropzone-card:hover, .photo-dropzone-card.is-dragover {
      border-color: var(--color-accent);
      background-color: var(--color-accent-soft);
    }

    .hidden-file-input {
      display: none;
    }

    .photo-empty-content {
      display: flex;
      flex-direction: column;
      align-items: center;
      gap: 0.8rem;
      width: 100%;
    }

    /* Título e Ilustración al 25% de opacidad según requerimiento */
    .photo-prompt-title {
      margin: 0;
      font-family: var(--font-heading);
      font-size: 1.65rem;
      font-weight: 700;
      color: var(--color-text-main);
      opacity: 0.25; /* Opacidad al 25% */
      letter-spacing: -0.2px;
    }

    .snoopy-illustration-container {
      width: 100%;
      max-width: 320px;
      margin: 0.4rem 0;
      display: flex;
      justify-content: center;
    }

    .snoopy-photo-img {
      width: 100%;
      height: auto;
      object-fit: contain;
      opacity: 0.25; /* Opacidad al 25% */
      filter: none; /* CERO glow */
    }

    .photo-drag-hint {
      margin: 0;
      font-family: var(--font-body);
      font-size: 0.82rem;
      color: var(--color-text-subtle);
      opacity: 0.45;
    }

    .btn-toggle-url {
      background: transparent;
      border: none;
      color: var(--color-accent);
      font-family: var(--font-heading);
      font-size: 0.82rem;
      font-weight: 700;
      cursor: pointer;
      text-decoration: underline;
      padding: 0.2rem;
    }

    .url-input-box {
      width: 100%;
      max-width: 320px;
      margin-top: 0.4rem;
    }

    .url-field {
      font-size: 0.85rem;
      padding: 0.45rem 0.75rem;
    }

    /* Previsualización de Foto */
    .photo-preview-wrapper {
      width: 100%;
      height: 100%;
      display: flex;
      flex-direction: column;
      align-items: center;
      gap: 1rem;
    }

    .photo-preview-img {
      width: 100%;
      max-height: 360px;
      object-fit: cover;
      border-radius: 18px;
      box-shadow: 0 4px 14px rgba(38, 36, 34, 0.08);
      border: 1px solid var(--color-border-subtle);
    }

    .photo-preview-actions {
      display: flex;
      gap: 0.6rem;
    }

    .btn-change-photo, .btn-remove-photo {
      font-family: var(--font-heading);
      font-size: 0.82rem;
      font-weight: 700;
      padding: 0.35rem 0.9rem;
      border-radius: var(--radius-pill);
      cursor: pointer;
      transition: all 0.15s ease;
    }

    .btn-change-photo {
      background: var(--color-accent);
      color: var(--color-text-inverse);
      border: none;
    }

    .btn-remove-photo {
      background: var(--color-bg-surface);
      border: 1px solid var(--color-border-strong);
      color: #DC2626;
    }

    .btn-remove-photo:hover {
      background: #FEE2E2;
    }

    /* Fila de Botones Inferiores */
    .actions-row {
      display: flex;
      justify-content: flex-end;
      align-items: center;
      gap: 0.9rem;
      margin-top: 0.5rem;
    }

    .btn-cancel {
      text-decoration: none;
      color: var(--color-text-main);
      background: var(--color-bg-surface);
      font-family: var(--font-heading);
      font-size: 0.92rem;
      font-weight: 700;
      padding: 0.65rem 1.6rem;
      border-radius: var(--radius-pill);
      border: 1px solid var(--color-border-strong);
      transition: all 0.15s ease;
      cursor: pointer;
      display: inline-flex;
      align-items: center;
      justify-content: center;
    }

    .btn-cancel:hover {
      background: var(--color-bg-muted);
    }

    .btn-publish {
      background: var(--color-accent);
      color: var(--color-text-inverse); /* Crema Lienzo */
      border: none;
      font-family: var(--font-heading);
      font-size: 0.92rem;
      font-weight: 700;
      padding: 0.65rem 1.8rem;
      border-radius: var(--radius-pill);
      cursor: pointer;
      display: inline-flex;
      align-items: center;
      justify-content: center;
      box-shadow: none; /* CERO glow */
      transition: background-color 0.15s ease, transform 0.1s ease;
    }

    .btn-publish:hover:not(:disabled) {
      background: var(--color-accent-hover);
      transform: translateY(-1px);
    }

    .btn-publish:disabled {
      background: var(--color-text-subtle);
      cursor: not-allowed;
      box-shadow: none;
    }

    @media (max-width: 960px) {
      .pet-creation-layout {
        grid-template-columns: 1fr;
        gap: 2.2rem;
      }
      .canvas-card {
        padding: 1.8rem 1.4rem;
      }
      .photo-dropzone-card {
        min-height: 320px;
      }
    }

    @media (max-width: 600px) {
      .canvas-card {
        padding: 1.5rem 1rem;
        border-radius: 20px;
      }
      .fields-grid-2 {
        grid-template-columns: 1fr;
      }
    }
  `]
})
export class PublicacionFormComponent {
  private authService = inject(AuthService);
  private pubService = inject(PublicacionService);
  private router = inject(Router);

  @ViewChild('fileInput') fileInputRef!: ElementRef<HTMLInputElement>;

  saving = signal(false);
  isDragging = signal(false);
  showUrlInput = signal(false);
  fotoPreview = signal<string | null>(null);

  formData: NuevaPublicacionDTO = {
    nombre: '',
    especie: 'Perro',
    sexo: 'Macho',
    anio_nacimiento: 2024,
    raza: '',
    microchip: '',
    foto: '',
    caracteristicas_mascota: '',
    condicion_adopcion: ''
  };

  triggerFileInput(): void {
    if (this.fileInputRef?.nativeElement) {
      this.fileInputRef.nativeElement.click();
    }
  }

  onFileSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    if (input.files && input.files[0]) {
      this.procesarArchivo(input.files[0]);
    }
  }

  onDragOver(event: DragEvent): void {
    event.preventDefault();
    this.isDragging.set(true);
  }

  onDragLeave(event: DragEvent): void {
    event.preventDefault();
    this.isDragging.set(false);
  }

  onDrop(event: DragEvent): void {
    event.preventDefault();
    this.isDragging.set(false);
    if (event.dataTransfer?.files && event.dataTransfer.files[0]) {
      this.procesarArchivo(event.dataTransfer.files[0]);
    }
  }

  private procesarArchivo(file: File): void {
    if (!file.type.startsWith('image/')) {
      alert('Por favor seleccione un archivo de imagen válido (PNG, JPG, WebP).');
      return;
    }
    const reader = new FileReader();
    reader.onload = (e) => {
      const result = e.target?.result as string;
      this.fotoPreview.set(result);
      this.formData.foto = result;
    };
    reader.readAsDataURL(file);
  }

  quitarFoto(event: MouseEvent): void {
    event.stopPropagation();
    this.fotoPreview.set(null);
    this.formData.foto = '';
    if (this.fileInputRef?.nativeElement) {
      this.fileInputRef.nativeElement.value = '';
    }
  }

  toggleUrlInput(event: MouseEvent): void {
    event.stopPropagation();
    this.showUrlInput.update(v => !v);
  }

  onUrlInput(): void {
    if (this.formData.foto && this.formData.foto.startsWith('http')) {
      this.fotoPreview.set(this.formData.foto);
    } else if (!this.formData.foto) {
      this.fotoPreview.set(null);
    }
  }

  guardarPublicacion(): void {
    if (!this.formData.nombre.trim()) {
      alert('Por favor ingrese el nombre de la mascota');
      return;
    }

    if (!this.formData.caracteristicas_mascota.trim()) {
      alert('Por favor complete la descripción y características de la mascota');
      return;
    }

    if (!this.formData.condicion_adopcion.trim()) {
      alert('Por favor complete las condiciones de adopción requeridas');
      return;
    }

    const refugio = this.authService.currentRefugio();
    if (!refugio) {
      alert('Debe iniciar sesión para registrar una publicación.');
      this.router.navigate(['/login']);
      return;
    }
    const refugioId = refugio.id_refugio;

    this.saving.set(true);
    setTimeout(() => {
      this.pubService.crearPublicacion(this.formData, refugioId);
      this.saving.set(false);
      this.router.navigate(['/publicaciones']);
    }, 350);
  }
}
