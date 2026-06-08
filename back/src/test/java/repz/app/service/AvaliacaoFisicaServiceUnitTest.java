package repz.app.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import repz.app.dto.request.AvaliacaoFisicaCreateRequest;
import repz.app.persistence.entity.Academia;
import repz.app.persistence.entity.AvaliacaoFisica;
import repz.app.persistence.entity.Personal;
import repz.app.persistence.entity.User;
import repz.app.persistence.entity.UserRole;
import repz.app.persistence.repository.AvaliacaoFisicaRepository;
import repz.app.persistence.repository.PersonalRepository;
import repz.app.persistence.repository.UserRepository;
import repz.app.message.Mensagens;
import repz.app.service.academia.AcademiaContextService;
import repz.app.service.avaliacaoFisica.AvaliacaoFisicaServiceImpl;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static repz.app.unit.UnitTestData.academia;
import static repz.app.unit.UnitTestData.auth;
import static repz.app.unit.UnitTestData.avaliacao;
import static repz.app.unit.UnitTestData.personal;
import static repz.app.unit.UnitTestData.user;

@ExtendWith(MockitoExtension.class)
class AvaliacaoFisicaServiceUnitTest {

    @Mock
    private AvaliacaoFisicaRepository avaliacaoFisicaRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private PersonalRepository personalRepository;

    @Mock
    private AcademiaContextService academiaContextService;

    @Mock
    private Mensagens mensagens;

    @InjectMocks
    private AvaliacaoFisicaServiceImpl service;

    @Test
    void personalCriaAvaliacaoCalculandoImc() {
        User academiaUser = user(1L, UserRole.GERENTE);
        Academia academia = academia(10L, academiaUser);
        User personalUser = user(2L, UserRole.PERSONAL);
        Personal personal = personal(20L, personalUser, academia);
        User aluno = user(3L, UserRole.ALUNO);
        AvaliacaoFisicaCreateRequest request = new AvaliacaoFisicaCreateRequest(aluno.getId(), 80.0, 180.0, null, null, null, null, null, null);

        when(userRepository.findByEmail(personalUser.getEmail())).thenReturn(Optional.of(personalUser));
        when(personalRepository.findAll()).thenReturn(List.of(personal));
        when(userRepository.findById(aluno.getId())).thenReturn(Optional.of(aluno));
        when(avaliacaoFisicaRepository.save(any(AvaliacaoFisica.class))).thenAnswer(invocation -> {
            AvaliacaoFisica avaliacao = invocation.getArgument(0);
            avaliacao.setId(1L);
            return avaliacao;
        });

        var response = service.criar(request, auth(personalUser.getEmail()));

        assertThat(response.getAlunoId()).isEqualTo(aluno.getId());
        assertThat(response.getAcademiaId()).isEqualTo(academia.getId());
        assertThat(response.getImc()).isCloseTo(24.69, org.assertj.core.data.Offset.offset(0.01));

        ArgumentCaptor<AvaliacaoFisica> captor = ArgumentCaptor.forClass(AvaliacaoFisica.class);
        verify(avaliacaoFisicaRepository).save(captor.capture());
        assertThat(captor.getValue().getPersonal()).isSameAs(personal);
        assertThat(captor.getValue().getAcademia()).isSameAs(academia);
    }

    @Test
    void usuarioNaoPodeCriarAvaliacaoFisica() {
        User aluno = user(3L, UserRole.ALUNO);
        when(userRepository.findByEmail(aluno.getEmail())).thenReturn(Optional.of(aluno));

        assertThrows(RuntimeException.class, () ->
                service.criar(new AvaliacaoFisicaCreateRequest(aluno.getId(), 80.0, 180.0, null, null, null, null, null, null), auth(aluno.getEmail())));
    }

    @Test
    void usuarioVisualizaSomenteAvaliacoesProprias() {
        User aluno = user(3L, UserRole.ALUNO);
        when(userRepository.findByEmail(aluno.getEmail())).thenReturn(Optional.of(aluno));

        assertThrows(RuntimeException.class, () -> service.findAll(99L, auth(aluno.getEmail())));
    }

