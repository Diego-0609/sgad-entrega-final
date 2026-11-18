import { Component, OnInit } from '@angular/core';
import { AuthService } from '../../services/auth.service';
import { AsistenciaService, DeportistaService, EntrenamientoService } from '../../services/api.service';
import { Asistencia, Deportista, Entrenamiento } from '../../models/models';
import { Paginado } from '../../util/paginado';

@Component({
  selector: 'app-asistencias',
  template: `
    <div class="page">
      <div class="page-header">
        <h1>✅ Asistencias</h1>
        <button class="btn-primary" *ngIf="puedeEditar" (click)="abrirModal()">+ Registrar Asistencia</button>
      </div>

      <app-paginacion [pag]="pag" modo="buscador" placeholder="Buscar por deportista, entrenamiento o estado..."></app-paginacion>

      <div class="table-wrapper">
        <table>
          <thead>
            <tr>
              <th>Deportista</th><th>Entrenamiento</th><th>Estado</th><th *ngIf="puedeEditar">Acciones</th>
            </tr>
          </thead>
          <tbody>
            <tr *ngFor="let a of pag.visibles">
              <td>{{ nombreDeportista(a.idDeportista) }}</td>
              <td>{{ infoEntrenamiento(a.idEntrenamiento) }}</td>
              <td>
                <span class="badge" [class.badge-green]="a.estado === 'presente'" [class.badge-red]="a.estado === 'ausente'">
                  {{ a.estado }}
                </span>
              </td>
              <td class="acciones" *ngIf="puedeEditar">
                <button class="btn-edit" (click)="editar(a)">✏️</button>
                <button class="btn-del" (click)="eliminar(a.idAsistencia!)">🗑️</button>
              </td>
            </tr>
            <tr *ngIf="pag.total === 0">
              <td colspan="4" class="empty">No hay asistencias registradas aún</td>
            </tr>
          </tbody>
        </table>
      </div>
      <app-paginacion [pag]="pag" modo="pie"></app-paginacion>

      <div class="modal-overlay" *ngIf="mostrarModal" (click)="cerrarModal()">
        <div class="modal" (click)="$event.stopPropagation()">
          <h2>{{ editando ? 'Editar' : 'Registrar' }} Asistencia</h2>
          <form (ngSubmit)="guardar()">
            <label>Deportista</label>
            <select [(ngModel)]="form.idDeportista" name="idDeportista" required [disabled]="editando">
              <option value="">-- Selecciona deportista --</option>
              <option *ngFor="let d of deportistas" [value]="d.idDeportista">{{ d.nombre }}</option>
            </select>
            <label>Entrenamiento</label>
            <select [(ngModel)]="form.idEntrenamiento" name="idEntrenamiento" required [disabled]="editando">
              <option value="">-- Selecciona entrenamiento --</option>
              <option *ngFor="let t of entrenamientos" [value]="t.idEntrenamiento">
                {{ t.fecha }} - {{ t.lugar }}
              </option>
            </select>
            <label>Estado</label>
            <select [(ngModel)]="form.estado" name="estado" required>
              <option value="presente">Presente</option>
              <option value="ausente">Ausente</option>
            </select>
            <div class="modal-actions">
              <button type="button" class="btn-cancel" (click)="cerrarModal()">Cancelar</button>
              <button type="submit" class="btn-primary">{{ editando ? 'Actualizar' : 'Guardar' }}</button>
            </div>
          </form>
        </div>
      </div>
    </div>
  `,
  styles: [`
    .badge-green { background: #d4edda; color: #155724; }
    .badge-red { background: #f8d7da; color: #721c24; }
  `]
})
export class AsistenciasComponent implements OnInit {
  // Con miles de registros solo se dibuja una página; los nombres se buscan en mapas, no recorriendo listas.
  pag = new Paginado<Asistencia>(a =>
    `${this.nombreDeportista(a.idDeportista)} ${this.infoEntrenamiento(a.idEntrenamiento)} ${a.estado}`);
  private nombres = new Map<string, string>();
  private fechas = new Map<string, string>();
  deportistas: Deportista[] = [];
  entrenamientos: Entrenamiento[] = [];
  mostrarModal = false;
  editando = false;
  idEditando?: string;
  form: Asistencia = { estado: 'presente', idDeportista: '', idEntrenamiento: '' };

  constructor(
    private svc: AsistenciaService,
    private deportistaSvc: DeportistaService,
    private entrenamientoSvc: EntrenamientoService,
    private auth: AuthService
  ) {}

  // El rol DEPORTISTA solo consulta; crear/editar/borrar es de ADMIN y ENTRENADOR
  get puedeEditar(): boolean { return this.auth.esPersonal(); }

  ngOnInit() {
    this.cargar();
    this.deportistaSvc.listar().subscribe(d => {
      this.deportistas = d;
      this.nombres = new Map(d.map(x => [x.idDeportista!, x.nombre] as [string, string]));
      this.pag.recalcular();
    });
    this.entrenamientoSvc.listar().subscribe(d => {
      this.entrenamientos = d;
      this.fechas = new Map(d.map(t => [t.idEntrenamiento!, `${t.fecha} — ${t.lugar}`] as [string, string]));
      this.pag.recalcular();
    });
  }

  cargar() { this.svc.listar().subscribe(d => this.pag.set(d)); }

  nombreDeportista(id: string): string {
    return this.nombres.get(id) ?? '—';
  }

  infoEntrenamiento(id: string): string {
    return this.fechas.get(id) ?? '—';
  }

  abrirModal() {
    this.editando = false;
    this.form = { estado: 'presente', idDeportista: '', idEntrenamiento: '' };
    this.mostrarModal = true;
  }

  editar(a: Asistencia) {
    this.editando = true;
    this.idEditando = a.idAsistencia;
    this.form = { ...a };
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
    if (confirm('¿Eliminar este registro de asistencia?')) {
      this.svc.eliminar(id).subscribe(() => this.cargar());
    }
  }

  cerrarModal() { this.mostrarModal = false; }
}
