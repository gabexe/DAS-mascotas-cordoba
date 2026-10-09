export type SexoMascota = 'Macho' | 'Hembra';
export type EspecieMascota = 'Perro' | 'Gato' | 'Otro';

export interface Mascota {
  nro_reg_municipal: number;
  nombre: string;
  especie: EspecieMascota;
  sexo: SexoMascota;
  anio_nacimiento: number; // Mapeado de año_nacimiento del modelo relacional
  microchip?: string | null;
  vive: boolean;
  id_refugio: number;
  raza?: string;
}
