package repz.app.service.personal;

import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import repz.app.dto.request.PersonalCreateRequest;
import repz.app.dto.request.PersonalUpdateRequest;
import repz.app.dto.response.AlunoResponse;
import repz.app.dto.response.PersonalAlunosResponse;
import repz.app.dto.response.PersonalResponse;
import repz.app.message.Mensagens;
import repz.app.persistence.entity.Academia;
import repz.app.persistence.entity.Aluno;
import repz.app.persistence.entity.Personal;
import repz.app.persistence.entity.User;
import repz.app.persistence.entity.UserRole;
import repz.app.persistence.repository.AcademiaRepository;
import repz.app.persistence.repository.AlunoRepository;
import repz.app.persistence.repository.PersonalRepository;
import repz.app.persistence.repository.UserRepository;
import repz.app.service.academia.AcademiaContextService;

import java.util.List;
import java.util.stream.Collectors;

@Service
@AllArgsConstructor
public class PersonalServiceImpl implements PersonalService {

    private final PersonalRepository personalRepository;
    private final UserRepository userRepository;
    private final AcademiaRepository academiaRepository;
    private final AlunoRepository alunoRepository;
    private final AcademiaContextService academiaContextService;
    private final Mensagens mensagens;

    public PersonalResponse criar(PersonalCreateRequest request, Long academiaHeaderId, Authentication auth) {
        User currentUser = userRepository.findByEmail(auth.getName())
                .orElseThrow(() -> new RuntimeException(mensagens.get("usuario.nao.encontrado")));
        Long academiaId = resolveAcademiaFromRequest(request.getAcademiaId(), academiaHeaderId, auth);

        if (currentUser.getRole() == UserRole.GERENTE) {
            Academia academia = academiaRepository.findById(academiaId)
                    .orElseThrow(() -> new RuntimeException(mensagens.get("academia.nao.encontrada")));

            if (!academia.getResponsibleUser().getId().equals(currentUser.getId())) {
                throw new RuntimeException(mensagens.get("personal.academia.registro.apenas.propria"));
            }
        } else if (currentUser.getRole() != UserRole.ADMIN) {
            throw new RuntimeException(mensagens.get("personal.acesso.criar.negado"));
        }

        User user = userRepository.findById(Math.toIntExact(request.getUserId()))
                .orElseThrow(() -> new RuntimeException(mensagens.get("usuario.nao.encontrado")));
        if (user.getRole() != UserRole.PERSONAL) {
            throw new AccessDeniedException(mensagens.get("personal.usuario.role.invalida"));
        }
        if (personalRepository.findByUserId(user.getId()).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, mensagens.get("personal.usuario.ja.vinculado"));
        }

        Academia academia = academiaRepository.findById(academiaId)
                .orElseThrow(() -> new RuntimeException(mensagens.get("academia.nao.encontrada")));

        Personal personal = new Personal();
        personal.setUser(user);
        personal.setAcademia(academia);
        personal.setEspecialidade(request.getEspecialidade());
        personal.setAtivo(true);

