package repz.app.service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import repz.app.dto.request.PersonalCreateRequest;
import repz.app.dto.request.PersonalUpdateRequest;
import repz.app.persistence.entity.Aluno;
import repz.app.persistence.entity.Plano;
import repz.app.persistence.entity.UserRole;
import repz.app.persistence.repository.AlunoRepository;
import repz.app.persistence.repository.PlanoRepository;
import repz.app.service.personal.PersonalService;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Transactional
class PersonalServiceTestIntegration extends ServiceIntegrationSupport {

    @Autowired
    private PersonalService personalService;

    @Autowired
    private AlunoRepository alunoRepository;

    @Autowired
    private PlanoRepository planoRepository;

    @Test
    void adminCriaEListaPersonalComFiltroDeAcademia() {
        var admin = criarUsuario(UserRole.ADMIN, "admin-personal");
        var academiaUser = criarUsuario(UserRole.ACADEMIA, "academia-personal");
        var academia = criarAcademia(academiaUser, "personal");
        var personalUser = criarUsuario(UserRole.PERSONAL, "personal-novo");

        var created = personalService.criar(
                new PersonalCreateRequest(personalUser.getId(), academia.getId(), "Funcional"),
                null,
                autenticar(admin));

        assertThat(created.getAcademiaId()).isEqualTo(academia.getId());
        assertThat(created.getEspecialidade()).isEqualTo("Funcional");
        assertThat(personalService.findAll(academia.getId(), autenticar(admin)))
                .extracting("id")
                .contains(created.getId());
    }

    @Test
    void academiaCriaSomenteNaPropriaUnidade() {
        var academiaUser = criarUsuario(UserRole.ACADEMIA, "academia-dona");
        var outraAcademiaUser = criarUsuario(UserRole.ACADEMIA, "academia-outra");
        var academia = criarAcademia(academiaUser, "dona");
        var outraAcademia = criarAcademia(outraAcademiaUser, "outra");
        var personalUser = criarUsuario(UserRole.PERSONAL, "personal-bloqueado");

        assertThatThrownBy(() -> personalService.criar(
                new PersonalCreateRequest(personalUser.getId(), outraAcademia.getId(), "Cross"),
                outraAcademia.getId(),
                autenticar(academiaUser)))
                .isInstanceOf(RuntimeException.class);

        var created = personalService.criar(
                new PersonalCreateRequest(personalUser.getId(), academia.getId(), "Cross"),
                academia.getId(),
                autenticar(academiaUser));
        assertThat(created.getAcademiaId()).isEqualTo(academia.getId());
    }

    @Test
    void atualizarAtivarEDesativarPersonal() {
        var admin = criarUsuario(UserRole.ADMIN, "admin-edita-personal");
        var academia = criarAcademia(admin, "edita-personal");
        var personal = criarPersonal(criarUsuario(UserRole.PERSONAL, "personal-edita"), academia);

        var updated = personalService.atualizar(
                personal.getId(),
                new PersonalUpdateRequest("Pilates", false),
                academia.getId(),
                autenticar(admin));

        assertThat(updated.getEspecialidade()).isEqualTo("Pilates");
        assertThat(updated.getAtivo()).isFalse();
        assertThat(personalService.ativar(personal.getId(), academia.getId(), autenticar(admin)).getAtivo()).isTrue();
        assertThat(personalService.desativar(personal.getId(), academia.getId(), autenticar(admin)).getAtivo()).isFalse();
    }

    @Test
    void personalConsultaEAtualizaMeuPerfilEListaAlunos() {
        var academiaUser = criarUsuario(UserRole.ACADEMIA, "academia-perfil");
        var academia = criarAcademia(academiaUser, "perfil");
        var personalUser = criarUsuario(UserRole.PERSONAL, "personal-perfil");
        var personal = criarPersonal(personalUser, academia);
        var alunoUser = criarUsuario(UserRole.USUARIO, "aluno-perfil");
        var plano = Plano.builder()
                .nome("Mensal")
                .duracaoDias(30)
                .valor(BigDecimal.valueOf(99.90))
                .ativo(true)
                .academia(academia)
                .build();
        planoRepository.saveAndFlush(plano);

        Aluno aluno = new Aluno();
        aluno.setUsuario(alunoUser);
        aluno.setAcademia(academia);
        aluno.setPersonal(personal);
        aluno.setPlano(plano);
        aluno.setDataInicio(LocalDate.now());
        aluno.setAtivo(true);
        alunoRepository.saveAndFlush(aluno);

        var perfil = personalService.obterMeuPerfil(autenticar(personalUser));
        assertThat(perfil.getUserId()).isEqualTo(personalUser.getId());

        var atualizado = personalService.atualizarMeuPerfil(
                new PersonalUpdateRequest("Hipertrofia", null),
                autenticar(personalUser));
        assertThat(atualizado.getEspecialidade()).isEqualTo("Hipertrofia");

        var alunos = personalService.obterMeusAlunos(autenticar(personalUser));
        assertThat(alunos.getAlunos()).extracting("id").contains(alunoUser.getId());
    }
}
