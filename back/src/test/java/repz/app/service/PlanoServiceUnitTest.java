package repz.app.service;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.server.ResponseStatusException;
import repz.app.dto.request.PlanoPostRequest;
import repz.app.dto.request.PlanoPutRequest;
import repz.app.dto.response.PlanoResponse;
import repz.app.message.Mensagens;
import repz.app.persistence.entity.Academia;
import repz.app.persistence.entity.Plano;
import repz.app.persistence.entity.User;
import repz.app.persistence.entity.UserRole;
import repz.app.persistence.repository.AcademiaRepository;
import repz.app.persistence.repository.PlanoRepository;
import repz.app.service.academia.AcademiaContextService;
import repz.app.service.plano.PlanoServiceImpl;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static repz.app.unit.UnitTestData.academia;
import static repz.app.unit.UnitTestData.auth;
import static repz.app.unit.UnitTestData.user;

@ExtendWith(MockitoExtension.class)
class PlanoServiceUnitTest {

    @Mock
    private PlanoRepository planoRepository;

    @Mock
    private AcademiaRepository academiaRepository;

    @Mock
    private AcademiaContextService academiaContextService;

    @Mock
    private Mensagens mensagens;

    @InjectMocks
    private PlanoServiceImpl service;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    // ─── criar ───────────────────────────────────────────────────────────────

    @Test
    void criarPlanoSempreVinculaNaAcademiaLogada() {
        User academiaUser = user(1L, UserRole.GERENTE);
        Academia academia = academia(10L, academiaUser);
        var auth = auth(academiaUser);
        when(academiaContextService.resolveRequired(auth, null)).thenReturn(academia.getId());
        when(academiaRepository.findById(academia.getId())).thenReturn(Optional.of(academia));

        service.criar(new PlanoPostRequest("Mensal", 30, BigDecimal.valueOf(99.90)), null, auth);

        ArgumentCaptor<Plano> captor = ArgumentCaptor.forClass(Plano.class);
        verify(planoRepository).save(captor.capture());
        assertThat(captor.getValue().getAcademia()).isSameAs(academia);
        assertThat(captor.getValue().getAtivo()).isTrue();
        assertThat(captor.getValue().getNome()).isEqualTo("Mensal");
        assertThat(captor.getValue().getDuracaoDias()).isEqualTo(30);
    }

    @Test
    void criarPlanoLancaExcecaoQuandoAcademiaNaoExiste() {
        User academiaUser = user(1L, UserRole.GERENTE);
        var auth = auth(academiaUser);
        when(academiaContextService.resolveRequired(auth, null)).thenReturn(99L);
        when(academiaRepository.findById(99L)).thenReturn(Optional.empty());
        when(mensagens.get(any())).thenReturn("academia nao encontrada");

        assertThrows(ResponseStatusException.class,
                () -> service.criar(new PlanoPostRequest("Mensal", 30, BigDecimal.valueOf(99.90)), null, auth));
    }

    // ─── findAll ──────────────────────────────────────────────────────────────

    @Test
    void findAllRetornaPlanosDaAcademia() {
        User academiaUser = user(1L, UserRole.GERENTE);
        Academia academia = academia(10L, academiaUser);
        Plano plano = criarPlano(1, "Mensal", academia, true);
        var auth = auth(academiaUser);

        when(academiaContextService.resolveOptional(auth, null)).thenReturn(academia.getId());
        when(planoRepository.findByAcademiaId(academia.getId())).thenReturn(List.of(plano));

        List<PlanoResponse> result = service.findAll(null, auth);
        assertThat(result).hasSize(1);
        assertThat(result.get(0).nome()).isEqualTo("Mensal");
    }

    @Test
    void findAllAdminSemContextoRetornaTodos() {
        User admin = user(1L, UserRole.ADMIN);
        Academia academia = academia(10L, admin);
        Plano p1 = criarPlano(1, "Mensal", academia, true);
        Plano p2 = criarPlano(2, "Trimestral", academia, true);
        var auth = auth(admin);

        when(academiaContextService.resolveOptional(auth, null)).thenReturn(null);
        when(planoRepository.findAll()).thenReturn(List.of(p1, p2));

        List<PlanoResponse> result = service.findAll(null, auth);
        assertThat(result).hasSize(2);
    }

    // ─── findById ─────────────────────────────────────────────────────────────