        Personal saved = personalRepository.save(personal);
        return toDTO(saved);
    }

    public List<PersonalResponse> findAll(Long academiaHeaderId, Authentication auth) {
        User currentUser = userRepository.findByEmail(auth.getName())
                .orElseThrow(() -> new RuntimeException(mensagens.get("usuario.nao.encontrado")));
        Long academiaId = academiaContextService.resolveOptional(auth, academiaHeaderId);

        if (currentUser.getRole() == UserRole.ADMIN) {
            return personalRepository.findAll().stream()
                    .filter(p -> academiaId == null || p.getAcademia().getId().equals(academiaId))
                    .map(this::toDTO)
                    .collect(Collectors.toList());
        } else if (currentUser.getRole() == UserRole.GERENTE) {
            Academia academia = academiaRepository.findByResponsibleUserId(currentUser.getId())
                    .stream()
                    .findFirst()
                    .orElseThrow(() -> new RuntimeException(mensagens.get("academia.nao.encontrada")));

            return personalRepository.findAll().stream()
                    .filter(p -> p.getAcademia().getId().equals(academia.getId()))
                    .map(this::toDTO)
                    .collect(Collectors.toList());
        }

        throw new RuntimeException(mensagens.get("personal.acesso.listar.negado"));
    }

    public PersonalResponse findById(Long id) {
        Personal personal = personalRepository.findById(id)
                .orElseThrow(() -> new RuntimeException(mensagens.get("personal.nao.encontrado")));
        return toDTO(personal);
    }

    public PersonalResponse atualizar(Long id, PersonalUpdateRequest request, Long academiaHeaderId, Authentication auth) {
        User currentUser = userRepository.findByEmail(auth.getName())
                .orElseThrow(() -> new RuntimeException(mensagens.get("usuario.nao.encontrado")));
        Long academiaId = academiaContextService.resolveOptional(auth, academiaHeaderId);

        Personal personal = personalRepository.findById(id)
                .orElseThrow(() -> new RuntimeException(mensagens.get("personal.nao.encontrado")));
        validatePersonalAcademia(personal, academiaId);

        if (currentUser.getRole() == UserRole.GERENTE) {
            Academia academia = academiaRepository.findByResponsibleUserId(currentUser.getId())
                    .stream()
                    .findFirst()
                    .orElseThrow(() -> new RuntimeException(mensagens.get("academia.nao.encontrada")));

            if (!personal.getAcademia().getId().equals(academia.getId())) {
                throw new RuntimeException(mensagens.get("personal.academia.editar.apenas.propria"));
            }
        } else if (currentUser.getRole() != UserRole.ADMIN) {
            throw new RuntimeException(mensagens.get("personal.acesso.editar.negado"));
        }

        personal.setEspecialidade(request.getEspecialidade());
        if (request.getAtivo() != null) {
            personal.setAtivo(request.getAtivo());
        }

        Personal updated = personalRepository.save(personal);
        return toDTO(updated);
    }

    public PersonalResponse ativar(Long id, Long academiaHeaderId, Authentication auth) {
        return alterarStatusPersonal(id, academiaHeaderId, auth, true);
    }

    public PersonalResponse desativar(Long id, Long academiaHeaderId, Authentication auth) {
        return alterarStatusPersonal(id, academiaHeaderId, auth, false);
    }

    private PersonalResponse alterarStatusPersonal(Long id, Long academiaHeaderId, Authentication auth, boolean ativo) {
        User currentUser = userRepository.findByEmail(auth.getName())
                .orElseThrow(() -> new RuntimeException(mensagens.get("usuario.nao.encontrado")));
        Long academiaId = academiaContextService.resolveOptional(auth, academiaHeaderId);

        Personal personal = personalRepository.findById(id)
                .orElseThrow(() -> new RuntimeException(mensagens.get("personal.nao.encontrado")));
        validatePersonalAcademia(personal, academiaId);

        if (currentUser.getRole() == UserRole.GERENTE) {
            Academia academia = academiaRepository.findByResponsibleUserId(currentUser.getId())
                    .stream()
                    .findFirst()
                    .orElseThrow(() -> new RuntimeException(mensagens.get("academia.nao.encontrada")));

            if (!personal.getAcademia().getId().equals(academia.getId())) {
                throw new RuntimeException(mensagens.get("personal.academia.alterar.apenas.propria"));
            }
        } else if (currentUser.getRole() != UserRole.ADMIN) {
            throw new RuntimeException(mensagens.get("personal.acesso.alterar.status.negado"));
        }

        personal.setAtivo(ativo);
        Personal updated = personalRepository.save(personal);
        return toDTO(updated);
    }

    public PersonalResponse obterMeuPerfil(Authentication auth) {
        User currentUser = userRepository.findByEmail(auth.getName())
                .orElseThrow(() -> new RuntimeException(mensagens.get("usuario.nao.encontrado")));

        Personal personal = personalRepository.findAll().stream()
                .filter(p -> p.getUser().getId().equals(currentUser.getId()))
                .findFirst()
                .orElseThrow(() -> new RuntimeException(mensagens.get("personal.nao.encontrado")));

        return toDTO(personal);
    }

    public PersonalResponse atualizarMeuPerfil(PersonalUpdateRequest request, Authentication auth) {
        User currentUser = userRepository.findByEmail(auth.getName())
                .orElseThrow(() -> new RuntimeException(mensagens.get("usuario.nao.encontrado")));

        Personal personal = personalRepository.findAll().stream()
                .filter(p -> p.getUser().getId().equals(currentUser.getId()))
                .findFirst()
                .orElseThrow(() -> new RuntimeException(mensagens.get("personal.nao.encontrado")));

        personal.setEspecialidade(request.getEspecialidade());
        Personal updated = personalRepository.save(personal);
        return toDTO(updated);
    }

    public PersonalAlunosResponse obterMeusAlunos(Authentication auth) {
        User currentUser = userRepository.findByEmail(auth.getName())
                .orElseThrow(() -> new RuntimeException(mensagens.get("usuario.nao.encontrado")));

        Personal personal = personalRepository.findAll().stream()
                .filter(p -> p.getUser().getId().equals(currentUser.getId()))
                .findFirst()
                .orElseThrow(() -> new RuntimeException(mensagens.get("personal.nao.encontrado")));

        List<AlunoResponse> alunos = alunoRepository.findByPersonalId(personal.getId()).stream()
                .map(Aluno::getUsuario)
                .map(u -> new AlunoResponse(u.getId(), u.getName(), u.getEmail()))
                .collect(Collectors.toList());

        return new PersonalAlunosResponse(
                personal.getId(),
                personal.getUser().getName(),
                personal.getEspecialidade(),
                personal.getAcademia().getId(),
                personal.getAcademia().getName(),
                alunos
        );
    }

    private Long resolveAcademiaFromRequest(Long requestAcademiaId, Long headerAcademiaId, Authentication auth) {
        Long requestedAcademiaId = headerAcademiaId != null ? headerAcademiaId : requestAcademiaId;
        Long resolvedAcademiaId = academiaContextService.resolveRequired(auth, requestedAcademiaId);

        if (requestAcademiaId != null && !requestAcademiaId.equals(resolvedAcademiaId)) {
            throw new RuntimeException(mensagens.get("personal.academia.corpo.difere"));
        }

        return resolvedAcademiaId;
    }

    private void validatePersonalAcademia(Personal personal, Long academiaId) {
        if (academiaId != null && !personal.getAcademia().getId().equals(academiaId)) {
            throw new RuntimeException(mensagens.get("personal.nao.pertence.academia"));
        }
    }

    private PersonalResponse toDTO(Personal personal) {
        return new PersonalResponse(
                personal.getId(),
                personal.getUser().getId(),
                personal.getUser().getName(),
                personal.getUser().getEmail(),
                personal.getAcademia().getId(),
                personal.getAcademia().getName(),
                personal.getEspecialidade(),
                personal.getAtivo(),
                personal.getUser().getFotoUrl()
        );
    }
}
