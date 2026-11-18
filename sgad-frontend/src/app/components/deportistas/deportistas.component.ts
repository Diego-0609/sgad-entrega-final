import { Component, OnInit } from '@angular/core';
import { DeportistaService, EscuelaService } from '../../services/api.service';
import { Deportista, Escuela } from '../../models/models';
import { Paginado } from '../../util/paginado';

@Component({
  selector: 'app-deportistas',
  template: `
    <div class="page">
      <div class="page-header">
        <h1>🏃 Deportistas</h1>
        <button class="btn-primary" (click)="abrirModal()">+ Nuevo Deportista</button>
      </div>

      <app-paginacion [pag]="pag" modo="buscador" placeholder="Buscar por nombre, deporte, género o escuela..."></app-paginacion>

      <div class="table-wrapper">
        <table>
          <thead>
            <tr>
              <th>Nombre</th><th>Edad</th><th>Género</th><th>Deporte</th><th>Escuela</th><th>Acciones</th>
            </tr>
          </thead>
          <tbody>
            <tr *ngFor="let d of pag.visibles">
              <td>{{ d.nombre }}</td>
              <td>{{ d.edad }}</td>
              <td>{{ d.genero }}</td>
              <td><span class="badge">{{ d.deporte }}</span></td>
              <td>{{ nombreEscuela(d.idEscuela) }}</td>
              <td class="acciones">
                <button class="btn-edit" (click)="editar(d)">✏️</button>
                <button class="btn-del" (click)="eliminar(d.idDeportista!)">🗑️</button>
              </td>
            </tr>
            <tr *ngIf="pag.total === 0">
              <td colspan="6" class="empty">No hay deportistas registrados aún</td>
            </tr>
          </tbody>
        </table>
      </div>
      <app-paginacion [pag]="pag" modo="pie"></app-paginacion>

      <div class="modal-overlay" *ngIf="mostrarModal" (click)="cerrarModal()">
        <div class="modal" (click)="$event.stopPropagation()">
          <h2>{{ editando ? 'Editar' : 'Nuevo' }} Deportista</h2>
          <form (ngSubmit)="guardar()">
            <label>Nombre</label>
            <input [(ngModel)]="form.nombre" name="nombre" required placeholder="Nombre completo"/>
            <label>Edad</label>
            <input [(ngModel)]="form.edad" name="edad" type="number" required placeholder="Ej: 18"/>
            <label>Género</label>
            <select [(ngModel)]="form.genero" name="genero" required>
              <option value="">-- Selecciona --</option>
              <option value="Masculino">Masculino</option>
              <option value="Femenino">Femenino</option>
              <option value="Otro">Otro</option>
            </select>
            <label>Deporte</label>
            <input [(ngModel)]="form.deporte" name="deporte" required placeholder="Ej: Fútbol, Natación"/>
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
export class DeportistasComponent implements OnInit {
  pag = new Paginado<Deportista>(d =>
    `${d.nombre} ${d.deporte} ${d.genero} ${this.nombreEscuela(d.idEscuela)}`);
  escuelas: Escuela[] = [];
  mostrarModal = false;
  editando = false;
  idEditando?: string;
  form: Deportista = { nombre: '', edad: 0, genero: '', deporte: '', idEscuela: '' };

  constructor(private svc: DeportistaService, private escuelaSvc: EscuelaService) {}

  ngOnInit() {
    this.cargar();
    this.escuelaSvc.listar().subscribe(d => { this.escuelas = d; this.pag.recalcular(); });
  }

  cargar() { this.svc.listar().subscribe(d => this.pag.set(d)); }

  nombreEscuela(id: string): string {
    return this.escuelas.find(e => e.idEscuela === id)?.nombre ?? '—';
  }

  abrirModal() {
    this.editando = false;
    this.form = { nombre: '', edad: 0, genero: '', deporte: '', idEscuela: '' };
    this.mostrarModal = true;
  }

  editar(d: Deportista) {
    this.editando = true;
    this.idEditando = d.idDeportista;
    this.form = { ...d };
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
    if (confirm('¿Eliminar este deportista?')) {
      this.svc.eliminar(id).subscribe(() => this.cargar());
    }
  }

  cerrarModal() { this.mostrarModal = false; }
}
