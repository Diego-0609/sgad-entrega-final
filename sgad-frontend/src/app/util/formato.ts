const LOCALE = 'es-CO';

/** "2026-11-04" -> "mié, 4 nov 2026" */
export function formatearFecha(iso: string | null | undefined): string {
  if (!iso) { return '—'; }
  const fecha = new Date(`${iso.substring(0, 10)}T00:00:00`);
  return fecha.toLocaleDateString(LOCALE, { weekday: 'short', day: 'numeric', month: 'short', year: 'numeric' });
}

/** "15:30:00" -> "3:30 p. m." */
export function formatearHora(hora: string | null | undefined): string {
  if (!hora) { return '—'; }
  const [h, m] = hora.split(':').map(Number);
  const fecha = new Date();
  fecha.setHours(h, m, 0, 0);
  return fecha.toLocaleTimeString(LOCALE, { hour: 'numeric', minute: '2-digit' });
}

/** "2026-11-04T10:15:00" -> "4 nov 2026, 10:15 a. m." */
export function formatearFechaHora(iso: string | null | undefined): string {
  if (!iso) { return '—'; }
  return new Date(iso).toLocaleString(LOCALE, { day: 'numeric', month: 'short', year: 'numeric', hour: 'numeric', minute: '2-digit' });
}

/** Un entrenamiento ya pasó si su fecha y hora quedaron atrás (hora de Colombia, igual que el servidor). */
export function yaPaso(fecha: string, hora: string | null): boolean {
  const ahora = new Date().toLocaleString('sv-SE', { timeZone: 'America/Bogota' }).replace(' ', 'T');
  return `${fecha}T${hora ?? '00:00:00'}` < ahora;
}
