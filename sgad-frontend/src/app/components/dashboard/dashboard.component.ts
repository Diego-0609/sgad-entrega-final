import { Component, OnInit } from '@angular/core';
import { EscuelaService, DeportistaService, EntrenadorService, EntrenamientoService, MaterialService } from '../../services/api.service';
import { AuthService } from '../../services/auth.service';

@Component({
  selector: 'app-dashboard',
  template: `
    <div class="dashboard">
      <h1 class="page-title">Panel Principal</h1>
      <p class="subtitle">Sistema de Gestión y Administración Deportiva — Cartagena</p>

      <div class="cards-grid">
        <div class="card" routerLink="/escuelas" *ngIf="esAdmin">
          <div class="card-icon">🏫</div>
          <div class="card-info">
            <span class="card-num">{{ totalEscuelas }}</span>
            <span class="card-label">Escuelas</span>
          </div>
        </div>
        <div class="card" routerLink="/entrenadores" *ngIf="esAdmin">
          <div class="card-icon">🧑‍🏫</div>
          <div class="card-info">
            <span class="card-num">{{ totalEntrenadores }}</span>
            <span class="card-label">Entrenadores</span>
          </div>
        </div>
        <div class="card" routerLink="/deportistas" *ngIf="esPersonal">
          <div class="card-icon">🏃</div>
          <div class="card-info">
            <span class="card-num">{{ totalDeportistas }}</span>
            <span class="card-label">Deportistas</span>
          </div>
        </div>
        <div class="card" routerLink="/entrenamientos">
          <div class="card-icon">📅</div>
          <div class="card-info">
            <span class="card-num">{{ totalEntrenamientos }}</span>
            <span class="card-label">Entrenamientos</span>
          </div>
        </div>
        <div class="card" routerLink="/materiales" *ngIf="esPersonal">
          <div class="card-icon">🎽</div>
          <div class="card-info">
            <span class="card-num">{{ totalMateriales }}</span>
            <span class="card-label">Materiales</span>
          </div>
        </div>
      </div>
    </div>
  `,
  styles: [`
    .dashboard { padding: 8px 0; }
    .page-title { font-size: 28px; font-weight: 800; color: #fff; margin: 0; }
    .subtitle { color: #999; margin: 4px 0 32px; }
    .cards-grid {
      display: grid;
      grid-template-columns: repeat(auto-fill, minmax(200px, 1fr));
      gap: 20px;
    }
    .card {
      background: #161616;
      border: 1px solid #2a2a2a;
      border-radius: 12px;
      padding: 24px;
      display: flex;
      align-items: center;
      gap: 16px;
      cursor: pointer;
      transition: transform 0.2s, border-color 0.2s;
      border-left: 4px solid #a3e635;
    }
    .card:hover { transform: translateY(-3px); border-color: #a3e635; }
    .card-icon { font-size: 36px; }
    .card-num { display: block; font-size: 32px; font-weight: 800; color: #fff; line-height: 1; }
    .card-label { font-size: 13px; color: #999; margin-top: 4px; display: block; }
  `]
})
export class DashboardComponent implements OnInit {
  totalEscuelas = 0;
  totalEntrenadores = 0;
  totalDeportistas = 0;
  totalEntrenamientos = 0;
  totalMateriales = 0;

  constructor(
    private escuelaService: EscuelaService,
    private entrenadorService: EntrenadorService,
    private deportistaService: DeportistaService,
    private entrenamientoService: EntrenamientoService,
    private materialService: MaterialService,
    private authService: AuthService
  ) {}

  get esAdmin(): boolean {
    return this.authService.esAdmin();
  }

  get esPersonal(): boolean {
    return this.authService.esPersonal();
  }

  ngOnInit() {
    this.escuelaService.listar().subscribe(d => this.totalEscuelas = d.length);
    this.entrenadorService.listar().subscribe(d => this.totalEntrenadores = d.length);
    this.deportistaService.listar().subscribe(d => this.totalDeportistas = d.length);
    this.entrenamientoService.listar().subscribe(d => this.totalEntrenamientos = d.length);
    // Materiales solo lo pueden leer ADMIN y ENTRENADOR (el DEPORTISTA recibiría 403)
    if (this.esPersonal) {
      this.materialService.listar().subscribe(d => this.totalMateriales = d.length);
    }
  }
}
