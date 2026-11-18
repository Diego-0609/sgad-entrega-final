import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { BehaviorSubject, Observable, tap } from 'rxjs';
import { environment } from '../../environments/environment';
import {
  Escuela, Entrenador, Deportista, Entrenamiento, Asistencia, Material,
  PerfilMio, EntrenamientoMio, EstadisticasMias, Reporte
} from '../models/models';

const API = environment.apiUrl;

// ─── ESCUELA ───────────────────────────────────────────────
@Injectable({ providedIn: 'root' })
export class EscuelaService {
  private url = `${API}/escuelas`;
  constructor(private http: HttpClient) {}
  listar(): Observable<Escuela[]> { return this.http.get<Escuela[]>(this.url); }
  buscar(id: string): Observable<Escuela> { return this.http.get<Escuela>(`${this.url}/${id}`); }
  crear(e: Escuela): Observable<Escuela> { return this.http.post<Escuela>(this.url, e); }
  actualizar(id: string, e: Escuela): Observable<Escuela> { return this.http.put<Escuela>(`${this.url}/${id}`, e); }
  eliminar(id: string): Observable<void> { return this.http.delete<void>(`${this.url}/${id}`); }
}

// ─── ENTRENADOR ────────────────────────────────────────────
@Injectable({ providedIn: 'root' })
export class EntrenadorService {
  private url = `${API}/entrenadores`;
  constructor(private http: HttpClient) {}
  listar(): Observable<Entrenador[]> { return this.http.get<Entrenador[]>(this.url); }
  listarPorEscuela(idEscuela: string): Observable<Entrenador[]> { return this.http.get<Entrenador[]>(`${this.url}/escuela/${idEscuela}`); }
  buscar(id: string): Observable<Entrenador> { return this.http.get<Entrenador>(`${this.url}/${id}`); }
  crear(e: Entrenador): Observable<Entrenador> { return this.http.post<Entrenador>(this.url, e); }
  actualizar(id: string, e: Entrenador): Observable<Entrenador> { return this.http.put<Entrenador>(`${this.url}/${id}`, e); }
  eliminar(id: string): Observable<void> { return this.http.delete<void>(`${this.url}/${id}`); }
}

// ─── DEPORTISTA ────────────────────────────────────────────
@Injectable({ providedIn: 'root' })
export class DeportistaService {
  private url = `${API}/deportistas`;
  constructor(private http: HttpClient) {}
  listar(): Observable<Deportista[]> { return this.http.get<Deportista[]>(this.url); }
  listarPorEscuela(idEscuela: string): Observable<Deportista[]> { return this.http.get<Deportista[]>(`${this.url}/escuela/${idEscuela}`); }
  buscar(id: string): Observable<Deportista> { return this.http.get<Deportista>(`${this.url}/${id}`); }
  crear(d: Deportista): Observable<Deportista> { return this.http.post<Deportista>(this.url, d); }
  actualizar(id: string, d: Deportista): Observable<Deportista> { return this.http.put<Deportista>(`${this.url}/${id}`, d); }
  eliminar(id: string): Observable<void> { return this.http.delete<void>(`${this.url}/${id}`); }
}

// ─── ENTRENAMIENTO ─────────────────────────────────────────
@Injectable({ providedIn: 'root' })
export class EntrenamientoService {
  private url = `${API}/entrenamientos`;
  constructor(private http: HttpClient) {}
  listar(): Observable<Entrenamiento[]> { return this.http.get<Entrenamiento[]>(this.url); }
  listarPorEntrenador(idEntrenador: string): Observable<Entrenamiento[]> { return this.http.get<Entrenamiento[]>(`${this.url}/entrenador/${idEntrenador}`); }
  buscar(id: string): Observable<Entrenamiento> { return this.http.get<Entrenamiento>(`${this.url}/${id}`); }
  crear(t: Entrenamiento): Observable<Entrenamiento> { return this.http.post<Entrenamiento>(this.url, t); }
  actualizar(id: string, t: Entrenamiento): Observable<Entrenamiento> { return this.http.put<Entrenamiento>(`${this.url}/${id}`, t); }
  eliminar(id: string): Observable<void> { return this.http.delete<void>(`${this.url}/${id}`); }
}

