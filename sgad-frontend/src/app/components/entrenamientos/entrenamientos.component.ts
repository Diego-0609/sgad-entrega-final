import { Component, OnInit } from '@angular/core';
import { AuthService } from '../../services/auth.service';
import { EntrenamientoService, EntrenadorService } from '../../services/api.service';
import { Entrenamiento, Entrenador } from '../../models/models';
import { Paginado } from '../../util/paginado';

@Component({
  selector: 'app-entrenamientos',
  template: `
    <div class="page">
      <div class="page-header">
        <h1>📅 Entrenamientos</h1>
        <button class="btn-primary" *ngIf="puedeEditar" (click)="abrirModal()">+ Nuevo Entrenamiento</button>
      </div>

      <app-paginacion [pag]="pag" modo="buscador" placeholder="Buscar por fecha, lugar o entrenador..."></app-paginacion>

      <div class="table-wrapper">
        <table>
          <thead>
            <tr>
              <th>Fecha</th><th>Hora</th><th>Lugar</th><th>Entrenador</th><th *ngIf="puedeEditar">Acciones</th>
            </tr>
          </thead>
          <tbody>
            <tr *ngFor="let t of pag.visibles">
              <td>{{ t.fecha }}</td>
              <td>{{ t.hora }}</td>
              <td>{{ t.lugar }}</td>
              <td>{{ nombreEntrenador(t.idEntrenador) }}</td>
              <td class="acciones" *ngIf="puedeEditar">
                <button class="btn-edit" (click)="editar(t)">✏️</button>
                <button class="btn-del" (click)="eliminar(t.idEntrenamiento!)">🗑️</button>
              </td>
            </tr>
            <tr *ngIf="pag.total === 0">
              <td colspan="5" class="empty">No hay entrenamientos registrados aún</td>
            </tr>
          </tbody>
        </table>
      </div>
      <app-paginacion [pag]="pag" modo="pie"></app-paginacion>

      <div class="modal-overlay" *ngIf="mostrarModal" (click)="cerrarModal()">
        <div class="modal" (click)="$event.stopPropagation()">
          <h2>{{ editando ? 'Editar' : 'Nuevo' }} Entrenamiento</h2>
          <form (ngSubmit)="guardar()">
            <label>Fecha</label>
            <input [(ngModel)]="form.fecha" name="fecha" type="date" required/>
            <label>Hora</label>
            <input [(ngModel)]="form.hora" name="hora" type="time" required/>
            <label>Lugar</label>
            <input [(ngModel)]="form.lugar" name="lugar" required placeholder="Ej: Cancha principal"/>
            <label>Entrenador</label>
            <select [(ngModel)]="form.idEntrenador" name="idEntrenador" required>
              <option value="">-- Selecciona entrenador --</option>
              <option *ngFor="let e of entrenadores" [value]="e.idEntrenador">{{ e.nombre }}</option>
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
export class EntrenamientosComponent implements OnInit {
  pag = new Paginado<Entrenamiento>(t =>
    `${t.fecha} ${t.hora} ${t.lugar} ${this.nombreEntrenador(t.idEntrenador)}`);
  entrenadores: Entrenador[] = [];
  mostrarModal = false;
  editando = false;
  idEditando?: string;
  form: Entrenamiento = { fecha: '', hora: '', lugar: '', idEntrenador: '' };

  constructor(private svc: EntrenamientoService, private entrenadorSvc: EntrenadorService, private auth: AuthService) {}

  // El rol DEPORTISTA solo consulta; crear/editar/borrar es de ADMIN y ENTRENADOR
  get puedeEditar(): boolean { return this.auth.esPersonal(); }

  ngOnInit() {
    this.cargar();
    this.entrenadorSvc.listar().subscribe(d => { this.entrenadores = d; this.pag.recalcular(); });
  }

  cargar() { this.svc.listar().subscribe(d => this.pag.set(d)); }

  nombreEntrenador(id: string): string {
    return this.entrenadores.find(e => e.idEntrenador === id)?.nombre ?? '—';
  }

  abrirModal() {
    this.editando = false;
    this.form = { fecha: '', hora: '', lugar: '', idEntrenador: '' };
    this.mostrarModal = true;
  }

  editar(t: Entrenamiento) {
    this.editando = true;
    this.idEditando = t.idEntrenamiento;
    this.form = { ...t };
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
    if (confirm('¿Eliminar este entrenamiento?')) {
      this.svc.eliminar(id).subscribe(() => this.cargar());
    }
  }

  cerrarModal() { this.mostrarModal = false; }
}
