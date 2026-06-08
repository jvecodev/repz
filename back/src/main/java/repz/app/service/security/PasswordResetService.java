package repz.app.service.security;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.mail.MailException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import repz.app.dto.auth.ForgotPasswordRequest;
import repz.app.dto.auth.ResetPasswordRequest;
import repz.app.message.Mensagens;
import repz.app.persistence.entity.PasswordResetToken;
import repz.app.persistence.repository.PasswordResetTokenRepository;
import repz.app.persistence.repository.UserRepository;
import repz.app.service.email.EmailService;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.HexFormat;

@Slf4j
@Service
@RequiredArgsConstructor
public class PasswordResetService {

    private final UserRepository userRepository;
    private final PasswordResetTokenRepository tokenRepository;
    private final EmailService emailService;
    private final PasswordEncoder passwordEncoder;
    private final Mensagens mensagens;

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    @Value("${api.password-reset.expiration-minutes:30}")
    private int expirationMinutes;

    @Transactional
    public void forgotPassword(ForgotPasswordRequest request) {
        userRepository.findByEmail(request.email()).ifPresent(user -> {
            tokenRepository.deleteByUserId(user.getId());

            var token = new PasswordResetToken();
            token.setUser(user);
            byte[] bytes = new byte[16];
            SECURE_RANDOM.nextBytes(bytes);
            token.setToken(HexFormat.of().formatHex(bytes));
            token.setExpiresAt(LocalDateTime.now().plusMinutes(expirationMinutes));
            token.setUsed(false);
            tokenRepository.save(token);

            log.debug("[PasswordReset] Token gerado para {}", user.getEmail());

            try {
                emailService.sendPasswordResetEmail(user.getEmail(), token.getToken());
            } catch (MailException e) {
                log.warn("[PasswordReset] Falha ao enviar e-mail para {}: {}", user.getEmail(), e.getMessage());
            }
        });
    }

    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        var resetToken = tokenRepository.findByToken(request.token())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.BAD_REQUEST, mensagens.get("auth.reset.token.invalido")));

        if (resetToken.getUsed() || resetToken.isExpired()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, mensagens.get("auth.reset.token.invalido"));
        }

        var user = resetToken.getUser();
        user.setPassword(passwordEncoder.encode(request.newPassword()));

        resetToken.setUsed(true);
        tokenRepository.save(resetToken);
    }
}
