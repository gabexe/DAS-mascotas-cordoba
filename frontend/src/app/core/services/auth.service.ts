import { Injectable, signal, computed } from '@angular/core';
import { UsuarioRefugio } from '../models/refugio.model';

const STORAGE_KEY = 'mascotas_cba_refugio_user';

const USUARIOS_MOCK: UsuarioRefugio[] = [
  {
    id_ciudadano: 1,
    cuil: '20-35444555-9',
    nombre: 'Martín',
    apellido: 'Gómez',
    correo: 'martin.gomez@huellitascba.org.ar',
    telefono: '351-4567890',
    id_refugio: 101,
    refugio: {
      id_refugio: 101,
      razon_social: 'Asociación Protectora Huellitas Cordobesas',
      correo: 'contacto@huellitascba.org.ar',
      telefono: '0351-4890123',
      domicilio: 'Av. Colón 4500, Córdoba Capital',
      habilitacion_municipal: 'HAB-MUNIC-CBA-2024-0412'
    }
  },
  {
    id_ciudadano: 2,
    cuil: '27-38999888-4',
    nombre: 'Camila',
    apellido: 'Fernández',
    correo: 'camila@patitasdelsur.org.ar',
    telefono: '351-7891234',
    id_refugio: 102,
    refugio: {
      id_refugio: 102,
      razon_social: 'Refugio Patitas del Sur',
      correo: 'info@patitasdelsur.org.ar',
      telefono: '0351-4998877',
      domicilio: 'Av. Valparaíso 3200, Córdoba',
      habilitacion_municipal: 'HAB-MUNIC-CBA-2023-0199'
    }
  }
];

@Injectable({
  providedIn: 'root'
})
export class AuthService {
  private currentUserSignal = signal<UsuarioRefugio | null>(this.getStoredUser());

  currentUser = computed(() => this.currentUserSignal());
  isAuthenticated = computed(() => this.currentUserSignal() !== null);
  currentRefugio = computed(() => this.currentUserSignal()?.refugio ?? null);

  private getStoredUser(): UsuarioRefugio | null {
    try {
      const stored = localStorage.getItem(STORAGE_KEY);
      return stored ? JSON.parse(stored) : null;
    } catch {
      return null;
    }
  }

  login(cuil: string, clave: string): Promise<UsuarioRefugio> {
    return new Promise((resolve, reject) => {
      setTimeout(() => {
        const cleanCuil = cuil.trim();
        const user = USUARIOS_MOCK.find(u => u.cuil === cleanCuil || u.cuil.replace(/-/g, '') === cleanCuil.replace(/-/g, ''));
        
        if (user && clave.length >= 4) {
          this.currentUserSignal.set(user);
          localStorage.setItem(STORAGE_KEY, JSON.stringify(user));
          resolve(user);
        } else {
          reject(new Error('Credenciales incorrectas o usuario no habilitado'));
        }
      }, 400);
    });
  }

  logout(): void {
    this.currentUserSignal.set(null);
    localStorage.removeItem(STORAGE_KEY);
  }

  seleccionarUsuarioMock(id: number): void {
    const user = USUARIOS_MOCK.find(u => u.id_ciudadano === id) || null;
    this.currentUserSignal.set(user);
    if (user) {
      localStorage.setItem(STORAGE_KEY, JSON.stringify(user));
    } else {
      localStorage.removeItem(STORAGE_KEY);
    }
  }
}