    @Test
    void findByIdRetornaPlanoNoEscopo() {
        User academiaUser = user(1L, UserRole.GERENTE);
        Academia academia = academia(10L, academiaUser);
        Plano plano = criarPlano(5, "Mensal", academia, true);
        var auth = auth(academiaUser);

        when(academiaContextService.resolveOptional(auth, null)).thenReturn(academia.getId());
        when(planoRepository.findByIdAndAcademiaId(5, academia.getId())).thenReturn(Optional.of(plano));

        PlanoResponse result = service.findById(5, null, auth);
        assertThat(result.nome()).isEqualTo("Mensal");
    }

    @Test
    void findByIdLancaExcecaoQuandoPlanoNaoExisteNoEscopo() {
        User academiaUser = user(1L, UserRole.GERENTE);
        Academia academia = academia(10L, academiaUser);
        var auth = auth(academiaUser);

        when(academiaContextService.resolveOptional(auth, null)).thenReturn(academia.getId());
        when(planoRepository.findByIdAndAcademiaId(99, academia.getId())).thenReturn(Optional.empty());
        when(mensagens.get(any())).thenReturn("plano nao encontrado");

        assertThrows(ResponseStatusException.class, () -> service.findById(99, null, auth));
    }

    // ─── atualizar ────────────────────────────────────────────────────────────

    @Test
    void atualizarPlanoRespeitaEscopoDaAcademiaLogada() {
        User academiaUser = user(1L, UserRole.GERENTE);
        Academia academia = academia(10L, academiaUser);
        Plano plano = criarPlano(5, "Mensal", academia, true);
        var auth = auth(academiaUser);

        when(academiaContextService.resolveOptional(auth, null)).thenReturn(academia.getId());
        when(planoRepository.findByIdAndAcademiaId(5, academia.getId())).thenReturn(Optional.of(plano));

        service.atualizar(5, new PlanoPutRequest("Trimestral", 90, BigDecimal.valueOf(249.90)), null, auth);

        assertThat(plano.getNome()).isEqualTo("Trimestral");
        assertThat(plano.getDuracaoDias()).isEqualTo(90);
        assertThat(plano.getValor()).isEqualByComparingTo("249.90");
        verify(planoRepository).save(plano);
    }

    // ─── ativar / desativar ───────────────────────────────────────────────────

    @Test
    void ativarPlanoMudaStatusParaTrue() {
        User academiaUser = user(1L, UserRole.GERENTE);
        Academia academia = academia(10L, academiaUser);
        Plano plano = criarPlano(5, "Mensal", academia, false);
        var auth = auth(academiaUser);

        when(academiaContextService.resolveOptional(auth, null)).thenReturn(academia.getId());
        when(planoRepository.findByIdAndAcademiaId(5, academia.getId())).thenReturn(Optional.of(plano));

        service.ativar(5, null, auth);

        assertThat(plano.getAtivo()).isTrue();
        verify(planoRepository).save(plano);
    }

    @Test
    void desativarPlanoMudaStatusParaFalse() {
        User academiaUser = user(1L, UserRole.GERENTE);
        Academia academia = academia(10L, academiaUser);
        Plano plano = criarPlano(5, "Mensal", academia, true);
        var auth = auth(academiaUser);

        when(academiaContextService.resolveOptional(auth, null)).thenReturn(academia.getId());
        when(planoRepository.findByIdAndAcademiaId(5, academia.getId())).thenReturn(Optional.of(plano));

        service.desativar(5, null, auth);

        assertThat(plano.getAtivo()).isFalse();
        verify(planoRepository).save(plano);
    }

    @Test
    void desativarPlanoInexistenteLancaExcecao() {
        User academiaUser = user(1L, UserRole.GERENTE);
        Academia academia = academia(10L, academiaUser);
        var auth = auth(academiaUser);

        when(academiaContextService.resolveOptional(auth, null)).thenReturn(academia.getId());
        when(planoRepository.findByIdAndAcademiaId(99, academia.getId())).thenReturn(Optional.empty());
        when(mensagens.get(any())).thenReturn("plano nao encontrado");

        assertThrows(ResponseStatusException.class, () -> service.desativar(99, null, auth));
    }

    // ─── helpers ─────────────────────────────────────────────────────────────

    private Plano criarPlano(Integer id, String nome, Academia academia, boolean ativo) {
        return Plano.builder()
                .id(id)
                .nome(nome)
                .duracaoDias(30)
                .valor(BigDecimal.valueOf(99.90))
                .ativo(ativo)
                .academia(academia)
                .build();
    }
}
