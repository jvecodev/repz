import { Component, computed, inject, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { catchError, forkJoin, of } from 'rxjs';
import {
  AcademiaService,
  AlunoService,
  FrequenciaService,
  PersonalService,
  PlanoService,
  UserService,
} from '@core/services';
import type {
  AcademiaResponse,
  AcademiaUpdateRequest,
  AlunoDetalheResponse,
  PersonalResponse,
  PlanoResponse,
  UserCreateRequest,
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

type AbaCadastro = 'aluno' | 'personal';

function senhaTemporaria(): string {
  return 'Repz' + Math.floor(1000 + Math.random() * 9000);
}

@Component({
  selector: 'app-academia',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    RouterLink,
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
  templateUrl: './academia.html',
  styleUrl: './academia.scss',
})
export class Academia implements OnInit {
  protected readonly userService = inject(UserService);
  private readonly academiaService = inject(AcademiaService);
  private readonly alunoService = inject(AlunoService);
  private readonly personalService = inject(PersonalService);
  private readonly freqService = inject(FrequenciaService);
  private readonly planoService = inject(PlanoService);
  private readonly router = inject(Router);
  private readonly i18n = inject(TranslateService);

  readonly carregando = signal(true);
  readonly erro = signal<string | null>(null);
  readonly aviso = signal<string | null>(null);
  readonly avisoSeverity = signal<'success' | 'error'>('success');

  readonly academia = signal<AcademiaResponse | null>(null);
  readonly alunos = signal<AlunoDetalheResponse[]>([]);
  readonly personais = signal<PersonalResponse[]>([]);
  readonly planos = signal<PlanoResponse[]>([]);
  readonly ausentes = signal(0);
  readonly checkinsMes = signal(0);

  readonly totalAlunos = computed(() => this.alunos().length);
  readonly totalAtivos = computed(() => this.alunos().filter((a) => a.ativo).length);
  readonly totalPersonais = computed(() => this.personais().length);
  readonly personaisAtivos = computed(() => this.personais().filter((p) => p.ativo).length);

  readonly frequenciaMedia = computed(() => {
    const ativos = this.totalAtivos();
    if (ativos === 0) return 0;
    return Math.round((this.checkinsMes() / ativos) * 10) / 10;
  });

  readonly editando = signal(false);
  readonly salvando = signal(false);
  formCnpj = '';
  formNome = '';
  formEndereco = '';
  formResponsavel = '';
  formEmail = '';
  formTelefone = '';

  readonly abaCadastro = signal<AbaCadastro>('aluno');
  readonly cadastrando = signal(false);
  cadNome = '';
  cadEmail = '';
  cadEspecialidade = '';
  cadPlanoId: number | null = null;

  ngOnInit(): void {
    this.userService.carregarNomeLogado();
    this.carregar();
  }

  private carregar(): void {
    this.carregando.set(true);
    this.erro.set(null);
    forkJoin({
      academia: this.academiaService.minhaAcademia().pipe(catchError(() => of(null))),
      alunos: this.alunoService.listar().pipe(catchError(() => of([] as AlunoDetalheResponse[]))),
      personais: this.personalService
        .listar()
        .pipe(catchError(() => of([] as PersonalResponse[]))),
      planos: this.planoService.listar().pipe(catchError(() => of([] as PlanoResponse[]))),
      inativos: this.freqService.alunosInativos().pipe(catchError(() => of([]))),
    }).subscribe(({ academia, alunos, personais, planos, inativos }) => {
      if (!academia) {
        this.carregando.set(false);
        this.erro.set(this.i18n.instant('ACADEMIA.DASH.LOAD_ERROR'));
        return;
      }
      this.academia.set(academia);
      this.alunos.set(alunos);
      this.personais.set(personais);
      this.planos.set(planos);
      this.ausentes.set(inativos.filter((i) => (i.diasSemTreino ?? 0) >= 14).length);
      const primeiroPlano = planos.find((p) => p.ativo) ?? planos[0];
      if (primeiroPlano) this.cadPlanoId = primeiroPlano.id;
      this.carregarFrequenciaMes(academia.id);
      this.carregando.set(false);
    });
  }

  private carregarFrequenciaMes(academiaId: number): void {
    const now = new Date();
    const inicio = new Date(now.getFullYear(), now.getMonth(), 1, 0, 0, 0);
    const fim = new Date(now.getFullYear(), now.getMonth() + 1, 0, 23, 59, 59);
    this.freqService
      .listarPeriodo(inicio, fim, academiaId)
      .pipe(catchError(() => of([])))
      .subscribe((checkins) => this.checkinsMes.set(checkins.length));
  }

  abrirEdicao(): void {
    const a = this.academia();
    if (!a) return;
    this.formCnpj = a.cnpj ?? '';
    this.formNome = a.name ?? '';
    this.formEndereco = a.address ?? '';
    this.formResponsavel = a.responsible ?? '';
    this.formEmail = a.email ?? '';
    this.formTelefone = a.phone ?? '';
    this.aviso.set(null);
    this.editando.set(true);
  }

  fecharEdicao(): void {
    this.editando.set(false);
  }

  salvarAcademia(): void {
    const a = this.academia();
    if (!a || this.salvando()) return;
    if (
      !this.formCnpj.trim() ||
      !this.formNome.trim() ||
      !this.formEndereco.trim() ||
      !this.formResponsavel.trim()
    ) {
      this.avisoSeverity.set('error');
      this.aviso.set(this.i18n.instant('ACADEMIA.DASH.REQUIRED_FIELDS'));
      return;
    }
    this.salvando.set(true);
    const req: AcademiaUpdateRequest = {
      cnpj: this.formCnpj.trim(),
      name: this.formNome.trim(),
      address: this.formEndereco.trim(),
      responsible: this.formResponsavel.trim(),
      email: this.formEmail.trim() || undefined,
      phone: this.formTelefone.trim() || undefined,
    };
    this.academiaService.atualizarMinhaAcademia(req).subscribe({
      next: () => {
        this.academia.update((x) =>
          x ? { ...x, cnpj: req.cnpj, name: req.name, address: req.address, responsible: req.responsible, email: req.email, phone: req.phone } : x,
        );
        this.salvando.set(false);
        this.editando.set(false);
        this.flash('success', this.i18n.instant('ACADEMIA.DASH.GYM_UPDATED'));
      },
      error: (err) => {
        this.salvando.set(false);
        this.avisoSeverity.set('error');
        this.aviso.set(err?.error?.message ?? this.i18n.instant('ACADEMIA.DASH.UPDATE_ERROR'));
      },
    });
  }

  trocarAba(a: AbaCadastro): void {
    this.abaCadastro.set(a);
    this.aviso.set(null);
  }

  cadastrar(): void {
    const academia = this.academia();
    if (!academia || this.cadastrando()) return;
    if (!this.cadNome.trim() || !this.cadEmail.trim()) {
      this.avisoSeverity.set('error');
      this.aviso.set(this.i18n.instant('PROFILE.NAME_EMAIL_REQUIRED'));
      return;
    }
    if (this.abaCadastro() === 'aluno' && !this.cadPlanoId) {
      this.avisoSeverity.set('error');
      this.aviso.set(this.i18n.instant('ACADEMIA.DASH.SELECT_PLAN'));
      return;
    }

    this.cadastrando.set(true);
    const senha = senhaTemporaria();

    const req: UserCreateRequest = {
      name: this.cadNome.trim(),
      email: this.cadEmail.trim(),
      password: senha,
      role: this.abaCadastro() === 'aluno' ? 'ALUNO' : 'PERSONAL',
      academiaId: academia.id,
      planoId: this.abaCadastro() === 'aluno' ? this.cadPlanoId! : undefined,
    };

    this.userService.criar(req).subscribe({
      next: () => {
        const label = this.i18n.instant(this.abaCadastro() === 'aluno' ? 'ROLES.STUDENT' : 'ROLES.TRAINER');
        this.flash(
          'success',
          this.i18n.instant('ACADEMIA.DASH.REGISTERED_TEMP_PWD', { role: label, nome: req.name, senha }),
        );
        this.cadNome = '';
        this.cadEmail = '';
        this.cadEspecialidade = '';
        this.cadastrando.set(false);
        this.carregar();
      },
      error: (err) => {
        this.cadastrando.set(false);
        this.avisoSeverity.set('error');
        this.aviso.set(err?.error?.message ?? this.i18n.instant('ACADEMIA.DASH.REGISTER_ERROR'));
      },
    });
  }

  private flash(severity: 'success' | 'error', msg: string): void {
    this.avisoSeverity.set(severity);
    this.aviso.set(msg);
    setTimeout(() => this.aviso.set(null), 6000);
  }

  inicial(nome: string): string {
    return (nome.trim()[0] ?? 'P').toUpperCase();
  }

  irParaPersonais(): void {
    this.router.navigate(['/academia/personais']);
  }
}
