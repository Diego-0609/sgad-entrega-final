import { Component, OnInit } from '@angular/core';
import { MaterialService, EscuelaService } from '../../services/api.service';
import { Material, Escuela } from '../../models/models';

@Component({
  selector: 'app-materiales',
  template: `
    <div class="page">
      <div class="page-header">
        <h1>🎽 Materiales</h1>
        <button class="btn-primary" (click)="abrirModal()">+ Nuevo Material</button>
      </div>

      <div class="table-wrapper">
        <table>
          <thead>
            <tr>
              <th>Nombre</th><th>Cantidad</th><th>Estado</th><th>Escuela</th><th>Acciones</th>
            </tr>
          </thead>
          <tbody>
            <tr *ngFor="let m of materiales">
              <td>{{ m.nombre }}</td>
              <td><span class="badge">{{ m.cantidad }}</span></td>
              <td>{{ m.estado }}</td>
              <td>{{ nombreEscuela(m.idEscuela) }}</td>
              <td class="acciones">
                <button class="btn-edit" (click)="editar(m)">✏️</button>
                <button class="btn-del" (click)="eliminar(m.idMaterial!)">🗑️</button>
              </td>
            </tr>
            <tr *ngIf="materiales.length === 0">
              <td colspan="5" class="empty">No hay materiales registrados aún</td>
            </tr>
          </tbody>
        </table>
      </div>

      <div class="modal-overlay" *ngIf="mostrarModal" (click)="cerrarModal()">
        <div class="modal" (click)="$event.stopPropagation()">
          <h2>{{ editando ? 'Editar' : 'Nuevo' }} Material</h2>
          <form (ngSubmit)="guardar()">
            <label>Nombre</label>
            <input [(ngModel)]="form.nombre" name="nombre" required placeholder="Ej: Balones de fútbol"/>
            <label>Cantidad</label>
            <input [(ngModel)]="form.cantidad" name="cantidad" type="number" required placeholder="Ej: 10"/>
            <label>Estado</label>
            <select [(ngModel)]="form.estado" name="estado" required>
              <option value="">-- Selecciona estado --</option>
              <option value="Bueno">Bueno</option>
              <option value="Regular">Regular</option>
              <option value="Malo">Malo</option>
            </select>
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
export class MaterialesComponent implements OnInit {
  materiales: Material[] = [];
  escuelas: Escuela[] = [];
  mostrarModal = false;
  editando = false;
  idEditando?: string;
  form: Material = { nombre: '', cantidad: 0, estado: '', idEscuela: '' };

  constructor(private svc: MaterialService, private escuelaSvc: EscuelaService) {}

  ngOnInit() {
    this.cargar();
    this.escuelaSvc.listar().subscribe(d => this.escuelas = d);
  }

  cargar() { this.svc.listar().subscribe(d => this.materiales = d); }

  nombreEscuela(id: string): string {
    return this.escuelas.find(e => e.idEscuela === id)?.nombre ?? '—';
  }

  abrirModal() {
    this.editando = false;
    this.form = { nombre: '', cantidad: 0, estado: '', idEscuela: '' };
    this.mostrarModal = true;
  }

  editar(m: Material) {
    this.editando = true;
    this.idEditando = m.idMaterial;
    this.form = { ...m };
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
    if (confirm('¿Eliminar este material?')) {
      this.svc.eliminar(id).subscribe(() => this.cargar());
    }
  }

  cerrarModal() { this.mostrarModal = false; }
}
