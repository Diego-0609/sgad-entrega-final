import { Component, OnInit } from '@angular/core';
import { EscuelaService } from '../../services/api.service';
import { Escuela } from '../../models/models';

@Component({
  selector: 'app-escuelas',
  template: `
    <div class="page">
      <div class="page-header">
        <h1>🏫 Escuelas</h1>
        <button class="btn-primary" (click)="abrirModal()">+ Nueva Escuela</button>
      </div>

      <div class="table-wrapper">
        <table>
          <thead>
            <tr>
              <th>Nombre</th><th>Dirección</th><th>Teléfono</th><th>Acciones</th>
            </tr>
          </thead>
          <tbody>
            <tr *ngFor="let e of escuelas">
              <td>{{ e.nombre }}</td>
              <td>{{ e.direccion }}</td>
              <td>{{ e.telefono }}</td>
              <td class="acciones">
                <button class="btn-edit" (click)="editar(e)">✏️</button>
                <button class="btn-del" (click)="eliminar(e.idEscuela!)">🗑️</button>
              </td>
            </tr>
            <tr *ngIf="escuelas.length === 0">
              <td colspan="4" class="empty">No hay escuelas registradas aún</td>
            </tr>
          </tbody>
        </table>
      </div>

      <!-- Modal -->
      <div class="modal-overlay" *ngIf="mostrarModal" (click)="cerrarModal()">
        <div class="modal" (click)="$event.stopPropagation()">
          <h2>{{ editando ? 'Editar' : 'Nueva' }} Escuela</h2>
          <form (ngSubmit)="guardar()">
            <label>Nombre</label>
            <input [(ngModel)]="form.nombre" name="nombre" required placeholder="Ej: Escuela Deportiva Bocagrande"/>
            <label>Dirección</label>
            <input [(ngModel)]="form.direccion" name="direccion" required placeholder="Ej: Cra 1 #23-45"/>
            <label>Teléfono</label>
            <input [(ngModel)]="form.telefono" name="telefono" required placeholder="Ej: 3001234567"/>
            <div class="modal-actions">
              <button type="button" class="btn-cancel" (click)="cerrarModal()">Cancelar</button>
              <button type="submit" class="btn-primary">{{ editando ? 'Actualizar' : 'Guardar' }}</button>
            </div>
          </form>
        </div>
      </div>
    </div>
  `,
  styles: [``]
})
export class EscuelasComponent implements OnInit {
  escuelas: Escuela[] = [];
  mostrarModal = false;
  editando = false;
  idEditando?: string;
  form: Escuela = { nombre: '', direccion: '', telefono: '' };

  constructor(private svc: EscuelaService) {}

  ngOnInit() { this.cargar(); }

  cargar() { this.svc.listar().subscribe(d => this.escuelas = d); }

  abrirModal() {
    this.editando = false;
    this.form = { nombre: '', direccion: '', telefono: '' };
    this.mostrarModal = true;
  }

  editar(e: Escuela) {
    this.editando = true;
    this.idEditando = e.idEscuela;
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
    if (confirm('¿Seguro que quieres eliminar esta escuela?')) {
      this.svc.eliminar(id).subscribe(() => this.cargar());
    }
  }

  cerrarModal() { this.mostrarModal = false; }
}
