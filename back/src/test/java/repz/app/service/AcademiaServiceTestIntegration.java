package repz.app.service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import repz.app.dto.request.AcademiaCreateRequest;
import repz.app.dto.request.AcademiaUpdateRequest;
import repz.app.persistence.entity.UserRole;
import repz.app.service.academia.AcademiaService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Transactional
class AcademiaServiceTestIntegration extends ServiceIntegrationSupport {

    @Autowired
    private AcademiaService academiaService;

    @Test
    void criarListarBuscarEAlterarStatus() {
        var created = academiaService.criar(new AcademiaCreateRequest(
                "12345678000190",
                "Repz Academy",
                "Rua Principal, 10",
                "Eduardo",
                "contato@academy.com",
                "11999999999"));

        assertThat(created.getId()).isNotNull();
        assertThat(created.getActive()).isTrue();
        assertThat(academiaService.findById(created.getId()).getName()).isEqualTo("Repz Academy");
        assertThat(academiaService.findAll()).extracting("id").contains(created.getId());

        assertThat(academiaService.desativar(created.getId()).getActive()).isFalse();
        assertThat(academiaService.ativar(created.getId()).getActive()).isTrue();
    }

    @Test
    void criarRejeitaCnpjDuplicado() {
        var dto = new AcademiaCreateRequest(
                "22345678000190",
                "Academia A",
                "Rua A",
                "Responsável A",
                "a@repz.com",
                "1111");
        academiaService.criar(dto);

        assertThatThrownBy(() -> academiaService.criar(dto))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void atualizarRejeitaCnpjDeOutraAcademia() {
        var a = academiaService.criar(new AcademiaCreateRequest(
                "32345678000190", "Academia A", "Rua A", "Resp A", "a@repz.com", "1111"));
        academiaService.criar(new AcademiaCreateRequest(
                "42345678000190", "Academia B", "Rua B", "Resp B", "b@repz.com", "2222"));

        assertThatThrownBy(() -> academiaService.atualizar(a.getId(), new AcademiaUpdateRequest(
                "42345678000190", "Academia A2", "Rua A2", "Resp A2", "a2@repz.com", "3333")))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void obterEAtualizarMinhaAcademia() {
        var responsavel = criarUsuario(UserRole.GERENTE, "responsavel-academia");
        var academia = criarAcademia(responsavel, "minha");

        var minha = academiaService.obterMinha(responsavel);
        assertThat(minha.getId()).isEqualTo(academia.getId());

        var atualizada = academiaService.atualizarMinha(responsavel, new AcademiaUpdateRequest(
                academia.getCnpj(),
                "Minha Academia Atualizada",
                "Rua Nova, 200",
                "Responsável Novo",
                "novo@repz.com",
                "11888888888"));

        assertThat(atualizada.getName()).isEqualTo("Minha Academia Atualizada");
        assertThatThrownBy(() -> academiaService.atualizarMinha(responsavel, new AcademiaUpdateRequest(
                "99999999000199", "Inválida", "Rua", "Resp", "x@repz.com", "1")))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void dashboardCalculaIndicadores() {
        var user = criarUsuario(UserRole.GERENTE, "dashboard");
        var academiaAtiva = criarAcademia(user, "ativa");
        academiaAtiva.setTotalStudents(10);
        academiaAtiva.setTotalInstructors(2);
        academiaRepository.saveAndFlush(academiaAtiva);

        var academiaInativa = criarAcademia(user, "inativa");
        academiaInativa.setActive(false);
        academiaInativa.setTotalStudents(5);
        academiaInativa.setTotalInstructors(1);
        academiaRepository.saveAndFlush(academiaInativa);

        var dashboard = academiaService.obterDashboard();

        assertThat(dashboard.getTotalAcademies()).isGreaterThanOrEqualTo(2);
        assertThat(dashboard.getTotalStudents()).isGreaterThanOrEqualTo(15);
        assertThat(dashboard.getTotalInstructors()).isGreaterThanOrEqualTo(3);
        assertThat(dashboard.getTotalInactiveAcademies()).isGreaterThanOrEqualTo(1);
    }
}
