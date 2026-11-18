import { NgModule } from '@angular/core';
import { BrowserModule } from '@angular/platform-browser';
import { HTTP_INTERCEPTORS, HttpClientModule } from '@angular/common/http';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';
import { RouterModule, Routes } from '@angular/router';

import { AppComponent } from './app.component';
import { NavbarComponent } from './components/navbar/navbar.component';
import { DashboardComponent } from './components/dashboard/dashboard.component';
import { EscuelasComponent } from './components/escuelas/escuelas.component';
import { EntrenadoresComponent } from './components/entrenadores/entrenadores.component';
import { DeportistasComponent } from './components/deportistas/deportistas.component';
import { EntrenamientosComponent } from './components/entrenamientos/entrenamientos.component';
import { AsistenciasComponent } from './components/asistencias/asistencias.component';
import { MaterialesComponent } from './components/materiales/materiales.component';
import { AuthGuard } from './guards/auth.guard';
import { AdminGuard } from './guards/admin.guard';
import { PaginacionComponent } from './components/paginacion/paginacion.component';
import { PersonalGuard } from './guards/personal.guard';
import { CredentialsInterceptor } from './interceptors/credentials.interceptor';
import { JwtInterceptor } from './interceptors/jwt.interceptor';
import { ErrorInterceptor } from './interceptors/error.interceptor';
import { ToastComponent } from './components/toast/toast.component';
import { MiPanelComponent } from './components/mi-panel/mi-panel.component';
import { MisEntrenamientosComponent } from './components/mis-entrenamientos/mis-entrenamientos.component';
import { MiAsistenciaComponent } from './components/mi-asistencia/mi-asistencia.component';
import { MisEstadisticasComponent } from './components/mis-estadisticas/mis-estadisticas.component';
import { BuzonComponent } from './components/buzon/buzon.component';
import { ReportesComponent } from './components/reportes/reportes.component';
import { DeportistaGuard, InicioGuard } from './guards/rol.guard';

const routes: Routes = [
  {
    path: '',
    canActivate: [AuthGuard],
    children: [
      { path: '', redirectTo: 'dashboard', pathMatch: 'full' },
      { path: 'dashboard', component: DashboardComponent, canActivate: [InicioGuard] },
      { path: 'mi-panel', component: MiPanelComponent, canActivate: [DeportistaGuard] },
      { path: 'mis-entrenamientos', component: MisEntrenamientosComponent, canActivate: [DeportistaGuard] },
      { path: 'mi-asistencia', component: MiAsistenciaComponent, canActivate: [DeportistaGuard] },
      { path: 'mis-estadisticas', component: MisEstadisticasComponent, canActivate: [DeportistaGuard] },
      { path: 'buzon', component: BuzonComponent, canActivate: [DeportistaGuard] },
      { path: 'reportes', component: ReportesComponent, canActivate: [PersonalGuard] },
      { path: 'escuelas', component: EscuelasComponent, canActivate: [AdminGuard] },
      { path: 'entrenadores', component: EntrenadoresComponent, canActivate: [AdminGuard] },
      { path: 'deportistas', component: DeportistasComponent, canActivate: [PersonalGuard] },
      { path: 'materiales', component: MaterialesComponent, canActivate: [PersonalGuard] },
      { path: 'entrenamientos', component: EntrenamientosComponent, canActivate: [PersonalGuard] },
      { path: 'asistencias', component: AsistenciasComponent, canActivate: [PersonalGuard] },
    ],
  },
];

@NgModule({
  declarations: [
    PaginacionComponent,
    AppComponent,
    NavbarComponent,
    DashboardComponent,
    EscuelasComponent,
    EntrenadoresComponent,
    DeportistasComponent,
    EntrenamientosComponent,
    AsistenciasComponent,
    MaterialesComponent,
    ToastComponent,
    MiPanelComponent,
    MisEntrenamientosComponent,
    MiAsistenciaComponent,
    MisEstadisticasComponent,
    BuzonComponent,
    ReportesComponent,
  ],
  imports: [
    BrowserModule,
    HttpClientModule,
    FormsModule,
    ReactiveFormsModule,
    RouterModule.forRoot(routes),
  ],
  providers: [
    { provide: HTTP_INTERCEPTORS, useClass: CredentialsInterceptor, multi: true },
    { provide: HTTP_INTERCEPTORS, useClass: JwtInterceptor, multi: true },
    { provide: HTTP_INTERCEPTORS, useClass: ErrorInterceptor, multi: true },
  ],
  bootstrap: [AppComponent],
})
export class AppModule {}
