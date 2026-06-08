package repz.app.service;

import com.auth0.jwt.interfaces.DecodedJWT;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import repz.app.persistence.entity.RevokedToken;
import repz.app.persistence.repository.RevokedTokenRepository;
import repz.app.service.security.TokenBlacklistService;
import repz.app.service.security.TokenService;

import java.time.LocalDateTime;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TokenBlacklistServiceUnitTest {

    @Mock
    private RevokedTokenRepository revokedTokenRepository;
    @Mock
    private TokenService tokenService;

    @InjectMocks
    private TokenBlacklistService service;

    @Test
    void revokeAdicionaTokenNoRepositorio() {
        DecodedJWT decoded = mock(DecodedJWT.class);
        when(decoded.getId()).thenReturn("jti-abc");
        when(decoded.getExpiresAt()).thenReturn(new Date(System.currentTimeMillis() + 60_000));
        when(tokenService.decodeToken("some-token")).thenReturn(decoded);
        when(revokedTokenRepository.existsById("jti-abc")).thenReturn(false);

        service.revoke("some-token");

        ArgumentCaptor<RevokedToken> captor = ArgumentCaptor.forClass(RevokedToken.class);
        verify(revokedTokenRepository).save(captor.capture());
        assertThat(captor.getValue().getJti()).isEqualTo("jti-abc");
    }

    @Test
    void revokeIgnoraTokenInvalido() {
        when(tokenService.decodeToken("bad-token")).thenReturn(null);

        service.revoke("bad-token");

        verify(revokedTokenRepository, never()).save(any());
    }

    @Test
    void revokeIgnoraTokenSemJti() {
        DecodedJWT decoded = mock(DecodedJWT.class);
        when(decoded.getId()).thenReturn(null);
        when(tokenService.decodeToken("no-jti")).thenReturn(decoded);

        service.revoke("no-jti");

        verify(revokedTokenRepository, never()).save(any());
    }

    @Test
    void revokeIgnoraTokenJaRevogado() {
        DecodedJWT decoded = mock(DecodedJWT.class);
        when(decoded.getId()).thenReturn("jti-dup");
        when(decoded.getExpiresAt()).thenReturn(new Date(System.currentTimeMillis() + 60_000));
        when(tokenService.decodeToken("dup-token")).thenReturn(decoded);
        when(revokedTokenRepository.existsById("jti-dup")).thenReturn(true);

        service.revoke("dup-token");

        verify(revokedTokenRepository, never()).save(any());
    }

    @Test
    void isRevokedRetornaTrueParaJtiRevogado() {
        when(revokedTokenRepository.existsById("jti-rev")).thenReturn(true);
        assertThat(service.isRevoked("jti-rev")).isTrue();
    }

    @Test
    void isRevokedRetornaFalseParaJtiNaoRevogado() {
        when(revokedTokenRepository.existsById("jti-ok")).thenReturn(false);
        assertThat(service.isRevoked("jti-ok")).isFalse();
    }

    @Test
    void isRevokedRetornaFalseParaJtiNulo() {
        assertThat(service.isRevoked(null)).isFalse();
    }

    @Test
    void limparExpiradosDelega() {
        when(revokedTokenRepository.deleteExpired(any(LocalDateTime.class))).thenReturn(0);

        service.limparExpirados();

        verify(revokedTokenRepository).deleteExpired(any(LocalDateTime.class));
    }
}
