package repz.app.service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import repz.app.dto.request.AlunoCreateRequest;
import repz.app.dto.request.AlunoMeUpdateRequest;
import repz.app.dto.request.AlunoUpdateRequest;
import repz.app.dto.response.AlunoDetalheResponse;
import repz.app.persistence.entity.Academia;
import repz.app.persistence.entity.Personal;
import repz.app.persistence.entity.Plano;
import repz.app.persistence.entity.User;
import repz.app.persistence.entity.UserRole;
import repz.app.persistence.repository.AlunoRepository;
import repz.app.persistence.repository.PlanoRepository;
import repz.app.service.aluno.AlunoService;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Transactional
class AlunoServiceTestIntegration extends ServiceIntegrationSupport {

    @Autowired
    private AlunoService alunoService;

    @Autowired
    private AlunoRepository alunoRepository;

    @Autowired
    private PlanoRepository planoRepository;


    @Test
    void matricularAlunoComSucessoComoAdmin() {
        User adminUser  = criarUsuario(UserRole.ADMIN, "admin-matricula");
        User alunoUser  = criarUsuario(UserRole.ALUNO, "aluno-novo");
        User acadUser   = criarUsuario(UserRole.GERENTE, "acad-responsavel");
        Academia academia = criarAcademia(acadUser, "academia-teste");
        Plano plano     = criarPlano(acadUser, "Mensal");

        AlunoCreateRequest req = new AlunoCreateRequest(alunoUser.getId(), plano.getId(), null, "Emagrecimento");
        AlunoDetalheResponse resp = alunoService.matricular(req, academia.getId(), autenticar(adminUser));

        assertThat(resp.getId()).isNotNull();
        assertThat(resp.getUserId()).isEqualTo(alunoUser.getId());
        assertThat(resp.getAcademiaId()).isEqualTo(academia.getId());
        assertThat(resp.getPlanoId()).isEqualTo(plano.getId());
        assertThat(resp.getObjetivo()).isEqualTo("Emagrecimento");
        assertThat(resp.getAtivo()).isTrue();
    }

    @Test
    void matricularAlunoComPersonalOpcional() {
        User acadUser   = criarUsuario(UserRole.GERENTE, "acad-p");
        User personalUser = criarUsuario(UserRole.PERSONAL, "personal-p");
        User alunoUser  = criarUsuario(UserRole.ALUNO, "aluno-p");
        Academia academia = criarAcademia(acadUser, "academia-personal");
        Personal personal = criarPersonal(personalUser, academia);
        Plano plano = criarPlano(acadUser, "Trimestral");

        AlunoCreateRequest req = new AlunoCreateRequest(alunoUser.getId(), plano.getId(), personal.getId(), null);
        AlunoDetalheResponse resp = alunoService.matricular(req, academia.getId(), autenticar(acadUser));

        assertThat(resp.getPersonalId()).isEqualTo(personal.getId());
        assertThat(resp.getPersonalNome()).isEqualTo(personalUser.getName());
    }

    @Test
    void matricularRejeitaDuplicataParaMesmaAcademia() {
        User acadUser  = criarUsuario(UserRole.GERENTE, "acad-dup");
        User alunoUser = criarUsuario(UserRole.ALUNO, "aluno-dup");
        Academia academia = criarAcademia(acadUser, "academia-dup");
        Plano plano = criarPlano(acadUser, "Mensal-dup");

        AlunoCreateRequest req = new AlunoCreateRequest(alunoUser.getId(), plano.getId(), null, null);
        alunoService.matricular(req, academia.getId(), autenticar(acadUser));

        assertThatThrownBy(() -> alunoService.matricular(req, academia.getId(), autenticar(acadUser)))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("409");
    }

    @Test
    void matricularRejeitaPlanoInexistente() {
        User adminUser = criarUsuario(UserRole.ADMIN, "admin-plano");
        User alunoUser = criarUsuario(UserRole.ALUNO, "aluno-plano");
        User acadUser  = criarUsuario(UserRole.GERENTE, "acad-plano");
        Academia academia = criarAcademia(acadUser, "academia-plano-inv");

        AlunoCreateRequest req = new AlunoCreateRequest(alunoUser.getId(), 999999, null, null);
        assertThatThrownBy(() -> alunoService.matricular(req, academia.getId(), autenticar(adminUser)))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("404");
    }

    @Test
    void matricularRejeitaUsuarioInexistente() {
        User adminUser = criarUsuario(UserRole.ADMIN, "admin-usr");
        User acadUser  = criarUsuario(UserRole.GERENTE, "acad-usr");
        Academia academia = criarAcademia(acadUser, "academia-usr-inv");
        Plano plano = criarPlano(acadUser, "Plano-usr");

        AlunoCreateRequest req = new AlunoCreateRequest(999999L, plano.getId(), null, null);
        assertThatThrownBy(() -> alunoService.matricular(req, academia.getId(), autenticar(adminUser)))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("404");
    }

 

    @Test
    void findAllRetornaSomenteAlunosDaAcademia() {
        User acadUser1 = criarUsuario(UserRole.GERENTE, "acad-list1");
        User acadUser2 = criarUsuario(UserRole.GERENTE, "acad-list2");
        Academia acad1 = criarAcademia(acadUser1, "list1");
        Academia acad2 = criarAcademia(acadUser2, "list2");
        Plano plano1 = criarPlano(acadUser1, "plano-list1");
        Plano plano2 = criarPlano(acadUser2, "plano-list2");

        User aluno1 = criarUsuario(UserRole.ALUNO, "aluno-list1");
        User aluno2 = criarUsuario(UserRole.ALUNO, "aluno-list2");

        alunoService.matricular(new AlunoCreateRequest(aluno1.getId(), plano1.getId(), null, null),
                acad1.getId(), autenticar(acadUser1));
        alunoService.matricular(new AlunoCreateRequest(aluno2.getId(), plano2.getId(), null, null),
                acad2.getId(), autenticar(acadUser2));

        List<AlunoDetalheResponse> resultAcad1 = alunoService.findAll(acad1.getId(), autenticar(acadUser1));
        assertThat(resultAcad1).hasSize(1);
        assertThat(resultAcad1.get(0).getUserId()).isEqualTo(aluno1.getId());
    }

