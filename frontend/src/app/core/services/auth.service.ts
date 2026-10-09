import { HttpClient } from '@angular/common/http';
import { Injectable, computed, inject, signal } from '@angular/core';
import { firstValueFrom } from 'rxjs';
import { environment } from '../../../environments/environment';
import { mensajeDeError } from '../http/api-error';
import { UsuarioRefugio } from '../models/refugio.model';

const STORAGE_KEY = 'mascotas_cba_refugio_user';

@Injectable({
  providedIn: 'root'
})
export class AuthService {
  private http = inject(HttpClient);
  private currentUserSignal = signal<UsuarioRefugio | null>(this.getStoredUser());

  currentUser = computed(() => this.currentUserSignal());
  isAuthenticated = computed(() => this.currentUserSignal() !== null);
  currentRefugio = computed(() => this.currentUserSignal()?.refugio ?? null);

  // =========================================================================
  // RF15 – Autenticación interna (CUIL + clave) contra POST /auth/login
  // =========================================================================
  async login(cuil: string, clave: string): Promise<UsuarioRefugio> {
    try {
      const user = await firstValueFrom(
        this.http.post<UsuarioRefugio>(`${environment.apiUrl}/auth/login`, { cuil: cuil.trim(), clave })
      );
      this.currentUserSignal.set(user);
      localStorage.setItem(STORAGE_KEY, JSON.stringify(user));
      return user;
    } catch (err) {
      throw new Error(mensajeDeError(err, 'Credenciales incorrectas o usuario no habilitado'));
    }
  }

  logout(): void {
    this.currentUserSignal.set(null);
    localStorage.removeItem(STORAGE_KEY);
  }

  // =========================================================================
  // Persistencia de sesión (solo el usuario autenticado; no hay tokens)
  // =========================================================================
  private getStoredUser(): UsuarioRefugio | null {
    try {
      const stored = localStorage.getItem(STORAGE_KEY);
      return stored ? JSON.parse(stored) : null;
    } catch {
      return null;
    }
  }
}
