import { Component, computed, inject, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import {
  AcademiaService,
  PersonalService,
  UserService,
} from '@core/services';
import type {
  AcademiaResponse,
  PersonalResponse,
  UserCreateRequest,
  UserPutRequest,
} from '@core/services';
import { AppShell } from '@shared/layout';
import { TranslatePipe, TranslateService } from '@ngx-translate/core';
import { ButtonModule } from 'primeng/button';
import { CardModule } from 'primeng/card';
import { DialogModule } from 'primeng/dialog';
import { InputTextModule } from 'primeng/inputtext';
import { MessageModule } from 'primeng/message';
import { ProgressSpinnerModule } from 'primeng/progressspinner';
import { TableModule } from 'primeng/table';
import { TagModule } from 'primeng/tag';

@Component({
  selector: 'app-academia-personais',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    AppShell,
    TranslatePipe,
    ButtonModule,
    CardModule,
    DialogModule,
    InputTextModule,
    MessageModule,
    ProgressSpinnerModule,
    TableModule,
    TagModule,
  ],
  templateUrl: './personais.html',
  styleUrl: './personais.scss',
})
export class AcademiaPersonais implements OnInit {
  protected readonly userService = inject(UserService);
  private readonly personalService = inject(PersonalService);
  private readonly academiaService = inject(AcademiaService);
  private readonly i18n = inject(TranslateService);
  private readonly academia = signal<AcademiaResponse | null>(null);

  readonly carregando = signal(true);
  readonly erro = signal<string | null>(null);
  readonly aviso = signal<string | null>(null);
  readonly avisoSeverity = signal<'success' | 'error'>('success');
  readonly personais = signal<PersonalResponse[]>([]);
  readonly busca = signal('');
  readonly alterandoId = signal<number | null>(null);

  readonly editando = signal<PersonalResponse | null>(null);
  readonly salvandoEdicao = signal(false);
  formNome = '';
  formEmail = '';
  formEspecialidade = '';

  readonly cadastrando = signal(false);
  readonly salvandoCad = signal(false);
  cadNome = '';
  cadEmail = '';
  cadCref = '';
  cadEspecialidade = '';

  readonly personaisFiltrados = computed(() => {
    const q = this.busca().trim().toLowerCase();
    if (!q) return this.personais();
    return this.personais().filter(
      (p) =>
        p.userName.toLowerCase().includes(q) ||
        p.email.toLowerCase().includes(q) ||
        (p.especialidade ?? '').toLowerCase().includes(q),
    );
  });

  ngOnInit(): void {
    this.userService.carregarNomeLogado();
    this.academiaService.minhaAcademia().subscribe({
      next: (a) => this.academia.set(a),
      error: () => {},
    });
    this.carregar();
  }

  abrirCadastro(): void {
    this.cadNome = '';
    this.cadEmail = '';
    this.cadCref = '';
    this.cadEspecialidade = '';
    this.aviso.set(null);
    this.cadastrando.set(true);
  }

  fecharCadastro(): void {
    this.cadastrando.set(false);
  }

  salvarCadastro(): void {
    const academia = this.academia();
    if (!academia || this.salvandoCad()) return;
    if (!this.cadNome.trim() || !this.cadEmail.trim()) {
      this.avisoSeverity.set('error');
      this.aviso.set(this.i18n.instant('PROFILE.NAME_EMAIL_REQUIRED'));
      return;
    }

    this.salvandoCad.set(true);
    const senha = 'Repz' + Math.floor(1000 + Math.random() * 9000);
    const req: UserCreateRequest = {
      name: this.cadNome.trim(),
      email: this.cadEmail.trim(),
      password: senha,
      role: 'PERSONAL',
      academiaId: academia.id,
    };

    this.userService.criar(req).subscribe({
      next: () => {
        const especialidade = this.cadEspecialidade.trim();
        const aplicarEspecialidade = () => {
          if (!especialidade) return;
          this.personalService.listar().subscribe({
            next: (lista) => {
              const novo = lista.find((p) => p.email === req.email);
              if (novo) {
                this.personalService
                  .atualizar(novo.id, { especialidade, ativo: true })
                  .subscribe({ error: () => {} });
              }
            },
            error: () => {},
          });
        };
        aplicarEspecialidade();

        this.salvandoCad.set(false);
        this.cadastrando.set(false);
        this.avisoSeverity.set('success');
        this.aviso.set(
          this.i18n.instant('ACADEMIA.TRAINERS.TRAINER_REGISTERED_PWD', { nome: req.name, senha }) +
            (this.cadCref
              ? this.i18n.instant('ACADEMIA.TRAINERS.CREF_SUFFIX', { cref: this.cadCref })
              : ''),
        );
        setTimeout(() => this.aviso.set(null), 6000);
        this.carregar();
      },
      error: (err) => {
        this.salvandoCad.set(false);
        this.avisoSeverity.set('error');
        this.aviso.set(err?.error?.message ?? this.i18n.instant('ACADEMIA.TRAINERS.REGISTER_ERROR'));
      },
    });
  }

