import { Component } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { Avisos } from './shared/aviso';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [RouterOutlet, Avisos],
  template: `<router-outlet /><fp-avisos />`,
})
export class App {}

