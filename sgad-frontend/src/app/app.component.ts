// app.component.ts
import { Component } from '@angular/core';

@Component({
  selector: 'app-root',
  template: `
    <app-navbar></app-navbar>
    <app-toast></app-toast>
    <div class="main-content">
      <router-outlet></router-outlet>
    </div>
  `,
  styles: [`
    .main-content {
      padding: 24px;
      max-width: 1200px;
      margin: 0 auto;
    }
  `]
})
export class AppComponent {}
