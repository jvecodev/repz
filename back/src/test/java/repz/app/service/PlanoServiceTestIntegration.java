package repz.app.service;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import repz.app.dto.request.PlanoPostRequest;
import repz.app.dto.request.PlanoPutRequest;
import repz.app.persistence.entity.UserRole;
import repz.app.service.plano.PlanoService;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Transactional
class PlanoServiceTestIntegration extends ServiceIntegrationSupport {

    @Autowired
    private PlanoService planoService;

    @AfterEach
    void clearSecurityContext() {
        deslogar();
    }

    @Test
    void criaListaBuscaAtualizaEAlteraStatusDoPlanoDaAcademiaLogada() {
        var academiaUser = criarUsuario(UserRole.ACADEMIA, "academia-plano");
        var academia = criarAcademia(academiaUser, "academia-plano");
        var auth = autenticar(academiaUser);

        planoService.criar(new PlanoPostRequest("Mensal", 30, new BigDecimal("99.90")), null, auth);

        var planos = planoService.findAll(null, auth);
        assertThat(planos).hasSize(1);
        var plano = planos.getFirst();
        assertThat(plano.nome()).isEqualTo("Mensal");
        assertThat(plano.ativo()).isTrue();

        planoService.atualizar(plano.id(), new PlanoPutRequest("Trimestral", 90, new BigDecimal("249.90")), null, auth);
        assertThat(planoService.findById(plano.id(), null, auth).nome()).isEqualTo("Trimestral");

        planoService.desativar(plano.id(), null, auth);
        assertThat(planoService.findById(plano.id(), null, auth).ativo()).isFalse();

        planoService.ativar(plano.id(), null, auth);
        assertThat(planoService.findById(plano.id(), null, auth).ativo()).isTrue();
    }

    @Test
    void planoFicaIsoladoPorAcademiaLogada() {
        var academiaA = criarUsuario(UserRole.ACADEMIA, "academia-plano-a");
        var academiaB = criarUsuario(UserRole.ACADEMIA, "academia-plano-b");
        criarAcademia(academiaA, "academia-plano-a");
        criarAcademia(academiaB, "academia-plano-b");

        var authA = autenticar(academiaA);
        planoService.criar(new PlanoPostRequest("Plano A", 30, new BigDecimal("99.90")), null, authA);
        var planoAId = planoService.findAll(null, authA).getFirst().id();

        var authB = autenticar(academiaB);
        assertThat(planoService.findAll(null, authB)).isEmpty();
        assertThatThrownBy(() -> planoService.findById(planoAId, null, authB))
                .isInstanceOf(org.springframework.web.server.ResponseStatusException.class);
    }
}
