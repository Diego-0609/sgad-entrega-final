import { Component, OnInit } from '@angular/core';
import { EntrenadorService, EscuelaService } from '../../services/api.service';
import { Entrenador, Escuela } from '../../models/models';

@Component({
  selector: 'app-entrenadores',
  template: `
    <div class="page">
      <div class="page-header">
        <h1>🧑‍🏫 Entrenadores</h1>
        <button class="btn-primary" (click)="abrirModal()">+ Nuevo Entrenador</button>
      </div>

      <div class="table-wrapper">
        <table>
          <thead>
            <tr>
              <th>Nombre</th><th>Especialidad</th><th>Teléfono</th><th>Escuela</th><th>Acciones</th>
            </tr>
          </thead>
          <tbody>
            <tr *ngFor="let e of entrenadores">
              <td>{{ e.nombre }}</td>
              <td>{{ e.especialidad }}</td>
              <td>{{ e.telefono }}</td>
              <td>{{ nombreEscuela(e.idEscuela) }}</td>
              <td class="acciones">
                <button class="btn-edit" (click)="editar(e)">✏️</button>
                <button class="btn-del" (click)="eliminar(e.idEntrenador!)">🗑️</button>
              </td>
            </tr>
            <tr *ngIf="entrenadores.length === 0">
              <td colspan="5" class="empty">No hay entrenadores registrados aún</td>
            </tr>
          </tbody>
        </table>
      </div>

      <div class="modal-overlay" *ngIf="mostrarModal" (click)="cerrarModal()">
        <div class="modal" (click)="$event.stopPropagation()">
          <h2>{{ editando ? 'Editar' : 'Nuevo' }} Entrenador</h2>
          <form (ngSubmit)="guardar()">
            <label>Nombre</label>
            <input [(ngModel)]="form.nombre" name="nombre" required placeholder="Nombre completo"/>
            <label>Especialidad</label>
            <input [(ngModel)]="form.especialidad" name="especialidad" required placeholder="Ej: Fútbol, Natación"/>
            <label>Teléfono</label>
            <input [(ngModel)]="form.telefono" name="telefono" required placeholder="Ej: 3001234567"/>
            <label>Escuela</label>
            <select [(ngModel)]="form.idEscuela" name="idEscuela" required>
              <option value="">-- Selecciona escuela --</option>
              <option *ngFor="let es of escuelas" [value]="es.idEscuela">{{ es.nombre }}</option>
            </select>
            <div class="modal-actions">
              <button type="button" class="btn-cancel" (click)="cerrarModal()">Cancelar</button>
              <button type="submit" class="btn-primary">{{ editando ? 'Actualizar' : 'Guardar' }}</button>
            </div>
          </form>
        </div>
      </div>
    </div>
  `
})
export class EntrenadoresComponent implements OnInit {
  entrenadores: Entrenador[] = [];
  escuelas: Escuela[] = [];
  mostrarModal = false;
  editando = false;
  idEditando?: string;
  form: Entrenador = { nombre: '', especialidad: '', telefono: '', idEscuela: '' };

  constructor(private svc: EntrenadorService, private escuelaSvc: EscuelaService) {}

  ngOnInit() {
    this.cargar();
    this.escuelaSvc.listar().subscribe(d => this.escuelas = d);
  }

  cargar() { this.svc.listar().subscribe(d => this.entrenadores = d); }

  nombreEscuela(id: string): string {
    return this.escuelas.find(e => e.idEscuela === id)?.nombre ?? '—';
  }

  abrirModal() {
    this.editando = false;
    this.form = { nombre: '', especialidad: '', telefono: '', idEscuela: '' };
    this.mostrarModal = true;
  }

  editar(e: Entrenador) {
    this.editando = true;
    this.idEditando = e.idEntrenador;
    this.form = { ...e };
    this.mostrarModal = true;
  }

  guardar() {
    if (this.editando && this.idEditando) {
      this.svc.actualizar(this.idEditando, this.form).subscribe(() => { this.cargar(); this.cerrarModal(); });
    } else {
      this.svc.crear(this.form).subscribe(() => { this.cargar(); this.cerrarModal(); });
    }
  }

  eliminar(id: string) {
    if (confirm('¿Eliminar este entrenador?')) {
      this.svc.eliminar(id).subscribe(() => this.cargar());
    }
  }

  cerrarModal() { this.mostrarModal = false; }
}
