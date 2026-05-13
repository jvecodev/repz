package repz.app.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import repz.app.persistence.entity.Academia;
import repz.app.persistence.entity.Personal;
import repz.app.persistence.entity.User;
import repz.app.persistence.entity.UserRole;
import repz.app.message.Mensagens;
import repz.app.persistence.repository.AcademiaRepository;
import repz.app.persistence.repository.PersonalRepository;
import repz.app.persistence.repository.UserRepository;
import repz.app.service.academia.AcademiaContextService;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;
import static repz.app.unit.UnitTestData.academia;
import static repz.app.unit.UnitTestData.auth;
import static repz.app.unit.UnitTestData.personal;
import static repz.app.unit.UnitTestData.user;

@ExtendWith(MockitoExtension.class)
class AcademiaContextServiceUnitTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private AcademiaRepository academiaRepository;

    @Mock
    private PersonalRepository personalRepository;

    @Mock
    private Mensagens mensagens;

    @InjectMocks
    private AcademiaContextService service;

    @Test
    void adminPodeResolverQualquerAcademiaExistenteOuNenhuma() {
        User admin = user(1L, UserRole.ADMIN);
        when(userRepository.findByEmail(admin.getEmail())).thenReturn(Optional.of(admin));
        when(academiaRepository.existsById(99L)).thenReturn(true);

        assertThat(service.resolveOptional(auth(admin.getEmail()), 99L)).isEqualTo(99L);
        assertThat(service.resolveOptional(auth(admin.getEmail()), null)).isNull();
    }

    @Test
    void gerenteFicaRestritoAoProprioContexto() {
        User academiaUser = user(2L, UserRole.GERENTE);
        Academia academia = academia(10L, academiaUser);
        when(userRepository.findByEmail(academiaUser.getEmail())).thenReturn(Optional.of(academiaUser));
        when(academiaRepository.findByResponsibleUserId(academiaUser.getId())).thenReturn(List.of(academia));

        assertThat(service.resolveRequired(auth(academiaUser.getEmail()), null)).isEqualTo(10L);
        assertThrows(AccessDeniedException.class, () -> service.resolveRequired(auth(academiaUser.getEmail()), 11L));
    }

    @Test
    void personalFicaRestritoAAcademiaVinculada() {
        User academiaUser = user(2L, UserRole.GERENTE);
        Academia academia = academia(10L, academiaUser);
        User personalUser = user(3L, UserRole.PERSONAL);
        Personal personal = personal(30L, personalUser, academia);
        when(userRepository.findByEmail(personalUser.getEmail())).thenReturn(Optional.of(personalUser));
        when(personalRepository.findByUserId(personalUser.getId())).thenReturn(Optional.of(personal));

        assertThat(service.resolveRequired(auth(personalUser.getEmail()), 10L)).isEqualTo(10L);
        assertThrows(AccessDeniedException.class, () -> service.resolveRequired(auth(personalUser.getEmail()), 99L));
    }

    @Test
    void alunoSemAcademiaFalhaSeContextoObrigatorio() {
        User aluno = user(4L, UserRole.ALUNO);
        when(userRepository.findByEmail(aluno.getEmail())).thenReturn(Optional.of(aluno));

        assertThrows(IllegalArgumentException.class, () -> service.resolveRequired(auth(aluno.getEmail()), null));
    }
}
