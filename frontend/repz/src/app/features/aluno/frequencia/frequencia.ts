import { Component } from '@angular/core';
import { AppShell } from '@shared/layout';
import { CardModule } from 'primeng/card';

@Component({
  selector: 'app-frequencia',
  standalone: true,
  imports: [AppShell, CardModule],
  templateUrl: './frequencia.html',
  styleUrl: './frequencia.scss',
})
export class Frequencia {}
