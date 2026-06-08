package repz.app.dto;

import org.junit.jupiter.api.Test;
import repz.app.dto.auth.AuthenticationDTO;
import repz.app.dto.auth.ForgotPasswordRequest;
import repz.app.dto.auth.LoginResponseDTO;
import repz.app.dto.auth.RegistrationDTO;
import repz.app.dto.auth.ResetPasswordRequest;
import repz.app.persistence.entity.UserRole;

import static org.assertj.core.api.Assertions.assertThat;

class AuthDTOTest {

    @Test
    void authenticationDTOCriaComEmailESenha() {
        var dto = new AuthenticationDTO("user@repz.com", "senha123");
        assertThat(dto.email()).isEqualTo("user@repz.com");
        assertThat(dto.password()).isEqualTo("senha123");
    }

    @Test
    void loginResponseDTOCriaComTokens() {
        var dto = new LoginResponseDTO("access-token", "refresh-token");
        assertThat(dto.token()).isEqualTo("access-token");
        assertThat(dto.refreshToken()).isEqualTo("refresh-token");
    }

    @Test
    void forgotPasswordRequestCriaComEmail() {
        var dto = new ForgotPasswordRequest("forgot@repz.com");
        assertThat(dto.email()).isEqualTo("forgot@repz.com");
    }

    @Test
    void resetPasswordRequestCriaComTokenESenha() {
        var dto = new ResetPasswordRequest("reset-token-123", "novaSenha");
        assertThat(dto.token()).isEqualTo("reset-token-123");
        assertThat(dto.newPassword()).isEqualTo("novaSenha");
    }

    @Test
    void authenticationDTOEqualsEHashCode() {
        var a = new AuthenticationDTO("x@r.com", "pass");
        var b = new AuthenticationDTO("x@r.com", "pass");
        assertThat(a).isEqualTo(b);
        assertThat(a.hashCode()).isEqualTo(b.hashCode());
        assertThat(a.toString()).contains("x@r.com");
    }

    @Test
    void loginResponseDTOEqualsEHashCode() {
        var a = new LoginResponseDTO("tok", "ref");
        var b = new LoginResponseDTO("tok", "ref");
        assertThat(a).isEqualTo(b);
        assertThat(a.hashCode()).isEqualTo(b.hashCode());
        assertThat(a.toString()).contains("tok");
    }

    @Test
    void forgotPasswordRequestEqualsEHashCode() {
        var a = new ForgotPasswordRequest("a@r.com");
        var b = new ForgotPasswordRequest("a@r.com");
        assertThat(a).isEqualTo(b);
        assertThat(a.hashCode()).isEqualTo(b.hashCode());
        assertThat(a.toString()).contains("a@r.com");
    }

    @Test
    void resetPasswordRequestEqualsEHashCode() {
        var a = new ResetPasswordRequest("tok", "pass");
        var b = new ResetPasswordRequest("tok", "pass");
        assertThat(a).isEqualTo(b);
        assertThat(a.hashCode()).isEqualTo(b.hashCode());
        assertThat(a.toString()).contains("tok");
    }

    @Test
    void recordsNaoSaoIguaisComValoresDiferentes() {
        assertThat(new AuthenticationDTO("a@r.com", "p1"))
                .isNotEqualTo(new AuthenticationDTO("b@r.com", "p1"));
        assertThat(new LoginResponseDTO("tok1", "ref1"))
                .isNotEqualTo(new LoginResponseDTO("tok2", "ref1"));
        assertThat(new ForgotPasswordRequest("a@r.com"))
                .isNotEqualTo(new ForgotPasswordRequest("b@r.com"));
        assertThat(new ResetPasswordRequest("t1", "p"))
                .isNotEqualTo(new ResetPasswordRequest("t2", "p"));
    }

    @Test
    void recordsNaoSaoIguaisComNull() {
        assertThat(new AuthenticationDTO("a@r.com", "p")).isNotEqualTo(null);
        assertThat(new LoginResponseDTO("tok", "ref")).isNotEqualTo(null);
        assertThat(new ForgotPasswordRequest("a@r.com")).isNotEqualTo(null);
        assertThat(new ResetPasswordRequest("tok", "pass")).isNotEqualTo(null);
    }

    @Test
    void registrationDTOCriaComTodosCampos() {
        var dto = new RegistrationDTO("Eduardo", "edu@repz.com", "senha123", UserRole.ALUNO);
        assertThat(dto.name()).isEqualTo("Eduardo");
        assertThat(dto.email()).isEqualTo("edu@repz.com");
        assertThat(dto.password()).isEqualTo("senha123");
        assertThat(dto.role()).isEqualTo(UserRole.ALUNO);
    }

    @Test
    void registrationDTOEqualsEHashCode() {
        var a = new RegistrationDTO("Eduardo", "edu@repz.com", "senha123", UserRole.ALUNO);
        var b = new RegistrationDTO("Eduardo", "edu@repz.com", "senha123", UserRole.ALUNO);
        assertThat(a).isEqualTo(b);
        assertThat(a.hashCode()).isEqualTo(b.hashCode());
        assertThat(a.toString()).contains("Eduardo");
    }

    @Test
    void registrationDTONaoEIgualComValoresDiferentes() {
        var a = new RegistrationDTO("A", "a@r.com", "pass", UserRole.ALUNO);
        var b = new RegistrationDTO("B", "b@r.com", "pass", UserRole.PERSONAL);
        assertThat(a).isNotEqualTo(b);
        assertThat(a).isNotEqualTo(null);
    }
}
