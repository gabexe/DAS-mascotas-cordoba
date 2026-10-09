import { HttpErrorResponse } from '@angular/common/http';

// =========================================================================
// Traducción de errores HTTP a mensajes legibles para el usuario
// Contrato backend: { status, error, mensaje, detalles? }
// =========================================================================
export function mensajeDeError(err: unknown, porDefecto: string): string {
  if (!(err instanceof HttpErrorResponse)) return porDefecto;
  if (err.status === 0) return 'No se pudo conectar con el servidor. Intente nuevamente.';

  const detalles = err.error?.detalles as Record<string, string> | undefined;
  if (detalles && Object.keys(detalles).length > 0) return Object.values(detalles).join('. ');
  return err.error?.mensaje || porDefecto;
}