    @Test
    void graficoRetornaEvolucaoDoAluno() {
        User aluno = user(3L, UserRole.ALUNO);
        User academiaUser = user(1L, UserRole.GERENTE);
        Academia academia = academia(10L, academiaUser);
        User personalUser = user(2L, UserRole.PERSONAL);
        Personal personal = personal(20L, personalUser, academia);
        AvaliacaoFisica avaliacao = avaliacao(1L, aluno, academia, personal);

        when(userRepository.findByEmail(aluno.getEmail())).thenReturn(Optional.of(aluno));
        when(userRepository.findById(aluno.getId())).thenReturn(Optional.of(aluno));
        when(avaliacaoFisicaRepository.findByAluno_IdOrderByDataAvaliacaoAsc(aluno.getId())).thenReturn(List.of(avaliacao));

        var response = service.obterGrafico(aluno.getId(), auth(aluno.getEmail()));

        assertThat(response.getAlunoId()).isEqualTo(aluno.getId());
        assertThat(response.getDados()).hasSize(1);
        assertThat(response.getDados().getFirst().getImc()).isEqualTo(avaliacao.getImc());
    }

    @Test
    void academiaListaAvaliacoesDaPropriaUnidade() {
        User academiaUser = user(1L, UserRole.GERENTE);
        Academia academia = academia(10L, academiaUser);
        User personalUser = user(2L, UserRole.PERSONAL);
        User aluno = user(3L, UserRole.ALUNO);
        Personal personal = personal(20L, personalUser, academia);

        when(userRepository.findByEmail(academiaUser.getEmail())).thenReturn(Optional.of(academiaUser));
        when(academiaContextService.resolveOptional(auth(academiaUser.getEmail()), academia.getId())).thenReturn(academia.getId());
        when(avaliacaoFisicaRepository.findByAcademia_Id(academia.getId()))
                .thenReturn(List.of(avaliacao(1L, aluno, academia, personal)));

        var response = service.obterDaUnidade(academia.getId(), auth(academiaUser.getEmail()));

        assertThat(response).hasSize(1);
        assertThat(response.getFirst().getAlunoId()).isEqualTo(aluno.getId());
        verify(avaliacaoFisicaRepository).findByAcademia_Id(academia.getId());
    }

    @Test
    void desativarAtualizaStatusDaAvaliacao() {
        AvaliacaoFisica avaliacao = new AvaliacaoFisica();
        avaliacao.setId(1L);
        avaliacao.setAtivo(true);
        when(avaliacaoFisicaRepository.findById(1L)).thenReturn(Optional.of(avaliacao));

        service.desativar(1L);

        assertThat(avaliacao.getAtivo()).isFalse();
        verify(avaliacaoFisicaRepository).save(avaliacao);
    }

    @Test
    void criarAvaliacaoComPersonalSucesso() {
        User academiaUser = user(1L, UserRole.GERENTE);
        Academia academia = academia(10L, academiaUser);
        User personalUser = user(2L, UserRole.PERSONAL);
        Personal personal = personal(20L, personalUser, academia);
        User alunoUser = user(3L, UserRole.ALUNO);

        var req = new AvaliacaoFisicaCreateRequest();
        req.setAlunoId(alunoUser.getId());
        req.setPesoKg(80.0);
        req.setAlturaCm(180.0);

        when(userRepository.findByEmail(personalUser.getEmail())).thenReturn(Optional.of(personalUser));
        when(personalRepository.findAll()).thenReturn(List.of(personal));
        when(userRepository.findById(alunoUser.getId())).thenReturn(Optional.of(alunoUser));
        when(avaliacaoFisicaRepository.save(any())).thenAnswer(inv -> {
            AvaliacaoFisica a = inv.getArgument(0);
            a.setId(1L);
            return a;
        });

        var resp = service.criar(req, auth(personalUser.getEmail()));
        assertThat(resp).isNotNull();
        verify(avaliacaoFisicaRepository).save(any());
    }

    @Test
    void criarAvaliacaoRejeitaParaNaoPersonal() {
        User alunoUser = user(3L, UserRole.ALUNO);
        when(userRepository.findByEmail(alunoUser.getEmail())).thenReturn(Optional.of(alunoUser));
        when(mensagens.get(any())).thenReturn("apenas personal");

        assertThrows(RuntimeException.class, () -> service.criar(new AvaliacaoFisicaCreateRequest(), auth(alunoUser.getEmail())));
    }

