package repz.app.service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import repz.app.dto.auth.ForgotPasswordRequest;
import repz.app.dto.auth.ResetPasswordRequest;
import repz.app.persistence.entity.UserRole;
import repz.app.persistence.repository.PasswordResetTokenRepository;
import repz.app.service.email.EmailService;
import repz.app.service.security.PasswordResetService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@SpringBootTest
@Transactional
class PasswordResetServiceTestIntegration extends ServiceIntegrationSupport {

    @Autowired
    private PasswordResetService passwordResetService;

    @Autowired
    private PasswordResetTokenRepository tokenRepository;

    @MockitoBean
    private EmailService emailService;

    @Test
    void forgotPasswordEnviaEmailEGeraTokenParaEmailCadastrado() {
        var user = criarUsuario(UserRole.ALUNO, "reset-usuario");

        passwordResetService.forgotPassword(new ForgotPasswordRequest(user.getEmail()));

        var token = tokenRepository.findAll().stream()
                .filter(t -> t.getUser().getId().equals(user.getId()))
                .findFirst();

        assertThat(token).isPresent();
        assertThat(token.get().getUsed()).isFalse();
        assertThat(token.get().isExpired()).isFalse();
        verify(emailService, times(1)).sendPasswordResetEmail(anyString(), anyString());
    }

    @Test
    void forgotPasswordNaoEnviaEmailParaEmailNaoCadastrado() {
        passwordResetService.forgotPassword(new ForgotPasswordRequest("inexistente@repz.com"));

        verify(emailService, never()).sendPasswordResetEmail(anyString(), anyString());
    }

    @Test
    void resetPasswordAtualizaSenhaComTokenValido() {
        var user = criarUsuario(UserRole.ALUNO, "reset-senha");
        passwordResetService.forgotPassword(new ForgotPasswordRequest(user.getEmail()));

        var token = tokenRepository.findAll().stream()
                .filter(t -> t.getUser().getId().equals(user.getId()))
                .findFirst()
                .orElseThrow();

        passwordResetService.resetPassword(new ResetPasswordRequest(token.getToken(), "novaSenha123"));

        var updatedToken = tokenRepository.findByToken(token.getToken()).orElseThrow();
        assertThat(updatedToken.getUsed()).isTrue();

        var updatedUser = userRepository.findById(user.getId().intValue()).orElseThrow();
        assertThat(updatedUser.getPassword()).isNotEqualTo("{noop}123456");
    }

    @Test
    void resetPasswordRejeitaTokenInvalido() {
        assertThatThrownBy(() ->
                passwordResetService.resetPassword(new ResetPasswordRequest("token-invalido", "novaSenha123")))
                .isInstanceOf(ResponseStatusException.class);
    }

    @Test
    void resetPasswordRejeitaTokenJaUtilizado() {
        var user = criarUsuario(UserRole.ALUNO, "reset-reuso");
        passwordResetService.forgotPassword(new ForgotPasswordRequest(user.getEmail()));

        var token = tokenRepository.findAll().stream()
                .filter(t -> t.getUser().getId().equals(user.getId()))
                .findFirst()
                .orElseThrow();

        passwordResetService.resetPassword(new ResetPasswordRequest(token.getToken(), "primeiraSenha1"));

        assertThatThrownBy(() ->
                passwordResetService.resetPassword(new ResetPasswordRequest(token.getToken(), "segundaSenha2")))
                .isInstanceOf(ResponseStatusException.class);
    }

    @Test
    void forgotPasswordSobrescreveTokenAnteriorDoMesmoUsuario() {
        var user = criarUsuario(UserRole.ALUNO, "reset-sobrescreve");

        passwordResetService.forgotPassword(new ForgotPasswordRequest(user.getEmail()));
        var tokensApos1 = tokenRepository.findAll().stream()
                .filter(t -> t.getUser().getId().equals(user.getId()))
                .toList();

        passwordResetService.forgotPassword(new ForgotPasswordRequest(user.getEmail()));
        var tokensApos2 = tokenRepository.findAll().stream()
                .filter(t -> t.getUser().getId().equals(user.getId()))
                .toList();

        assertThat(tokensApos1).hasSize(1);
        assertThat(tokensApos2).hasSize(1);
        assertThat(tokensApos1.get(0).getToken()).isNotEqualTo(tokensApos2.get(0).getToken());
    }
}
