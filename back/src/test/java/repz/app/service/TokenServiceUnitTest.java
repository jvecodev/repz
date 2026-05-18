package repz.app.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import repz.app.message.Mensagens;
import repz.app.persistence.entity.User;
import repz.app.persistence.entity.UserRole;
import repz.app.service.security.TokenService;

import static org.assertj.core.api.Assertions.assertThat;
import static repz.app.unit.UnitTestData.user;

@ExtendWith(MockitoExtension.class)
class TokenServiceUnitTest {

    @Mock
    private Mensagens mensagens;

    private TokenService tokenService;

    @BeforeEach
    void setUp() {
        tokenService = new TokenService(mensagens);
        ReflectionTestUtils.setField(tokenService, "secret", "unit-test-secret");
    }

    @Test
    void geraEValidaAccessTokenComEmailDoUsuario() {
        User user = user(1L, UserRole.ADMIN);

        String token = tokenService.generateToken(user);

        assertThat(tokenService.validateToken(token)).isEqualTo(user.getEmail());
        assertThat(tokenService.validateRefreshToken(token)).isNull();
    }

    @Test
    void geraEValidaRefreshTokenSeparadoDoAccessToken() {
        User user = user(2L, UserRole.ALUNO);

        String token = tokenService.generateRefreshToken(user);

        assertThat(tokenService.validateRefreshToken(token)).isEqualTo(user.getEmail());
        assertThat(tokenService.validateToken(token)).isNull();
    }
}
