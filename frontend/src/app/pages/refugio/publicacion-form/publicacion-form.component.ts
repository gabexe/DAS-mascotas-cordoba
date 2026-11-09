import { Component, ElementRef, ViewChild, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { AuthService } from '../../../core/services/auth.service';
import { mensajeDeError } from '../../../core/http/api-error';
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

        <!-- Banners de Notificación Reactivos No Bloqueantes -->
        @if (errorMessage()) {
          <div class="alert-banner alert-banner-error" role="alert" aria-live="assertive">
            <div class="alert-banner-content">
              <span class="alert-banner-icon" aria-hidden="true">⚠️</span>
              <span class="alert-banner-text">{{ errorMessage() }}</span>
            </div>
            <button 
              type="button" 
              class="btn-alert-dismiss" 
              (click)="dismissError()" 
              aria-label="Cerrar notificación de error">
              ✕
            </button>
          </div>
        }

        @if (successMessage()) {
          <div class="alert-banner alert-banner-success" role="status" aria-live="polite">
            <div class="alert-banner-content">
              <span class="alert-banner-icon" aria-hidden="true">✓</span>
              <span class="alert-banner-text">{{ successMessage() }}</span>
            </div>
            <button 
              type="button" 
              class="btn-alert-dismiss" 
              (click)="dismissSuccess()" 
              aria-label="Cerrar notificación de éxito">
              ✕
            </button>
          </div>
        }

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
  styleUrl: './publicacion-form.component.css'
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
  errorMessage = signal<string | null>(null);
  successMessage = signal<string | null>(null);

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

  dismissError(): void {
    this.errorMessage.set(null);
  }

  dismissSuccess(): void {
    this.successMessage.set(null);
  }

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
      this.errorMessage.set('Por favor seleccione un archivo de imagen válido (PNG, JPG, WebP).');
      return;
    }
    this.errorMessage.set(null);
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
    this.errorMessage.set(null);
    this.successMessage.set(null);

    if (!this.formData.nombre.trim()) {
      this.errorMessage.set('Por favor ingrese el nombre de la mascota.');
      return;
    }

    if (!this.formData.foto.trim()) {
      this.errorMessage.set('Por favor agregue una fotografía de la mascota.');
      return;
    }

    if (!this.formData.caracteristicas_mascota.trim()) {
      this.errorMessage.set('Por favor complete la descripción y características de la mascota.');
      return;
    }

    if (!this.formData.condicion_adopcion.trim()) {
      this.errorMessage.set('Por favor complete las condiciones de adopción requeridas.');
      return;
    }

    const refugio = this.authService.currentRefugio();
    if (!refugio) {
      this.errorMessage.set('Debe iniciar sesión para registrar una publicación. Redirigiendo a inicio de sesión...');
      setTimeout(() => this.router.navigate(['/login']), 1800);
      return;
    }

    this.saving.set(true);
    this.pubService.crearPublicacion({ ...this.formData, anio_nacimiento: Number(this.formData.anio_nacimiento) }).subscribe({
      next: () => {
        this.saving.set(false);
        this.successMessage.set('¡Publicación registrada exitosamente! Redirigiendo al listado...');
        setTimeout(() => {
          this.router.navigate(['/publicaciones']);
        }, 1200);
      },
      error: err => {
        this.saving.set(false);
        this.errorMessage.set(mensajeDeError(err, 'No se pudo registrar la publicación.'));
      }
    });
  }
}
