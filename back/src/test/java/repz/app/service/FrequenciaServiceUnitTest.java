package repz.app.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import repz.app.dto.request.FrequenciaCreateRequest;
import repz.app.dto.response.AlunoInativoResponse;
import repz.app.dto.response.FrequenciaRelatorioResponse;
import repz.app.persistence.entity.Academia;
import repz.app.persistence.entity.Frequencia;
import repz.app.persistence.entity.Personal;
import repz.app.persistence.entity.User;
import repz.app.persistence.entity.UserRole;
import repz.app.persistence.repository.AcademiaRepository;
import repz.app.persistence.repository.FrequenciaRepository;
import repz.app.persistence.repository.PersonalRepository;
import repz.app.persistence.repository.UserRepository;
import repz.app.message.Mensagens;
import repz.app.service.academia.AcademiaContextService;
import repz.app.service.frequencia.FrequenciaServiceImpl;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static repz.app.unit.UnitTestData.academia;
import static repz.app.unit.UnitTestData.auth;
import static repz.app.unit.UnitTestData.frequencia;
import static repz.app.unit.UnitTestData.personal;
import static repz.app.unit.UnitTestData.user;

@ExtendWith(MockitoExtension.class)
class FrequenciaServiceUnitTest {

    @Mock
    private FrequenciaRepository frequenciaRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private AcademiaRepository academiaRepository;

    @Mock
    private PersonalRepository personalRepository;

    @Mock
    private AcademiaContextService academiaContextService;

    @Mock
    private Mensagens mensagens;

    @InjectMocks
    private FrequenciaServiceImpl service;

    @Test
    void usuarioRegistraCheckInSomenteParaSiMesmo() {
        User aluno = user(10L, UserRole.ALUNO);
        User academiaUser = user(20L, UserRole.GERENTE);
        Academia academia = academia(30L, academiaUser);
        LocalDateTime dataHora = LocalDateTime.of(2026, 5, 2, 10, 0);
        FrequenciaCreateRequest request = new FrequenciaCreateRequest(aluno.getId(), academia.getId(), null, dataHora);

        when(userRepository.findByEmail(aluno.getEmail())).thenReturn(Optional.of(aluno));
        when(academiaContextService.resolveRequired(auth(aluno.getEmail()), academia.getId())).thenReturn(academia.getId());
        when(userRepository.findById(Math.toIntExact(aluno.getId()))).thenReturn(Optional.of(aluno));
        when(academiaRepository.findById(academia.getId())).thenReturn(Optional.of(academia));
        when(frequenciaRepository.save(any(Frequencia.class))).thenAnswer(invocation -> {
            Frequencia frequencia = invocation.getArgument(0);
            frequencia.setId(1L);
            return frequencia;
        });

        var response = service.criar(request, null, auth(aluno.getEmail()));

        assertThat(response.getAlunoId()).isEqualTo(aluno.getId());
        assertThat(response.getAcademiaId()).isEqualTo(academia.getId());
        ArgumentCaptor<Frequencia> captor = ArgumentCaptor.forClass(Frequencia.class);
        verify(frequenciaRepository).save(captor.capture());
        assertThat(captor.getValue().getDataHora()).isEqualTo(dataHora);
    }

    @Test
    void usuarioNaoRegistraCheckInParaOutroAluno() {
        User aluno = user(10L, UserRole.ALUNO);
        FrequenciaCreateRequest request = new FrequenciaCreateRequest(99L, 30L, null, null);
        when(userRepository.findByEmail(aluno.getEmail())).thenReturn(Optional.of(aluno));
        when(academiaContextService.resolveRequired(auth(aluno.getEmail()), 30L)).thenReturn(30L);

        assertThrows(RuntimeException.class, () -> service.criar(request, null, auth(aluno.getEmail())));
    }

