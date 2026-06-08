package repz.app.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;
import repz.app.dto.auth.ForgotPasswordRequest;
import repz.app.dto.auth.ResetPasswordRequest;
import repz.app.message.Mensagens;
import repz.app.persistence.entity.PasswordResetToken;
import repz.app.persistence.entity.User;
import repz.app.persistence.entity.UserRole;
import repz.app.persistence.repository.PasswordResetTokenRepository;
import repz.app.persistence.repository.UserRepository;
import repz.app.service.email.EmailService;
import repz.app.service.security.PasswordResetService;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static repz.app.unit.UnitTestData.user;

@ExtendWith(MockitoExtension.class)
class PasswordResetServiceUnitTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private PasswordResetTokenRepository tokenRepository;
    @Mock
    private EmailService emailService;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private Mensagens mensagens;

    @InjectMocks
    private PasswordResetService service;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(service, "expirationMinutes", 30);
    }

    @Test
    void forgotPasswordGeraTokenEEnviaEmail() {
        User u = user(1L, UserRole.ALUNO);
        when(userRepository.findByEmail(u.getEmail())).thenReturn(Optional.of(u));
        doNothing().when(tokenRepository).deleteByUserId(u.getId());
        when(tokenRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        doNothing().when(emailService).sendPasswordResetEmail(anyString(), anyString());

        service.forgotPassword(new ForgotPasswordRequest(u.getEmail()));

        ArgumentCaptor<PasswordResetToken> captor = ArgumentCaptor.forClass(PasswordResetToken.class);
        verify(tokenRepository).save(captor.capture());
        assertThat(captor.getValue().getUser()).isSameAs(u);
        assertThat(captor.getValue().getToken()).isNotBlank();
        assertThat(captor.getValue().getExpiresAt()).isAfter(LocalDateTime.now());
        verify(emailService).sendPasswordResetEmail(anyString(), anyString());
    }

    @Test
    void forgotPasswordIgnoraEmailInexistente() {
        when(userRepository.findByEmail("nobody@repz.com")).thenReturn(Optional.empty());

        service.forgotPassword(new ForgotPasswordRequest("nobody@repz.com"));

        verify(tokenRepository, never()).save(any());
        verify(emailService, never()).sendPasswordResetEmail(anyString(), anyString());
    }

    @Test
    void forgotPasswordNaoLancaExcecaoQuandoEmailFalha() {
        User u = user(2L, UserRole.ALUNO);
        when(userRepository.findByEmail(u.getEmail())).thenReturn(Optional.of(u));
        doNothing().when(tokenRepository).deleteByUserId(u.getId());
        when(tokenRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        org.springframework.mail.MailSendException mailEx = new org.springframework.mail.MailSendException("SMTP down");
        org.mockito.Mockito.doThrow(mailEx).when(emailService).sendPasswordResetEmail(anyString(), anyString());

        // Should not throw - mail failure is swallowed
        service.forgotPassword(new ForgotPasswordRequest(u.getEmail()));

        verify(tokenRepository).save(any());
    }

    @Test
    void resetPasswordAlteraSenhaComTokenValido() {
        User u = user(3L, UserRole.ALUNO);
        PasswordResetToken token = new PasswordResetToken();
        token.setUser(u);
        token.setToken("valid-token");
        token.setUsed(false);
        token.setExpiresAt(LocalDateTime.now().plusMinutes(10));

        when(tokenRepository.findByToken("valid-token")).thenReturn(Optional.of(token));
        when(passwordEncoder.encode("newPass")).thenReturn("hashedNewPass");
        when(tokenRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.resetPassword(new ResetPasswordRequest("valid-token", "newPass"));

        assertThat(u.getPassword()).isEqualTo("hashedNewPass");
        assertThat(token.getUsed()).isTrue();
        verify(tokenRepository).save(token);
    }

    @Test
    void resetPasswordLancaExcecaoComTokenInexistente() {
        when(tokenRepository.findByToken("bad-token")).thenReturn(Optional.empty());
        when(mensagens.get(any())).thenReturn("token invalido");

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> service.resetPassword(new ResetPasswordRequest("bad-token", "pass")));
        assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void resetPasswordLancaExcecaoComTokenJaUsado() {
        User u = user(4L, UserRole.ALUNO);
        PasswordResetToken token = new PasswordResetToken();
        token.setUser(u);
        token.setToken("used-token");
        token.setUsed(true);
        token.setExpiresAt(LocalDateTime.now().plusMinutes(10));

        when(tokenRepository.findByToken("used-token")).thenReturn(Optional.of(token));
        when(mensagens.get(any())).thenReturn("token invalido");

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> service.resetPassword(new ResetPasswordRequest("used-token", "pass")));
        assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void resetPasswordLancaExcecaoComTokenExpirado() {
        User u = user(5L, UserRole.ALUNO);
        PasswordResetToken token = new PasswordResetToken();
        token.setUser(u);
        token.setToken("expired-token");
        token.setUsed(false);
        token.setExpiresAt(LocalDateTime.now().minusMinutes(1)); // expired

        when(tokenRepository.findByToken("expired-token")).thenReturn(Optional.of(token));
        when(mensagens.get(any())).thenReturn("token invalido");

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> service.resetPassword(new ResetPasswordRequest("expired-token", "pass")));
        assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }
}
