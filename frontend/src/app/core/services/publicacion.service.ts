import { Injectable, signal } from '@angular/core';
import { EstadoPublicacion, NuevaPublicacionDTO, PublicacionAdopcion } from '../models/publicacion.model';

const MOCK_PUBLICACIONES: PublicacionAdopcion[] = [
  {
    nro_publicacion: 1001,
    nro_reg_municipal: 50123,
    id_refugio: 101,
    fecha_publicacion: '2026-09-15',
    caracteristicas_mascota: 'Muy cariñoso, sociable con niños y otros perros. Tiene energía media-alta y sabe pasear con correa.',
    condicion_adopcion: 'Hogar con patio cerrado o compromiso de paseos diarios. Esquema de vacunación al día y seguimiento.',
    foto: 'https://images.unsplash.com/photo-1543466835-00a7907e9de1?auto=format&fit=crop&w=600&q=80',
    estado_publicacion: 'Activa',
    mascota: {
      nro_reg_municipal: 50123,
      nombre: 'Milo',
      especie: 'Perro',
      sexo: 'Macho',
      anio_nacimiento: 2023,
      raza: 'Mestizo de Labrador',
      microchip: 'MC-CBA-99124',
      vive: true,
      id_refugio: 101
    }
  },
  {
    nro_publicacion: 1002,
    nro_reg_municipal: 50124,
    id_refugio: 101,
    fecha_publicacion: '2026-09-20',
    caracteristicas_mascota: 'Tranquila, ronronea mucho. Ideal para departamento. Acostumbrada a usar rascador y piedritas.',
    condicion_adopcion: 'Depto con protección en balcones/ventanas. Compromiso de mantener al día desparasitación.',
    foto: 'https://images.unsplash.com/photo-1514888286974-6c03e2ca1dba?auto=format&fit=crop&w=600&q=80',
    estado_publicacion: 'Activa',
    mascota: {
      nro_reg_municipal: 50124,
      nombre: 'Luna',
      especie: 'Gato',
      sexo: 'Hembra',
      anio_nacimiento: 2024,
      raza: 'Europeo Común',
      microchip: 'MC-CBA-99125',
      vive: true,
      id_refugio: 101
    }
  },
  {
    nro_publicacion: 1003,
    nro_reg_municipal: 50125,
    id_refugio: 101,
    fecha_publicacion: '2026-08-10',
    caracteristicas_mascota: 'Excelente guardián y compañero de caminatas. En tratamiento por leve dermatitis ya controlada.',
    condicion_adopcion: 'Disponibilidad pausada momentáneamente por control veterinario programado.',
    foto: 'https://images.unsplash.com/photo-1583511655857-d19b40a7a54e?auto=format&fit=crop&w=600&q=80',
    estado_publicacion: 'Pausada',
    mascota: {
      nro_reg_municipal: 50125,
      nombre: 'Thor',
      especie: 'Perro',
      sexo: 'Macho',
      anio_nacimiento: 2021,
      raza: 'Mestizo Ovejero',
      microchip: null,
      vive: true,
      id_refugio: 101
    }
  },
  {
    nro_publicacion: 1004,
    nro_reg_municipal: 50126,
    id_refugio: 101,
    fecha_publicacion: '2026-07-01',
    caracteristicas_mascota: 'Dócil y juguetona. ¡Ya encontró su familia definitiva en barrio General Paz!',
    condicion_adopcion: 'Proceso de adopción finalizado exitosamente.',
    foto: 'https://images.unsplash.com/photo-1537151625747-768eb6cf92b2?auto=format&fit=crop&w=600&q=80',
    estado_publicacion: 'Finalizada',
    mascota: {
      nro_reg_municipal: 50126,
      nombre: 'Pelusa',
      especie: 'Perro',
      sexo: 'Hembra',
      anio_nacimiento: 2022,
      raza: 'Mestiza Caniche',
      microchip: 'MC-CBA-88712',
      vive: true,
      id_refugio: 101
    }
  },
  {
    nro_publicacion: 1005,
    nro_reg_municipal: 50127,
    id_refugio: 102,
    fecha_publicacion: '2026-09-28',
    caracteristicas_mascota: 'Joven y muy curioso. Se lleva bien con otros felinos.',
    condicion_adopcion: 'Ambiente seguro sin acceso libre a la calle.',
    foto: 'https://images.unsplash.com/photo-1573865526739-10659fec78a5?auto=format&fit=crop&w=600&q=80',
    estado_publicacion: 'Activa',
    mascota: {
      nro_reg_municipal: 50127,
      nombre: 'Simba',
      especie: 'Gato',
      sexo: 'Macho',
      anio_nacimiento: 2025,
      raza: 'Siamés mestizo',
      microchip: null,
      vive: true,
      id_refugio: 102
    }
  }
];

