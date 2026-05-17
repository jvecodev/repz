import { Component } from '@angular/core';
import { AppShell } from '@shared/layout';
import { ButtonModule } from 'primeng/button';
import { CardModule } from 'primeng/card';
import { TagModule } from 'primeng/tag';

@Component({
  selector: 'app-personal',
  imports: [AppShell, ButtonModule, CardModule, TagModule],
  templateUrl: './personal.html',
  styleUrl: './personal.scss',
})
export class Personal {}
