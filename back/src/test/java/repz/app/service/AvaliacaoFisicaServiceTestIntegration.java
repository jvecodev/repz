package repz.app.service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import repz.app.dto.request.AvaliacaoFisicaCreateRequest;
import repz.app.persistence.entity.AvaliacaoFisica;
import repz.app.persistence.entity.UserRole;
import repz.app.persistence.repository.AvaliacaoFisicaRepository;
import repz.app.service.avaliacaoFisica.AvaliacaoFisicaService;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Transactional
class AvaliacaoFisicaServiceTestIntegration extends ServiceIntegrationSupport {

    @Autowired
    private AvaliacaoFisicaService avaliacaoFisicaService;

    @Autowired
    private AvaliacaoFisicaRepository avaliacaoFisicaRepository;

    @Test
    void personalCriaAvaliacaoCalculandoImc() {
        var academia = criarAcademia(criarUsuario(UserRole.GERENTE, "academia-avaliacao"), "avaliacao");
        var personalUser = criarUsuario(UserRole.PERSONAL, "personal-avaliacao");
        criarPersonal(personalUser, academia);
        var aluno = criarUsuario(UserRole.ALUNO, "aluno-avaliacao");

        var response = avaliacaoFisicaService.criar(
                new AvaliacaoFisicaCreateRequest(aluno.getId(), 80.0, 180.0, 20.0, "Cintura: 80cm", null, null, null, null),
                autenticar(personalUser));

        assertThat(response.getAlunoId()).isEqualTo(aluno.getId());
        assertThat(response.getAcademiaId()).isEqualTo(academia.getId());
        assertThat(response.getImc()).isCloseTo(24.69, withinPercentage(0.5));
    }

    @Test
    void usuarioNaoPodeCriarAvaliacaoENaoAcessaAvaliacaoDeOutroAluno() {
        var aluno = criarUsuario(UserRole.ALUNO, "aluno-negado-avaliacao");
        var outroAluno = criarUsuario(UserRole.ALUNO, "outro-negado-avaliacao");

        assertThatThrownBy(() -> avaliacaoFisicaService.criar(
                new AvaliacaoFisicaCreateRequest(aluno.getId(), 70.0, 170.0, null, null, null, null, null, null),
                autenticar(aluno)))
                .isInstanceOf(RuntimeException.class);

        assertThatThrownBy(() -> avaliacaoFisicaService.findAll(outroAluno.getId(), autenticar(aluno)))
                .isInstanceOf(RuntimeException.class);
        assertThatThrownBy(() -> avaliacaoFisicaService.obterGrafico(outroAluno.getId(), autenticar(aluno)))
                .isInstanceOf(RuntimeException.class);
    }

    @Test
    void historicoGraficoEUnidadeRetornamAvaliacoes() {
        var admin = criarUsuario(UserRole.ADMIN, "admin-avaliacao");
        var academia = criarAcademia(criarUsuario(UserRole.GERENTE, "academia-historico-avaliacao"), "historico-avaliacao");
        var personalUser = criarUsuario(UserRole.PERSONAL, "personal-historico-avaliacao");
        var personal = criarPersonal(personalUser, academia);
        var aluno = criarUsuario(UserRole.ALUNO, "aluno-historico-avaliacao");
        salvarAvaliacao(aluno, personal, academia, 77.0, 175.0, LocalDateTime.now().minusDays(2));
        salvarAvaliacao(aluno, personal, academia, 76.0, 175.0, LocalDateTime.now().minusDays(1));

        assertThat(avaliacaoFisicaService.findAll(aluno.getId(), autenticar(aluno))).hasSize(2);

        var grafico = avaliacaoFisicaService.obterGrafico(aluno.getId(), autenticar(aluno));
        assertThat(grafico.getAlunoId()).isEqualTo(aluno.getId());
        assertThat(grafico.getDados()).hasSize(2);

        var unidade = avaliacaoFisicaService.obterDaUnidade(academia.getId(), autenticar(admin));
        assertThat(unidade).extracting("alunoId").contains(aluno.getId());
    }

    @Test
    void ativarEDesativarAvaliacao() {
        var academia = criarAcademia(criarUsuario(UserRole.GERENTE, "academia-status-avaliacao"), "status-avaliacao");
        var personal = criarPersonal(criarUsuario(UserRole.PERSONAL, "personal-status-avaliacao"), academia);
        var aluno = criarUsuario(UserRole.ALUNO, "aluno-status-avaliacao");
        var avaliacao = salvarAvaliacao(aluno, personal, academia, 70.0, 170.0, LocalDateTime.now());

        avaliacaoFisicaService.desativar(avaliacao.getId());
        assertThat(avaliacaoFisicaRepository.findById(avaliacao.getId()).orElseThrow().getAtivo()).isFalse();

        avaliacaoFisicaService.ativar(avaliacao.getId());
        assertThat(avaliacaoFisicaRepository.findById(avaliacao.getId()).orElseThrow().getAtivo()).isTrue();
    }

    private AvaliacaoFisica salvarAvaliacao(repz.app.persistence.entity.User aluno,
                                            repz.app.persistence.entity.Personal personal,
                                            repz.app.persistence.entity.Academia academia,
                                            Double peso,
                                            Double altura,
                                            LocalDateTime dataAvaliacao) {
        AvaliacaoFisica avaliacao = new AvaliacaoFisica();
        avaliacao.setAluno(aluno);
        avaliacao.setPersonal(personal);
        avaliacao.setAcademia(academia);
        avaliacao.setPesoKg(peso);
        avaliacao.setAlturaCm(altura);
        avaliacao.setImc(peso / Math.pow(altura / 100.0, 2));
        avaliacao.setDataAvaliacao(dataAvaliacao);
        avaliacao.setAtivo(true);
        return avaliacaoFisicaRepository.saveAndFlush(avaliacao);
    }

    private org.assertj.core.data.Offset<Double> withinPercentage(double percentage) {
        return org.assertj.core.data.Offset.offset(percentage);
    }
}