// ─── ASISTENCIA ────────────────────────────────────────────
@Injectable({ providedIn: 'root' })
export class AsistenciaService {
  private url = `${API}/asistencias`;
  constructor(private http: HttpClient) {}
  listar(): Observable<Asistencia[]> { return this.http.get<Asistencia[]>(this.url); }
  listarPorEntrenamiento(id: string): Observable<Asistencia[]> { return this.http.get<Asistencia[]>(`${this.url}/entrenamiento/${id}`); }
  listarPorDeportista(id: string): Observable<Asistencia[]> { return this.http.get<Asistencia[]>(`${this.url}/deportista/${id}`); }
  crear(a: Asistencia): Observable<Asistencia> { return this.http.post<Asistencia>(this.url, a); }
  actualizar(id: string, a: Asistencia): Observable<Asistencia> { return this.http.put<Asistencia>(`${this.url}/${id}`, a); }
  eliminar(id: string): Observable<void> { return this.http.delete<void>(`${this.url}/${id}`); }
}

// ─── MATERIAL ──────────────────────────────────────────────
@Injectable({ providedIn: 'root' })
export class MaterialService {
  private url = `${API}/materiales`;
  constructor(private http: HttpClient) {}
  listar(): Observable<Material[]> { return this.http.get<Material[]>(this.url); }
  listarPorEscuela(idEscuela: string): Observable<Material[]> { return this.http.get<Material[]>(`${this.url}/escuela/${idEscuela}`); }
  buscar(id: string): Observable<Material> { return this.http.get<Material>(`${this.url}/${id}`); }
  crear(m: Material): Observable<Material> { return this.http.post<Material>(this.url, m); }
  actualizar(id: string, m: Material): Observable<Material> { return this.http.put<Material>(`${this.url}/${id}`, m); }
  eliminar(id: string): Observable<void> { return this.http.delete<void>(`${this.url}/${id}`); }
}

// ─── REPORTES (los envía el personal) ──────────────────────
@Injectable({ providedIn: 'root' })
export class ReporteService {
  private url = `${API}/reportes`;
  constructor(private http: HttpClient) {}
  listar(): Observable<Reporte[]> { return this.http.get<Reporte[]>(this.url); }
  crear(r: Reporte): Observable<Reporte> { return this.http.post<Reporte>(this.url, r); }
  eliminar(id: string): Observable<void> { return this.http.delete<void>(`${this.url}/${id}`); }
}

// ─── PANEL DEL DEPORTISTA (solo sus propios datos) ─────────
@Injectable({ providedIn: 'root' })
export class MiService {
  private url = `${API}/mi`;
  /** Reportes sin leer: lo muestra la barra de navegación como avisos del buzón */
  readonly noLeidos$ = new BehaviorSubject<number>(0);

  constructor(private http: HttpClient) {}

  perfil(): Observable<PerfilMio> {
    return this.http.get<PerfilMio>(`${this.url}/perfil`).pipe(tap(p => this.noLeidos$.next(p.reportesNoLeidos)));
  }
  entrenamientos(): Observable<EntrenamientoMio[]> { return this.http.get<EntrenamientoMio[]>(`${this.url}/entrenamientos`); }
  asistencias(): Observable<EntrenamientoMio[]> { return this.http.get<EntrenamientoMio[]>(`${this.url}/asistencias`); }
  estadisticas(): Observable<EstadisticasMias> { return this.http.get<EstadisticasMias>(`${this.url}/estadisticas`); }
  reportes(): Observable<Reporte[]> {
    return this.http.get<Reporte[]>(`${this.url}/reportes`).pipe(
      tap(lista => this.noLeidos$.next(lista.filter(r => !r.leido).length)));
  }
  marcarLeido(id: string): Observable<Reporte> {
    return this.http.put<Reporte>(`${this.url}/reportes/${id}/leido`, {}).pipe(
      tap(() => this.noLeidos$.next(Math.max(0, this.noLeidos$.value - 1))));
  }
}
