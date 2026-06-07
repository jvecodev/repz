package repz.app.persistence.repository;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import repz.app.persistence.entity.Academia;
import repz.app.persistence.entity.AvaliacaoFisica;
import repz.app.persistence.entity.Frequencia;
import repz.app.persistence.entity.Personal;
import repz.app.persistence.entity.Plano;
import repz.app.persistence.entity.User;
import repz.app.persistence.entity.UserRole;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static repz.app.unit.UnitTestData.academia;
import static repz.app.unit.UnitTestData.avaliacao;
import static repz.app.unit.UnitTestData.frequencia;
import static repz.app.unit.UnitTestData.personal;
import static repz.app.unit.UnitTestData.user;

@ExtendWith(MockitoExtension.class)
class RepositoryUnitTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private AcademiaRepository academiaRepository;

    @Mock
    private PersonalRepository personalRepository;

    @Mock
    private FrequenciaRepository frequenciaRepository;

    @Mock
    private AvaliacaoFisicaRepository avaliacaoFisicaRepository;

    @Mock
    private PlanoRepository planoRepository;

    @Test
    void userRepositoryExpoeConsultasDeAutenticacaoEStatus() {
        User aluno = user(1L, UserRole.ALUNO);

        when(userRepository.findByEmail(aluno.getEmail()))
                .thenReturn(Optional.of(aluno));

        when(userRepository.findByIdAndDeletedAtIsNull(1L))
                .thenReturn(Optional.of(aluno));

        when(userRepository.findByActiveAndDeletedAtIsNull(true))
                .thenReturn(List.of(aluno));

        assertThat(userRepository.findByEmail(aluno.getEmail()))
                .contains(aluno);

        assertThat(userRepository.findByIdAndDeletedAtIsNull(1L))
                .contains(aluno);

        assertThat(userRepository.findByActiveAndDeletedAtIsNull(true))
                .containsExactly(aluno);

        verify(userRepository).findByEmail(aluno.getEmail());
    }

    @Test
    void academiaRepositoryExpoeConsultasDeUnidadeEResponsavel() {
        User responsavel = user(2L, UserRole.GERENTE);
        Academia academia = academia(10L, responsavel);

        when(academiaRepository.findByCnpj(academia.getCnpj()))
                .thenReturn(Optional.of(academia));

        when(academiaRepository.findByResponsibleUserId(responsavel.getId()))
                .thenReturn(List.of(academia));

        when(academiaRepository.findByActiveTrue())
                .thenReturn(List.of(academia));

        assertThat(academiaRepository.findByCnpj(academia.getCnpj()))
                .contains(academia);

        assertThat(academiaRepository.findByResponsibleUserId(responsavel.getId()))
                .containsExactly(academia);

        assertThat(academiaRepository.findByActiveTrue())
                .containsExactly(academia);
    }

    @Test
    void personalRepositoryExpoeVinculoComUsuario() {
        User responsavel = user(2L, UserRole.GERENTE);
        Academia academia = academia(10L, responsavel);
        User personalUser = user(3L, UserRole.PERSONAL);
        Personal personal = personal(20L, personalUser, academia);

        when(personalRepository.findByUserId(personalUser.getId()))
                .thenReturn(Optional.of(personal));

        assertThat(personalRepository.findByUserId(personalUser.getId()))
                .contains(personal);
    }

    @Test
    void frequenciaRepositoryExpoeConsultasDePeriodoEHistorico() {
        User responsavel = user(2L, UserRole.GERENTE);
        Academia academia = academia(10L, responsavel);
        User aluno = user(1L, UserRole.ALUNO);

        LocalDateTime inicio = LocalDateTime.now().minusDays(7);
        LocalDateTime fim = LocalDateTime.now();

        Frequencia frequencia = frequencia(
                30L,
                aluno,
                academia,
                null,
                LocalDateTime.now()
        );

        when(frequenciaRepository.findByAlunoIdAndPeriodo(aluno.getId(), inicio, fim))
                .thenReturn(List.of(frequencia));

        when(frequenciaRepository.findByAcademiaIdAndPeriodo(academia.getId(), inicio, fim))
                .thenReturn(List.of(frequencia));

        when(frequenciaRepository.findByAluno_IdOrderByDataHoraDesc(aluno.getId()))
                .thenReturn(List.of(frequencia));

        when(frequenciaRepository.findLatestByAcademia(academia.getId()))
                .thenReturn(frequencia);

        assertThat(frequenciaRepository.findByAlunoIdAndPeriodo(aluno.getId(), inicio, fim))
                .containsExactly(frequencia);

        assertThat(frequenciaRepository.findByAcademiaIdAndPeriodo(academia.getId(), inicio, fim))
                .containsExactly(frequencia);

        assertThat(frequenciaRepository.findByAluno_IdOrderByDataHoraDesc(aluno.getId()))
                .containsExactly(frequencia);

        assertThat(frequenciaRepository.findLatestByAcademia(academia.getId()))
                .isSameAs(frequencia);
    }

    @Test
    void avaliacaoFisicaRepositoryExpoeHistoricoGraficoEUnidade() {
        User responsavel = user(2L, UserRole.GERENTE);
        Academia academia = academia(10L, responsavel);
        User personalUser = user(3L, UserRole.PERSONAL);
        User aluno = user(1L, UserRole.ALUNO);

        AvaliacaoFisica avaliacao = avaliacao(
                40L,
                aluno,
                academia,
                personal(20L, personalUser, academia)
        );

        when(avaliacaoFisicaRepository.findByAluno_IdOrderByDataAvaliacaoDesc(aluno.getId()))
                .thenReturn(List.of(avaliacao));

        when(avaliacaoFisicaRepository.findByAluno_IdOrderByDataAvaliacaoAsc(aluno.getId()))
                .thenReturn(List.of(avaliacao));

        when(avaliacaoFisicaRepository.findByAcademia_Id(academia.getId()))
                .thenReturn(List.of(avaliacao));

        assertThat(avaliacaoFisicaRepository.findByAluno_IdOrderByDataAvaliacaoDesc(aluno.getId()))
                .containsExactly(avaliacao);

        assertThat(avaliacaoFisicaRepository.findByAluno_IdOrderByDataAvaliacaoAsc(aluno.getId()))
                .containsExactly(avaliacao);

        assertThat(avaliacaoFisicaRepository.findByAcademia_Id(academia.getId()))
                .containsExactly(avaliacao);
    }

    @Test
    void planoRepositoryExpoeEscopoPorAcademia() {
        User academiaUser = user(2L, UserRole.GERENTE);
        Academia academia = academia(10L, academiaUser);

        Plano plano = Plano.builder()
                .id(1)
                .nome("Mensal")
                .duracaoDias(30)
                .ativo(true)
                .academia(academia)
                .build();

        when(planoRepository.findByAcademia(academia))
                .thenReturn(List.of(plano));

        when(planoRepository.findByIdAndAcademia(1, academia))
                .thenReturn(Optional.of(plano));

        assertThat(planoRepository.findByAcademia(academia))
                .containsExactly(plano);

        assertThat(planoRepository.findByIdAndAcademia(1, academia))
                .contains(plano);
    }
}