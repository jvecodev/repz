import { Component } from '@angular/core';
import { RouterLink } from '@angular/router';
import { AppShell } from '@shared/layout';
import { ButtonModule } from 'primeng/button';
import { CardModule } from 'primeng/card';
import { TagModule } from 'primeng/tag';

@Component({
  selector: 'app-aluno',
  imports: [RouterLink, AppShell, ButtonModule, CardModule, TagModule],
  templateUrl: './aluno.html',
  styleUrl: './aluno.scss',
})
export class Aluno {}
