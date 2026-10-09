export interface Refugio {
  id_refugio: number;
  razon_social: string;
  correo: string;
  telefono: string;
  domicilio: string;
  habilitacion_municipal: string;
}

export interface UsuarioRefugio {
  id_ciudadano: number;
  cuil: string;
  nombre: string;
  apellido: string;
  correo: string;
  telefono: string;
  id_refugio: number;
  refugio: Refugio;
}