const STORAGE_KEY = 'mascotas_cba_publicaciones';

function getStoredPublicaciones(): PublicacionAdopcion[] {
  try {
    const stored = localStorage.getItem(STORAGE_KEY);
    return stored ? JSON.parse(stored) : MOCK_PUBLICACIONES;
  } catch {
    return MOCK_PUBLICACIONES;
  }
}

function persistPublicaciones(pubs: PublicacionAdopcion[]): void {
  try {
    localStorage.setItem(STORAGE_KEY, JSON.stringify(pubs));
  } catch (err) {
    console.error('Error al persistir publicaciones:', err);
  }
}

@Injectable({
  providedIn: 'root'
})
export class PublicacionService {
  private publicacionesSignal = signal<PublicacionAdopcion[]>(getStoredPublicaciones());

  getPublicaciones() {
    return this.publicacionesSignal.asReadonly();
  }

  getPublicacionesByRefugio(id_refugio: number): PublicacionAdopcion[] {
    return this.publicacionesSignal().filter(p => p.id_refugio === id_refugio);
  }

  getPublicacionById(nro_publicacion: number): PublicacionAdopcion | undefined {
    return this.publicacionesSignal().find(p => p.nro_publicacion === nro_publicacion);
  }

  getPublicacionesActivas(): PublicacionAdopcion[] {
    return this.publicacionesSignal().filter(p => p.estado_publicacion === 'Activa');
  }

  cambiarEstado(nro_publicacion: number, nuevoEstado: EstadoPublicacion): boolean {
    const list = this.publicacionesSignal();
    const index = list.findIndex(p => p.nro_publicacion === nro_publicacion);
    if (index === -1) return false;

    const actual = list[index].estado_publicacion;
    if (actual === 'Finalizada') {
      console.warn('Una publicación Finalizada no puede cambiar de estado.');
      return false;
    }

    const updated = [...list];
    updated[index] = {
      ...updated[index],
      estado_publicacion: nuevoEstado
    };

    this.publicacionesSignal.set(updated);
    persistPublicaciones(updated);
    return true;
  }

  crearPublicacion(dto: NuevaPublicacionDTO, id_refugio: number): PublicacionAdopcion {
    const nextReg = Math.floor(50000 + Math.random() * 40000);
    const nextPub = Math.floor(2000 + Math.random() * 8000);

    const defaultImg = dto.foto?.trim() || (dto.especie === 'Gato' 
      ? 'https://images.unsplash.com/photo-1514888286974-6c03e2ca1dba?auto=format&fit=crop&w=600&q=80'
      : 'https://images.unsplash.com/photo-1543466835-00a7907e9de1?auto=format&fit=crop&w=600&q=80');

    const nueva: PublicacionAdopcion = {
      nro_publicacion: nextPub,
      nro_reg_municipal: nextReg,
      id_refugio: id_refugio,
      fecha_publicacion: new Date().toISOString().split('T')[0],
      caracteristicas_mascota: dto.caracteristicas_mascota,
      condicion_adopcion: dto.condicion_adopcion,
      foto: defaultImg,
      estado_publicacion: 'Activa',
      mascota: {
        nro_reg_municipal: nextReg,
        nombre: dto.nombre,
        especie: dto.especie,
        sexo: dto.sexo,
        anio_nacimiento: Number(dto.anio_nacimiento),
        raza: dto.raza || 'Mestizo',
        microchip: dto.microchip || null,
        vive: true,
        id_refugio: id_refugio
      }
    };

    this.publicacionesSignal.update(pubs => {
      const updated = [nueva, ...pubs];
      persistPublicaciones(updated);
      return updated;
    });
    return nueva;
  }
}
