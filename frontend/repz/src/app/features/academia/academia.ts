import { Component } from '@angular/core';
import { AppShell } from '@shared/layout';
import { ButtonModule } from 'primeng/button';
import { CardModule } from 'primeng/card';
import { TagModule } from 'primeng/tag';

@Component({
  selector: 'app-academia',
  imports: [AppShell, ButtonModule, CardModule, TagModule],
  templateUrl: './academia.html',
  styleUrl: './academia.scss',
})
export class Academia {}
