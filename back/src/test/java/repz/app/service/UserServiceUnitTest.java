package repz.app.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;
import repz.app.dto.request.AdminCreateRequest;
import repz.app.dto.request.UserCreateRequest;
import repz.app.message.Mensagens;
import repz.app.persistence.entity.Academia;
import repz.app.persistence.entity.Aluno;
import repz.app.persistence.entity.Plano;
import repz.app.persistence.entity.User;
import repz.app.persistence.entity.UserRole;
import repz.app.persistence.mapper.UserMapper;
import repz.app.persistence.repository.AcademiaRepository;
import repz.app.persistence.repository.AlunoRepository;
import repz.app.persistence.repository.PersonalRepository;
import repz.app.persistence.repository.PlanoRepository;
import repz.app.persistence.repository.UserRepository;
import repz.app.service.user.UserDetailsServiceImpl;
import repz.app.service.user.UserServiceImpl;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static repz.app.unit.UnitTestData.academia;
import static repz.app.unit.UnitTestData.user;

@ExtendWith(MockitoExtension.class)
class UserServiceUnitTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private AcademiaRepository academiaRepository;

    @Mock
    private PlanoRepository planoRepository;

    @Mock
    private PersonalRepository personalRepository;

    @Mock
    private AlunoRepository alunoRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private UserMapper userMapper;

    @Mock
    private Mensagens mensagens;

    @InjectMocks
    private UserServiceImpl userService;

    @Test
    void criarUsuarioRejeitaRoleAdmin() {
        var dto = new UserCreateRequest("Admin", "admin@repz.com", "123456", UserRole.ADMIN, 1L, null);

        assertThrows(AccessDeniedException.class, () -> userService.criarUsuario(dto, null));
        verify(userRepository, never()).save(any());
    }

    @Test
    void criarUsuarioAluno() {
        User gerenteUser = user(1L, UserRole.GERENTE);
        Academia academia = academia(10L, gerenteUser);
        Plano plano = new Plano();
        plano.setId(5);
        var dto = new UserCreateRequest("Aluno Novo", "aluno@repz.com", "123456",
                UserRole.ALUNO, academia.getId(), plano.getId());

        when(userRepository.findByEmail("aluno@repz.com")).thenReturn(Optional.empty());
        when(academiaRepository.findById(academia.getId())).thenReturn(Optional.of(academia));
        when(passwordEncoder.encode("123456")).thenReturn("hash");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> {
            User u = inv.getArgument(0);
            u.setId(99L);
            return u;
        });
        when(planoRepository.findByIdAndAcademiaId(plano.getId(), academia.getId())).thenReturn(Optional.of(plano));
        when(alunoRepository.existsByUsuarioIdAndAcademiaId(99L, academia.getId())).thenReturn(false);

        userService.criarUsuario(dto, null);

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        assertThat(userCaptor.getValue().getRole()).isEqualTo(UserRole.ALUNO);
        assertThat(userCaptor.getValue().getPassword()).isEqualTo("hash");
        verify(alunoRepository).save(any(Aluno.class));
    }

    @Test
    void criarUsuarioPersonal() {
        User gerenteUser = user(1L, UserRole.GERENTE);
        Academia academia = academia(10L, gerenteUser);
        var dto = new UserCreateRequest("Personal Novo", "personal@repz.com", "123456",
                UserRole.PERSONAL, academia.getId(), null);

        when(userRepository.findByEmail("personal@repz.com")).thenReturn(Optional.empty());
        when(academiaRepository.findById(academia.getId())).thenReturn(Optional.of(academia));
        when(passwordEncoder.encode("123456")).thenReturn("hash");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        userService.criarUsuario(dto, null);

        verify(personalRepository).save(any());
    }

    @Test
    void criarUsuarioRejeitaEmailDuplicado() {
        when(userRepository.findByEmail("aluno@repz.com")).thenReturn(Optional.of(user(1L, UserRole.ALUNO)));

        var dto = new UserCreateRequest("Aluno", "aluno@repz.com", "123456", UserRole.ALUNO, 10L, 5);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> userService.criarUsuario(dto, null));
        assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        verify(userRepository, never()).save(any());
    }

    @Test
    void criarAdmin() {
        when(userRepository.findByEmail("admin@repz.com")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("123456")).thenReturn("hash");

        userService.criarAdmin(new AdminCreateRequest("Admin", "admin@repz.com", "123456"));

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        assertThat(captor.getValue().getRole()).isEqualTo(UserRole.ADMIN);
        assertThat(captor.getValue().getPassword()).isEqualTo("hash");
    }

    @Test
    void criarAdminRejeitaEmailDuplicado() {
        when(userRepository.findByEmail("admin@repz.com")).thenReturn(Optional.of(user(1L, UserRole.ADMIN)));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> userService.criarAdmin(new AdminCreateRequest("Admin", "admin@repz.com", "123456")));
        assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        verify(userRepository, never()).save(any());
    }

    @Test
    void loadUserByUsernameRetornaUsuarioOuErro() {
        User aluno = user(40L, UserRole.ALUNO);
        UserDetailsServiceImpl service = new UserDetailsServiceImpl(userRepository, mensagens);
        when(userRepository.findByEmail("aluno@repz.com")).thenReturn(Optional.of(aluno));

        assertThat(service.loadUserByUsername("aluno@repz.com")).isSameAs(aluno);

        when(userRepository.findByEmail("missing@repz.com")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.loadUserByUsername("missing@repz.com"))
                .isInstanceOf(org.springframework.security.core.userdetails.UsernameNotFoundException.class);
    }
}
