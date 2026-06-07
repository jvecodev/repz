package repz.app.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import repz.app.dto.request.PersonalCreateRequest;
import repz.app.persistence.entity.Academia;
import repz.app.persistence.entity.Personal;
import repz.app.persistence.entity.User;
import repz.app.persistence.entity.UserRole;
import repz.app.persistence.repository.AcademiaRepository;
import repz.app.persistence.repository.PersonalRepository;
import repz.app.persistence.repository.UserRepository;
import repz.app.message.Mensagens;
import repz.app.service.academia.AcademiaContextService;
import repz.app.service.personal.PersonalServiceImpl;

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
class PersonalServiceUnitTest {

    @Mock
    private PersonalRepository personalRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private AcademiaRepository academiaRepository;

    @Mock
    private AcademiaContextService academiaContextService;

    @Mock
    private Mensagens mensagens;

    @InjectMocks
    private PersonalServiceImpl service;

    @Test
    void adminPodeCriarPersonalParaQualquerAcademia() {
        User admin = user(1L, UserRole.ADMIN);
        User personalUser = user(2L, UserRole.PERSONAL);
        User academiaUser = user(3L, UserRole.GERENTE);
        Academia academia = academia(10L, academiaUser);
        PersonalCreateRequest request = new PersonalCreateRequest(personalUser.getId(), academia.getId(), "Funcional");

        when(userRepository.findByEmail(admin.getEmail())).thenReturn(Optional.of(admin));
        when(academiaContextService.resolveRequired(auth(admin.getEmail()), academia.getId())).thenReturn(academia.getId());
        when(userRepository.findById(personalUser.getId())).thenReturn(Optional.of(personalUser));
        when(academiaRepository.findById(academia.getId())).thenReturn(Optional.of(academia));
        when(personalRepository.save(any(Personal.class))).thenAnswer(invocation -> {
            Personal personal = invocation.getArgument(0);
            personal.setId(99L);
            return personal;
        });

        var response = service.criar(request, null, auth(admin.getEmail()));

        assertThat(response.getUserId()).isEqualTo(personalUser.getId());
        assertThat(response.getAcademiaId()).isEqualTo(academia.getId());
        verify(personalRepository).save(any(Personal.class));
    }

    @Test
    void academiaPodeCriarPersonalApenasNaPropriaAcademia() {
        User academiaUser = user(3L, UserRole.GERENTE);
        User otherAcademiaUser = user(4L, UserRole.GERENTE);
        User personalUser = user(2L, UserRole.PERSONAL);
        Academia outraAcademia = academia(20L, otherAcademiaUser);
        PersonalCreateRequest request = new PersonalCreateRequest(personalUser.getId(), outraAcademia.getId(), "Funcional");

        when(userRepository.findByEmail(academiaUser.getEmail())).thenReturn(Optional.of(academiaUser));
        when(academiaContextService.resolveRequired(auth(academiaUser.getEmail()), outraAcademia.getId())).thenReturn(outraAcademia.getId());
        when(academiaRepository.findById(outraAcademia.getId())).thenReturn(Optional.of(outraAcademia));

        assertThrows(RuntimeException.class, () -> service.criar(request, null, auth(academiaUser.getEmail())));
    }

    @Test
    void usuarioNaoPodeCriarPersonal() {
        User aluno = user(5L, UserRole.ALUNO);
        PersonalCreateRequest request = new PersonalCreateRequest(2L, 10L, "Funcional");
        when(userRepository.findByEmail(aluno.getEmail())).thenReturn(Optional.of(aluno));
        when(academiaContextService.resolveRequired(auth(aluno.getEmail()), 10L)).thenReturn(10L);

        assertThrows(RuntimeException.class, () -> service.criar(request, null, auth(aluno.getEmail())));
    }
}
