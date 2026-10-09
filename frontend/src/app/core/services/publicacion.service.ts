import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Injectable, inject, signal } from '@angular/core';
import { Observable, tap } from 'rxjs';
import { environment } from '../../../environments/environment';
import { EstadoPublicacion, NuevaPublicacionDTO, PublicacionAdopcion } from '../models/publicacion.model';
import { AuthService } from './auth.service';

@Injectable({
  providedIn: 'root'
})
export class PublicacionService {
  private http = inject(HttpClient);
  private authService = inject(AuthService);
  private readonly baseUrl = `${environment.apiUrl}/publicaciones`;

  private publicacionesSignal = signal<PublicacionAdopcion[]>([]);
  readonly publicaciones = this.publicacionesSignal.asReadonly();

  // =========================================================================
  // Consultas
  // =========================================================================
  cargarPublicaciones(): Observable<PublicacionAdopcion[]> {
    return this.http
      .get<PublicacionAdopcion[]>(this.baseUrl, { headers: this.headersRefugio() })
      .pipe(tap(lista => this.publicacionesSignal.set(lista)));
  }

  obtenerPublicacion(nro: number): Observable<PublicacionAdopcion> {
    return this.http.get<PublicacionAdopcion>(`${this.baseUrl}/${nro}`, { headers: this.headersRefugio() });
  }

  // =========================================================================
  // Comandos (alta y cambio de estado; sin edición según alcance del parcial)
  // =========================================================================
  crearPublicacion(dto: NuevaPublicacionDTO): Observable<PublicacionAdopcion> {
    return this.http
      .post<PublicacionAdopcion>(this.baseUrl, dto, { headers: this.headersRefugio() })
      .pipe(tap(nueva => this.publicacionesSignal.update(lista => [nueva, ...lista])));
  }

  cambiarEstado(nro: number, nuevoEstado: EstadoPublicacion): Observable<PublicacionAdopcion> {
    return this.http
      .patch<PublicacionAdopcion>(`${this.baseUrl}/${nro}/estado`, { nuevo_estado: nuevoEstado }, { headers: this.headersRefugio() })
      .pipe(tap(actualizada =>
        this.publicacionesSignal.update(lista => lista.map(p => (p.nro_publicacion === nro ? actualizada : p)))
      ));
  }

  // El refugio del usuario autenticado acota todas las operaciones (RF13)
  private headersRefugio(): HttpHeaders {
    const idRefugio = this.authService.currentRefugio()?.id_refugio;
    return new HttpHeaders(idRefugio ? { 'X-Refugio-Id': String(idRefugio) } : {});
  }
}