    @Test
    void personalPodeRegistrarCheckInDeAluno() {
        User personalUser = user(11L, UserRole.PERSONAL);
        User aluno = user(12L, UserRole.ALUNO);
        User academiaUser = user(20L, UserRole.GERENTE);
        Academia academia = academia(30L, academiaUser);
        Personal personal = personal(40L, personalUser, academia);
        FrequenciaCreateRequest request = new FrequenciaCreateRequest(aluno.getId(), academia.getId(), personal.getId(), null);

        when(userRepository.findByEmail(personalUser.getEmail())).thenReturn(Optional.of(personalUser));
        when(academiaContextService.resolveRequired(auth(personalUser.getEmail()), academia.getId())).thenReturn(academia.getId());
        when(userRepository.findById(Math.toIntExact(aluno.getId()))).thenReturn(Optional.of(aluno));
        when(academiaRepository.findById(academia.getId())).thenReturn(Optional.of(academia));
        when(personalRepository.findById(personal.getId())).thenReturn(Optional.of(personal));
        when(frequenciaRepository.save(any(Frequencia.class))).thenAnswer(invocation -> {
            Frequencia frequencia = invocation.getArgument(0);
            frequencia.setId(2L);
            return frequencia;
        });

        var response = service.criar(request, null, auth(personalUser.getEmail()));

        assertThat(response.getRegistradoPorId()).isEqualTo(personalUser.getId());
        verify(frequenciaRepository).save(any(Frequencia.class));
    }

    @Test
    void academiaNaoPodeRegistrarCheckIn() {
        User academiaUser = user(20L, UserRole.GERENTE);
        when(userRepository.findByEmail(academiaUser.getEmail())).thenReturn(Optional.of(academiaUser));
        when(academiaContextService.resolveRequired(auth(academiaUser.getEmail()), 30L)).thenReturn(30L);

        assertThrows(RuntimeException.class, () ->
                service.criar(new FrequenciaCreateRequest(10L, 30L, null, null), null, auth(academiaUser.getEmail())));
    }

    @Test
    void relatorioPermiteAcademiaOuAdminERejeitaUsuario() {
        User academiaUser = user(20L, UserRole.GERENTE);
        User aluno = user(10L, UserRole.ALUNO);
        Academia academia = academia(30L, academiaUser);
        LocalDateTime inicio = LocalDateTime.now().minusDays(7);
        LocalDateTime fim = LocalDateTime.now();

        when(userRepository.findByEmail(academiaUser.getEmail())).thenReturn(Optional.of(academiaUser));
        when(academiaContextService.resolveRequired(auth(academiaUser), academia.getId())).thenReturn(academia.getId());
        when(frequenciaRepository.relatorioFrequencia(academia.getId(), inicio, fim))
                .thenReturn(List.<Object[]>of(new Object[]{aluno.getName(), 1L}));

        FrequenciaRelatorioResponse response = service.obterRelatorio(academia.getId(), inicio, fim, auth(academiaUser));

        assertThat(response.getTotalFrequencias()).isEqualTo(1);

        when(userRepository.findByEmail(aluno.getEmail())).thenReturn(Optional.of(aluno));
        assertThrows(RuntimeException.class, () -> service.obterRelatorio(academia.getId(), inicio, fim, auth(aluno)));
    }

    @Test
    void alunosInativosRetornaSomenteQuemNaoTreinaHaMaisDeSeteDias() {
        User academiaUser = user(20L, UserRole.GERENTE);
        Academia academia = academia(30L, academiaUser);
        User alunoInativo = user(10L, UserRole.ALUNO);

        when(academiaContextService.resolveRequired(auth(academiaUser), academia.getId())).thenReturn(academia.getId());
        when(academiaRepository.findById(academia.getId())).thenReturn(Optional.of(academia));
        when(frequenciaRepository.alunosInativos(academia.getId(), 7)).thenReturn(List.<Object[]>of(
                new Object[]{alunoInativo.getId(), alunoInativo.getName(), alunoInativo.getEmail(), 10L, false}
        ));

        List<AlunoInativoResponse> response = service.obterAlunosInativos(academia.getId(), auth(academiaUser));

        assertThat(response).extracting(AlunoInativoResponse::getAlunoId).containsExactly(alunoInativo.getId());
    }
}
