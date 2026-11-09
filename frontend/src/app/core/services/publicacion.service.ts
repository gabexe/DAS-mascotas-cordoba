import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { EstadoPublicacion, NuevaPublicacionDTO, PublicacionAdopcion } from '../models/publicacion.model';

@Injectable({
  providedIn: 'root'
})
export class PublicacionService {
  private http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiUrl}/publicaciones`;

  // =========================================================================
  // Consultas (Lectura)
  // =========================================================================

  /**
   * Obtiene la lista de publicaciones de adopción.
   * La cabecera X-Refugio-Id es inyectada automáticamente por authRefugioInterceptor.
   *
   * @param estado Filtro opcional por estado ('Activa', 'Pausada', 'Finalizada'). 'TODAS' se omite.
   * @param busqueda Término de búsqueda opcional por nombre, especie o raza.
   */
  listar(estado?: string, busqueda?: string): Observable<PublicacionAdopcion[]> {
    let params = new HttpParams();
    if (estado && estado !== 'TODAS') {
      params = params.set('estado', estado);
    }
    if (busqueda && busqueda.trim()) {
      params = params.set('q', busqueda.trim());
    }
    return this.http.get<PublicacionAdopcion[]>(this.baseUrl, { params });
  }

  /**
   * Obtiene el detalle de una publicación por su número identificador.
   */
  obtenerPorId(id: number): Observable<PublicacionAdopcion> {
    return this.http.get<PublicacionAdopcion>(`${this.baseUrl}/${id}`);
  }

  // Alias para compatibilidad con código existente
  cargarPublicaciones(): Observable<PublicacionAdopcion[]> {
    return this.listar();
  }

  obtenerPublicacion(nro: number): Observable<PublicacionAdopcion> {
    return this.obtenerPorId(nro);
  }

  // =========================================================================
  // Comandos (Escritura y Transiciones)
  // =========================================================================

  /**
   * Registra una nueva publicación de adopción.
   */
  crear(dto: NuevaPublicacionDTO): Observable<PublicacionAdopcion> {
    return this.http.post<PublicacionAdopcion>(this.baseUrl, dto);
  }

  /**
   * Ejecuta una transición de estado en una publicación existente.
   */
  cambiarEstado(id: number, nuevoEstado: EstadoPublicacion): Observable<PublicacionAdopcion> {
    return this.http.patch<PublicacionAdopcion>(`${this.baseUrl}/${id}/estado`, { nuevo_estado: nuevoEstado });
  }

  // Alias para compatibilidad con código existente
  crearPublicacion(dto: NuevaPublicacionDTO): Observable<PublicacionAdopcion> {
    return this.crear(dto);
  }
}
