package repz.app.service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import repz.app.persistence.entity.UserRole;
import repz.app.service.security.TokenService;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class TokenServiceTestIntegration extends ServiceIntegrationSupport {

    @Autowired
    private TokenService tokenService;

    @Test
    void geraEValidaTokenDeAcesso() {
        var user = criarUsuario(UserRole.ALUNO, "token-acesso");

        var token = tokenService.generateToken(user);

        assertThat(token).isNotBlank();
        assertThat(tokenService.validateToken(token)).isEqualTo(user.getEmail());
        assertThat(tokenService.validateToken("token-invalido")).isNull();
    }

    @Test
    void geraEValidaRefreshTokenSeparadamente() {
        var user = criarUsuario(UserRole.ALUNO, "token-refresh");

        var refreshToken = tokenService.generateRefreshToken(user);

        assertThat(refreshToken).isNotBlank();
        assertThat(tokenService.validateRefreshToken(refreshToken)).isEqualTo(user.getEmail());
        assertThat(tokenService.validateToken(refreshToken)).isNull();
    }
}
