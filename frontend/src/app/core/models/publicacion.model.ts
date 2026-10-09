import { Mascota } from './mascota.model';
import { Refugio } from './refugio.model';

export type EstadoPublicacion = 'Activa' | 'Pausada' | 'Finalizada';

export interface PublicacionAdopcion {
  nro_publicacion: number;
  nro_reg_municipal: number;
  id_refugio: number;
  fecha_publicacion: string; // ISO date string
  caracteristicas_mascota: string;
  condicion_adopcion: string;
  foto: string;
  estado_publicacion: EstadoPublicacion;
  // Relaciones cargadas para la vista
  mascota?: Mascota;
  refugio?: Refugio;
}

export interface NuevaPublicacionDTO {
  // Datos de la mascota
  nombre: string;
  especie: 'Perro' | 'Gato' | 'Otro';
  sexo: 'Macho' | 'Hembra';
  anio_nacimiento: number;
  raza: string;
  microchip?: string;
  // Datos de la publicación
  foto: string;
  caracteristicas_mascota: string;
  condicion_adopcion: string;
}
