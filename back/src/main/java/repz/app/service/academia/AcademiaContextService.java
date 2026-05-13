package repz.app.service.academia;

import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import repz.app.persistence.entity.User;
import repz.app.persistence.entity.UserRole;
import repz.app.persistence.repository.AcademiaRepository;
import repz.app.persistence.repository.PersonalRepository;
import repz.app.persistence.repository.UserRepository;
import repz.app.message.Mensagens;

@Service
@RequiredArgsConstructor
public class AcademiaContextService {

    public static final String HEADER_NAME = "X-Academia-Id";

    private final UserRepository userRepository;
    private final AcademiaRepository academiaRepository;
    private final PersonalRepository personalRepository;
    private final Mensagens mensagens;

    public Long resolveOptional(Authentication auth, Long requestedAcademiaId) {
        User currentUser = getCurrentUser(auth);

        if (currentUser.getRole() == UserRole.ADMIN) {
            if (requestedAcademiaId != null) {
                assertAcademiaExists(requestedAcademiaId);
            }
            return requestedAcademiaId;
        }

        if (currentUser.getRole() == UserRole.ALUNO) {
            if (requestedAcademiaId != null) {
                assertAcademiaExists(requestedAcademiaId);
            }
            return requestedAcademiaId;
        }

        Long userAcademiaId = getUserAcademiaId(currentUser);
        validateRequestedAcademia(requestedAcademiaId, userAcademiaId);
        return userAcademiaId;
    }

    public Long resolveRequired(Authentication auth, Long requestedAcademiaId) {
        Long academiaId = resolveOptional(auth, requestedAcademiaId);
        if (academiaId == null) {
            throw new IllegalArgumentException(mensagens.get("academia.header.obrigatorio", HEADER_NAME));
        }
        return academiaId;
    }

    private User getCurrentUser(Authentication auth) {
        if (auth == null || auth.getName() == null) {
            throw new AccessDeniedException(mensagens.get("auth.usuario.nao.autenticado"));
        }

        return userRepository.findByEmail(auth.getName())
                .orElseThrow(() -> new AccessDeniedException(mensagens.get("usuario.nao.encontrado")));
    }

    private Long getUserAcademiaId(User user) {
        if (user.getRole() == UserRole.GERENTE) {
            return academiaRepository.findByResponsibleUserId(user.getId()).stream()
                    .findFirst()
                    .orElseThrow(() -> new AccessDeniedException(mensagens.get("auth.usuario.sem.academia")))
                    .getId();
        }

        if (user.getRole() == UserRole.PERSONAL) {
            return personalRepository.findByUserId(user.getId())
                    .orElseThrow(() -> new AccessDeniedException(mensagens.get("auth.personal.sem.academia")))
                    .getAcademia()
                    .getId();
        }

        throw new AccessDeniedException(mensagens.get("auth.perfil.sem.contexto.academia"));
    }

    private void validateRequestedAcademia(Long requestedAcademiaId, Long userAcademiaId) {
        if (requestedAcademiaId != null && !requestedAcademiaId.equals(userAcademiaId)) {
            throw new AccessDeniedException(mensagens.get("auth.academia.acesso.negado"));
        }
    }

    private void assertAcademiaExists(Long academiaId) {
        if (!academiaRepository.existsById(academiaId)) {
            throw new IllegalArgumentException(mensagens.get("academia.nao.encontrada"));
        }
    }
}