    @Test
    void findAllPersonalVeSomenteSeusAlunos() {
        User acadUser    = criarUsuario(UserRole.GERENTE, "acad-personal-list");
        User personalUser = criarUsuario(UserRole.PERSONAL, "personal-list");
        User aluno1      = criarUsuario(UserRole.ALUNO, "aluno-personal1");
        User aluno2      = criarUsuario(UserRole.ALUNO, "aluno-sem-personal");
        Academia academia = criarAcademia(acadUser, "academia-personal-list");
        Personal personal = criarPersonal(personalUser, academia);
        Plano plano = criarPlano(acadUser, "plano-personal-list");

        alunoService.matricular(new AlunoCreateRequest(aluno1.getId(), plano.getId(), personal.getId(), null),
                academia.getId(), autenticar(acadUser));
        alunoService.matricular(new AlunoCreateRequest(aluno2.getId(), plano.getId(), null, null),
                academia.getId(), autenticar(acadUser));

        List<AlunoDetalheResponse> result = alunoService.findAll(null, autenticar(personalUser));
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getUserId()).isEqualTo(aluno1.getId());
    }

    @Test
    void atualizarAluno() {
        User acadUser  = criarUsuario(UserRole.GERENTE, "acad-upd");
        User alunoUser = criarUsuario(UserRole.ALUNO, "aluno-upd");
        Academia academia = criarAcademia(acadUser, "academia-upd");
        Plano plano1 = criarPlano(acadUser, "Plano-upd1");
        Plano plano2 = criarPlano(acadUser, "Plano-upd2");

        AlunoDetalheResponse criado = alunoService.matricular(
                new AlunoCreateRequest(alunoUser.getId(), plano1.getId(), null, "Hipertrofia"),
                academia.getId(), autenticar(acadUser));

        AlunoDetalheResponse atualizado = alunoService.atualizar(criado.getId(),
                new AlunoUpdateRequest(plano2.getId(), null, "Emagrecimento"),
                academia.getId(), autenticar(acadUser));

        assertThat(atualizado.getPlanoId()).isEqualTo(plano2.getId());
        assertThat(atualizado.getObjetivo()).isEqualTo("Emagrecimento");
    }

    @Test
    void inativarAluno() {
        User acadUser  = criarUsuario(UserRole.GERENTE, "acad-inat");
        User alunoUser = criarUsuario(UserRole.ALUNO, "aluno-inat");
        Academia academia = criarAcademia(acadUser, "academia-inat");
        Plano plano = criarPlano(acadUser, "plano-inat");

        AlunoDetalheResponse criado = alunoService.matricular(
                new AlunoCreateRequest(alunoUser.getId(), plano.getId(), null, null),
                academia.getId(), autenticar(acadUser));

        alunoService.inativar(criado.getId(), academia.getId(), autenticar(acadUser));

        assertThat(alunoRepository.findById(criado.getId()).orElseThrow().getAtivo()).isFalse();
    }



    @Test
    void findByIdPersonalNaoVeAlunoDeOutro() {
        User acadUser    = criarUsuario(UserRole.GERENTE, "acad-rb");
        User p1User      = criarUsuario(UserRole.PERSONAL, "personal-rb1");
        User p2User      = criarUsuario(UserRole.PERSONAL, "personal-rb2");
        User alunoUser   = criarUsuario(UserRole.ALUNO, "aluno-rb");
        Academia academia = criarAcademia(acadUser, "academia-rb");
        Personal p1 = criarPersonal(p1User, academia);
        criarPersonal(p2User, academia);
        Plano plano = criarPlano(acadUser, "plano-rb");

        AlunoDetalheResponse criado = alunoService.matricular(
                new AlunoCreateRequest(alunoUser.getId(), plano.getId(), p1.getId(), null),
                academia.getId(), autenticar(acadUser));

        assertThatThrownBy(() -> alunoService.findById(criado.getId(), autenticar(p2User)))
                .isInstanceOf(AccessDeniedException.class);
    }



    @Test
    void atualizarMeuPerfilAlteraNomeETelefone() {
        User acadUser  = criarUsuario(UserRole.GERENTE, "acad-me");
        User alunoUser = criarUsuario(UserRole.ALUNO, "aluno-me");
        Academia academia = criarAcademia(acadUser, "academia-me");
        Plano plano = criarPlano(acadUser, "plano-me");

        alunoService.matricular(new AlunoCreateRequest(alunoUser.getId(), plano.getId(), null, null),
                academia.getId(), autenticar(acadUser));

        AlunoDetalheResponse resp = alunoService.atualizarMeuPerfil(
                new AlunoMeUpdateRequest("Novo Nome", "11988887777", null, null),
                autenticar(alunoUser));

        assertThat(resp.getNome()).isEqualTo("Novo Nome");
        assertThat(resp.getTelefone()).isEqualTo("11988887777");
    }



    private Plano criarPlano(User acadUser, String nome) {
        Academia academia = academiaRepository.findByResponsibleUserId(acadUser.getId()).getFirst();
        Plano plano = Plano.builder()
                .nome(nome)
                .duracaoDias(30)
                .valor(BigDecimal.valueOf(99.90))
                .ativo(true)
                .academia(academia)
                .build();
        return planoRepository.saveAndFlush(plano);
    }
}