    @Test
    void findAllPersonalVerificaVinculo() {
        User academiaUser = user(1L, UserRole.GERENTE);
        Academia academia = academia(10L, academiaUser);
        User personalUser = user(2L, UserRole.PERSONAL);
        Personal personal = personal(20L, personalUser, academia);
        User alunoUser = user(3L, UserRole.ALUNO);
        AvaliacaoFisica av = avaliacao(1L, alunoUser, academia, personal);

        when(userRepository.findByEmail(personalUser.getEmail())).thenReturn(Optional.of(personalUser));
        when(personalRepository.findAll()).thenReturn(List.of(personal));
        when(avaliacaoFisicaRepository.findByAluno_IdOrderByDataAvaliacaoDesc(alunoUser.getId()))
                .thenReturn(List.of(av));

        var result = service.findAll(alunoUser.getId(), auth(personalUser.getEmail()));
        assertThat(result).hasSize(1);
    }

    @Test
    void findAllAlunoVeSomentePropriaAvaliacao() {
        User alunoUser = user(3L, UserRole.ALUNO);
        User academiaUser = user(1L, UserRole.GERENTE);
        Academia academia = academia(10L, academiaUser);
        User personalUser = user(2L, UserRole.PERSONAL);
        Personal personal = personal(20L, personalUser, academia);
        AvaliacaoFisica av = avaliacao(1L, alunoUser, academia, personal);

        when(userRepository.findByEmail(alunoUser.getEmail())).thenReturn(Optional.of(alunoUser));
        when(avaliacaoFisicaRepository.findByAluno_IdOrderByDataAvaliacaoDesc(alunoUser.getId()))
                .thenReturn(List.of(av));

        var result = service.findAll(alunoUser.getId(), auth(alunoUser.getEmail()));
        assertThat(result).hasSize(1);
    }

    @Test
    void ativarAtualizaStatusDaAvaliacao() {
        AvaliacaoFisica avaliacao = new AvaliacaoFisica();
        avaliacao.setId(1L);
        avaliacao.setAtivo(false);
        when(avaliacaoFisicaRepository.findById(1L)).thenReturn(Optional.of(avaliacao));

        service.ativar(1L);

        assertThat(avaliacao.getAtivo()).isTrue();
        verify(avaliacaoFisicaRepository).save(avaliacao);
    }

    @Test
    void findByIdRetornaAvaliacao() {
        User academiaUser = user(1L, UserRole.GERENTE);
        Academia academia = academia(10L, academiaUser);
        User aluno = user(3L, UserRole.ALUNO);
        User personalUser = user(2L, UserRole.PERSONAL);
        Personal personal = personal(20L, personalUser, academia);
        AvaliacaoFisica av = avaliacao(1L, aluno, academia, personal);
        when(avaliacaoFisicaRepository.findById(1L)).thenReturn(Optional.of(av));

        var resp = service.findById(1L);
        assertThat(resp.getId()).isEqualTo(1L);
    }

    @Test
    void adminListaTodasAvaliacoes() {
        User adminUser = user(1L, UserRole.ADMIN);
        User academiaUser = user(2L, UserRole.GERENTE);
        Academia academia = academia(10L, academiaUser);
        User aluno = user(3L, UserRole.ALUNO);
        User personalUser = user(4L, UserRole.PERSONAL);
        Personal personal = personal(20L, personalUser, academia);

        when(userRepository.findByEmail(adminUser.getEmail())).thenReturn(Optional.of(adminUser));
        when(academiaContextService.resolveOptional(auth(adminUser.getEmail()), null)).thenReturn(null);
        when(avaliacaoFisicaRepository.findAll()).thenReturn(List.of(avaliacao(1L, aluno, academia, personal)));

        var resp = service.obterDaUnidade(null, auth(adminUser.getEmail()));
        assertThat(resp).hasSize(1);
    }

    @Test
    void obterGraficoRetornaDados() {
        User personalUser = user(2L, UserRole.PERSONAL);
        User aluno = user(3L, UserRole.ALUNO);
        User academiaUser = user(1L, UserRole.GERENTE);
        Academia academia = academia(10L, academiaUser);
        Personal personal = personal(20L, personalUser, academia);
        AvaliacaoFisica av = avaliacao(1L, aluno, academia, personal);

        when(userRepository.findByEmail(aluno.getEmail())).thenReturn(Optional.of(aluno));
        when(userRepository.findById(aluno.getId())).thenReturn(Optional.of(aluno));
        when(avaliacaoFisicaRepository.findByAluno_IdOrderByDataAvaliacaoAsc(aluno.getId()))
                .thenReturn(List.of(av));

        var resp = service.obterGrafico(aluno.getId(), auth(aluno.getEmail()));
        assertThat(resp).isNotNull();
        assertThat(resp.getAlunoId()).isEqualTo(aluno.getId());
    }
}
