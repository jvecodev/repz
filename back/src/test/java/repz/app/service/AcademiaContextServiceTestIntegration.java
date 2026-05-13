package repz.app.service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.transaction.annotation.Transactional;
import repz.app.persistence.entity.UserRole;
import repz.app.service.academia.AcademiaContextService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Transactional
class AcademiaContextServiceTestIntegration extends ServiceIntegrationSupport {

    @Autowired
    private AcademiaContextService academiaContextService;

    @Test
    void adminPodeResolverAcademiaInformadaOuNula() {
        var admin = criarUsuario(UserRole.ADMIN, "admin-contexto");
        var academia = criarAcademia(admin, "admin-contexto");

        assertThat(academiaContextService.resolveOptional(autenticar(admin), academia.getId()))
                .isEqualTo(academia.getId());
        assertThat(academiaContextService.resolveOptional(autenticar(admin), null)).isNull();
    }

    @Test
    void academiaResolveApenasSuaPropriaAcademia() {
        var academiaUser = criarUsuario(UserRole.GERENTE, "academia-contexto");
        var outraAcademiaUser = criarUsuario(UserRole.GERENTE, "outra-contexto");
        var academia = criarAcademia(academiaUser, "propria");
        var outraAcademia = criarAcademia(outraAcademiaUser, "outra");

        assertThat(academiaContextService.resolveOptional(autenticar(academiaUser), null))
                .isEqualTo(academia.getId());
        assertThatThrownBy(() -> academiaContextService.resolveOptional(autenticar(academiaUser), outraAcademia.getId()))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void personalResolveAcademiaDoSeuVinculo() {
        var academiaUser = criarUsuario(UserRole.GERENTE, "academia-personal-contexto");
        var personalUser = criarUsuario(UserRole.PERSONAL, "personal-contexto");
        var academia = criarAcademia(academiaUser, "contexto-personal");
        criarPersonal(personalUser, academia);

        assertThat(academiaContextService.resolveRequired(autenticar(personalUser), null))
                .isEqualTo(academia.getId());
    }

    @Test
    void usuarioSemAcademiaExigeHeaderQuandoObrigatorio() {
        var aluno = criarUsuario(UserRole.ALUNO, "usuario-contexto");

        assertThatThrownBy(() -> academiaContextService.resolveRequired(autenticar(aluno), null))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
