package repz.app.service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import repz.app.dto.request.FrequenciaCreateRequest;
import repz.app.persistence.entity.Frequencia;
import repz.app.persistence.entity.UserRole;
import repz.app.persistence.repository.FrequenciaRepository;
import repz.app.service.frequencia.FrequenciaService;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Transactional
class FrequenciaServiceTestIntegration extends ServiceIntegrationSupport {

    @Autowired
    private FrequenciaService frequenciaService;

    @Autowired
    private FrequenciaRepository frequenciaRepository;

    @Test
    void usuarioRegistraCheckinApenasParaSiMesmo() {
        var academia = criarAcademia(criarUsuario(UserRole.GERENTE, "academia-checkin"), "checkin");
        var aluno = criarUsuario(UserRole.ALUNO, "aluno-checkin");
        var outroAluno = criarUsuario(UserRole.ALUNO, "outro-aluno-checkin");
        var dataHora = LocalDateTime.now().minusHours(1);

        var response = frequenciaService.criar(
                new FrequenciaCreateRequest(aluno.getId(), academia.getId(), null, dataHora),
                null,
                autenticar(aluno));

        assertThat(response.getAlunoId()).isEqualTo(aluno.getId());
        assertThat(response.getAcademiaId()).isEqualTo(academia.getId());

        assertThatThrownBy(() -> frequenciaService.criar(
                new FrequenciaCreateRequest(outroAluno.getId(), academia.getId(), null, dataHora),
                null,
                autenticar(aluno)))
                .isInstanceOf(RuntimeException.class);
    }

    @Test
    void personalRegistraCheckinParaAluno() {
        var academia = criarAcademia(criarUsuario(UserRole.GERENTE, "academia-personal-checkin"), "personal-checkin");
        var personalUser = criarUsuario(UserRole.PERSONAL, "personal-checkin");
        var personal = criarPersonal(personalUser, academia);
        var aluno = criarUsuario(UserRole.ALUNO, "aluno-com-personal");

        var response = frequenciaService.criar(
                new FrequenciaCreateRequest(aluno.getId(), academia.getId(), personal.getId(), LocalDateTime.now()),
                academia.getId(),
                autenticar(personalUser));

        assertThat(response.getRegistradoPorId()).isEqualTo(personalUser.getId());
        assertThat(response.getRegistradoPorNome()).isEqualTo(personalUser.getName());
    }

    @Test
    void filtraHistoricoRelatorioEAlunosInativos() {
        var academiaUser = criarUsuario(UserRole.GERENTE, "academia-relatorio");
        var academia = criarAcademia(academiaUser, "relatorio");
        var alunoAtivo = criarUsuario(UserRole.ALUNO, "aluno-ativo");
        var alunoInativo = criarUsuario(UserRole.ALUNO, "aluno-inativo");
        salvarFrequencia(alunoAtivo, academia, LocalDateTime.now().minusDays(2));
        salvarFrequencia(alunoInativo, academia, LocalDateTime.now().minusDays(10));

        var inicio = LocalDateTime.now().minusDays(20);
        var fim = LocalDateTime.now().plusDays(1);

        assertThat(frequenciaService.filtrarPorAcademiaEPeriodo(academia.getId(), inicio, fim, autenticar(academiaUser)))
                .hasSize(2);
        assertThat(frequenciaService.meuHistorico(alunoAtivo.getId()))
                .extracting("alunoId")
                .containsOnly(alunoAtivo.getId());

        var inativos = frequenciaService.obterAlunosInativos(academia.getId(), autenticar(academiaUser));
        assertThat(inativos).extracting("alunoId").contains(alunoInativo.getId());
        assertThat(inativos).extracting("alunoId").doesNotContain(alunoAtivo.getId());

        var relatorio = frequenciaService.obterRelatorio(academia.getId(), inicio, fim, autenticar(academiaUser));
        assertThat(relatorio.getTotalFrequencias()).isEqualTo(2);
        assertThat(relatorio.getFrequenciaPorAluno()).containsKeys(alunoAtivo.getName(), alunoInativo.getName());
    }

    @Test
    void ativarEDesativarCheckin() {
        var academia = criarAcademia(criarUsuario(UserRole.GERENTE, "academia-status-checkin"), "status-checkin");
        var aluno = criarUsuario(UserRole.ALUNO, "aluno-status-checkin");
        var frequencia = salvarFrequencia(aluno, academia, LocalDateTime.now());

        frequenciaService.desativar(frequencia.getId());
        assertThat(frequenciaRepository.findById(frequencia.getId()).orElseThrow().getAtivo()).isFalse();

        frequenciaService.ativar(frequencia.getId());
        assertThat(frequenciaRepository.findById(frequencia.getId()).orElseThrow().getAtivo()).isTrue();
    }

    private Frequencia salvarFrequencia(repz.app.persistence.entity.User aluno,
                                        repz.app.persistence.entity.Academia academia,
                                        LocalDateTime dataHora) {
        Frequencia frequencia = new Frequencia();
        frequencia.setAluno(aluno);
        frequencia.setAcademia(academia);
        frequencia.setDataHora(dataHora);
        frequencia.setAtivo(true);
        return frequenciaRepository.saveAndFlush(frequencia);
    }
}