  private carregar(): void {
    this.carregando.set(true);
    this.erro.set(null);
    this.personalService.listar().subscribe({
      next: (lista) => {
        this.personais.set(lista);
        this.carregando.set(false);
      },
      error: () => {
        this.carregando.set(false);
        this.erro.set(this.i18n.instant('ACADEMIA.TRAINERS.LOAD_ERROR'));
      },
    });
  }

  abrirEdicao(p: PersonalResponse): void {
    this.formNome = p.userName;
    this.formEmail = p.email;
    this.formEspecialidade = p.especialidade ?? '';
    this.aviso.set(null);
    this.editando.set(p);
  }

  fecharEdicao(): void {
    this.editando.set(null);
  }

  salvarEdicao(): void {
    const p = this.editando();
    if (!p || this.salvandoEdicao()) return;
    if (!this.formNome.trim() || !this.formEmail.trim()) {
      this.avisoSeverity.set('error');
      this.aviso.set(this.i18n.instant('PROFILE.NAME_EMAIL_REQUIRED'));
      return;
    }
    this.salvandoEdicao.set(true);

    const userReq: UserPutRequest = {
      name: this.formNome.trim(),
      email: this.formEmail.trim(),
      role: 'PERSONAL',
      active: p.ativo,
    };

    this.userService.atualizar(p.userId, userReq).subscribe({
      next: () => {
        this.personalService
          .atualizar(p.id, { especialidade: this.formEspecialidade.trim() || '—', ativo: p.ativo })
          .subscribe({
            next: () => {
              const nome = this.formNome.trim();
              const email = this.formEmail.trim();
              const esp = this.formEspecialidade.trim();
              this.personais.update((lista) =>
                lista.map((x) =>
                  x.id === p.id ? { ...x, userName: nome, email, especialidade: esp || x.especialidade } : x,
                ),
              );
              this.salvandoEdicao.set(false);
              this.editando.set(null);
              this.avisoSeverity.set('success');
              this.aviso.set(this.i18n.instant('ACADEMIA.TRAINERS.TRAINER_UPDATED', { nome }));
              setTimeout(() => this.aviso.set(null), 3500);
            },
            error: () => {
              this.salvandoEdicao.set(false);
              this.editando.set(null);
              this.avisoSeverity.set('success');
              this.aviso.set(this.i18n.instant('ACADEMIA.TRAINERS.TRAINER_UPDATED', { nome: this.formNome.trim() }));
              setTimeout(() => this.aviso.set(null), 3500);
            },
          });
      },
      error: (err) => {
        this.salvandoEdicao.set(false);
        this.avisoSeverity.set('error');
        this.aviso.set(err?.error?.message ?? this.i18n.instant('ACADEMIA.TRAINERS.UPDATE_ERROR'));
      },
    });
  }

  alternarStatus(p: PersonalResponse): void {
    if (this.alterandoId() !== null) return;
    this.alterandoId.set(p.id);
    this.aviso.set(null);

    this.personalService
      .atualizar(p.id, { especialidade: p.especialidade ?? '—', ativo: !p.ativo })
      .subscribe({
        next: () => {
          this.personais.update((lista) =>
            lista.map((x) => (x.id === p.id ? { ...x, ativo: !x.ativo } : x)),
          );
          this.alterandoId.set(null);
          this.avisoSeverity.set('success');
          this.aviso.set(
            this.i18n.instant(
              p.ativo ? 'ACADEMIA.TRAINERS.TRAINER_DEACTIVATED' : 'ACADEMIA.TRAINERS.TRAINER_ACTIVATED',
              { nome: p.userName },
            ),
          );
          setTimeout(() => this.aviso.set(null), 3500);
        },
        error: (err) => {
          this.alterandoId.set(null);
          this.avisoSeverity.set('error');
          this.aviso.set(err?.error?.message ?? this.i18n.instant('ACADEMIA.TRAINERS.STATUS_ERROR'));
        },
      });
  }
}
