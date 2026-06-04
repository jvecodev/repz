package repz.app.service.security;

import com.auth0.jwt.interfaces.DecodedJWT;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import repz.app.persistence.entity.RevokedToken;
import repz.app.persistence.repository.RevokedTokenRepository;

import java.time.LocalDateTime;
import java.time.ZoneId;

/**
 * Mantém a lista de access tokens revogados por logout. Como os tokens são
 * stateless, a revogação é feita guardando o jti até a sua expiração natural.
 */
@Service
@RequiredArgsConstructor
public class TokenBlacklistService {

    private final RevokedTokenRepository revokedTokenRepository;
    private final TokenService tokenService;

    /** Revoga um access token a partir do seu jti, ignorando tokens inválidos ou antigos sem jti. */
    public void revoke(String token) {
        DecodedJWT decoded = tokenService.decodeToken(token);
        if (decoded == null) {
            return;
        }
        String jti = decoded.getId();
        if (jti == null || decoded.getExpiresAt() == null) {
            return;
        }
        if (revokedTokenRepository.existsById(jti)) {
            return;
        }
        LocalDateTime expiresAt = decoded.getExpiresAt().toInstant()
                .atZone(ZoneId.systemDefault())
                .toLocalDateTime();
        revokedTokenRepository.save(new RevokedToken(jti, expiresAt));
    }

    public boolean isRevoked(String jti) {
        return jti != null && revokedTokenRepository.existsById(jti);
    }

    /** Remove de hora em hora os tokens revogados que já expiraram. */
    @Scheduled(cron = "0 0 * * * *")
    @Transactional
    public void limparExpirados() {
        revokedTokenRepository.deleteExpired(LocalDateTime.now());
    }
}
