package repz.app.service.aluno;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import repz.app.dto.request.AlunoCreateRequest;
import repz.app.dto.request.AlunoMeUpdateRequest;
import repz.app.dto.request.AlunoUpdateRequest;
import repz.app.dto.response.AlunoDetalheResponse;
import repz.app.message.Mensagens;
import repz.app.persistence.entity.*;
import repz.app.persistence.repository.*;
import repz.app.service.academia.AcademiaContextService;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AlunoServiceImpl implements AlunoService {

    private final AlunoRepository alunoRepository;
    private final UserRepository userRepository;
    private final AcademiaRepository academiaRepository;
    private final PersonalRepository personalRepository;
    private final PlanoRepository planoRepository;
    private final AcademiaContextService academiaContextService;
    private final PasswordEncoder passwordEncoder;
    private final Mensagens mensagens;

    @Override
    public AlunoDetalheResponse matricular(AlunoCreateRequest request, Long academiaHeaderId, Authentication auth) {
        Long academiaId = academiaContextService.resolveRequired(auth, academiaHeaderId);

        User usuario = userRepository.findByIdAndDeletedAtIsNull(Math.toIntExact(request.userId()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, mensagens.get("usuario.nao.encontrado")));
        if (usuario.getRole() != UserRole.USUARIO) {
            throw new AccessDeniedException(mensagens.get("aluno.usuario.role.invalida"));
        }

        if (alunoRepository.existsByUsuarioIdAndAcademiaId(request.userId(), academiaId)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, mensagens.get("aluno.ja.matriculado"));
        }

        Academia academia = academiaRepository.findById(academiaId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, mensagens.get("academia.nao.encontrada")));

        Plano plano = planoRepository.findById(request.planoId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, mensagens.get("plano.nao.encontrado")));
        validatePlanoAcademia(plano, academia);

        Personal personal = null;
        if (request.personalId() != null) {
            personal = personalRepository.findById(request.personalId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, mensagens.get("personal.nao.encontrado")));
            validatePersonalAcademia(personal, academia);
        }

        Aluno aluno = new Aluno();
        aluno.setUsuario(usuario);
        aluno.setAcademia(academia);
        aluno.setPlano(plano);
        aluno.setPersonal(personal);
        aluno.setDataInicio(LocalDate.now());
        aluno.setObjetivo(request.objetivo());
        aluno.setAtivo(true);

        return toResponse(alunoRepository.save(aluno));
    }

    @Override
    public List<AlunoDetalheResponse> findAll(Long academiaHeaderId, Authentication auth) {
        User currentUser = getCurrentUser(auth);
        Long academiaId = academiaContextService.resolveOptional(auth, academiaHeaderId);

        if (currentUser.getRole() == UserRole.ADMIN) {
            List<Aluno> alunos = academiaId != null
                    ? alunoRepository.findByAcademiaId(academiaId)
                    : alunoRepository.findAll();
            return alunos.stream().map(this::toResponse).toList();
        }

        if (currentUser.getRole() == UserRole.ACADEMIA) {
            return alunoRepository.findByAcademiaId(academiaId).stream()
                    .map(this::toResponse).toList();
        }

        if (currentUser.getRole() == UserRole.PERSONAL) {
            Personal personal = personalRepository.findByUserId(currentUser.getId())
                    .orElseThrow(() -> new AccessDeniedException(mensagens.get("personal.nao.encontrado")));
            return alunoRepository.findByPersonalId(personal.getId()).stream()
                    .map(this::toResponse).toList();
        }

        throw new AccessDeniedException(mensagens.get("erro.acesso.negado"));
    }

    @Override
    public AlunoDetalheResponse findById(Long id, Authentication auth) {
        User currentUser = getCurrentUser(auth);

        Aluno aluno = alunoRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, mensagens.get("aluno.nao.encontrado")));

        if (currentUser.getRole() == UserRole.PERSONAL) {
            Personal personal = personalRepository.findByUserId(currentUser.getId())
                    .orElseThrow(() -> new AccessDeniedException(mensagens.get("personal.nao.encontrado")));
            if (aluno.getPersonal() == null || !aluno.getPersonal().getId().equals(personal.getId())) {
                throw new AccessDeniedException(mensagens.get("aluno.acesso.negado.personal"));
            }
        }

        return toResponse(aluno);
    }

    @Override
    public AlunoDetalheResponse atualizar(Long id, AlunoUpdateRequest request, Long academiaHeaderId, Authentication auth) {
        Long academiaId = academiaContextService.resolveRequired(auth, academiaHeaderId);

        Aluno aluno = alunoRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, mensagens.get("aluno.nao.encontrado")));

        if (!aluno.getAcademia().getId().equals(academiaId)) {
            throw new AccessDeniedException(mensagens.get("aluno.academia.editar.apenas.propria"));
        }
        Academia academia = aluno.getAcademia();

        if (request.planoId() != null) {
            Plano plano = planoRepository.findById(request.planoId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, mensagens.get("plano.nao.encontrado")));
            validatePlanoAcademia(plano, academia);
            aluno.setPlano(plano);
        }

        if (request.personalId() != null) {
            Personal personal = personalRepository.findById(request.personalId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, mensagens.get("personal.nao.encontrado")));
            validatePersonalAcademia(personal, academia);
            aluno.setPersonal(personal);
        }

        if (request.objetivo() != null) {
            aluno.setObjetivo(request.objetivo());
        }

        return toResponse(alunoRepository.save(aluno));
    }

    @Override
    public void inativar(Long id, Long academiaHeaderId, Authentication auth) {
        Long academiaId = academiaContextService.resolveRequired(auth, academiaHeaderId);

        Aluno aluno = alunoRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, mensagens.get("aluno.nao.encontrado")));

        if (!aluno.getAcademia().getId().equals(academiaId)) {
            throw new AccessDeniedException(mensagens.get("aluno.academia.editar.apenas.propria"));
        }

        aluno.setAtivo(false);
        alunoRepository.save(aluno);
    }

    @Override
    public AlunoDetalheResponse obterMeuPerfil(Authentication auth) {
        User currentUser = getCurrentUser(auth);
        Aluno aluno = alunoRepository.findByUsuarioId(currentUser.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, mensagens.get("aluno.nao.encontrado")));
        return toResponse(aluno);
    }

    @Override
    public AlunoDetalheResponse atualizarMeuPerfil(AlunoMeUpdateRequest request, Authentication auth) {
        User currentUser = getCurrentUser(auth);
        Aluno aluno = alunoRepository.findByUsuarioId(currentUser.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, mensagens.get("aluno.nao.encontrado")));

        if (request.nome() != null) {
            currentUser.setName(request.nome());
            userRepository.save(currentUser);
        }
        if (request.telefone() != null) {
            aluno.setTelefone(request.telefone());
        }
        if (request.fotoUrl() != null) {
            aluno.setFotoUrl(request.fotoUrl());
        }
        if (request.senha() != null) {
            currentUser.setPassword(passwordEncoder.encode(request.senha()));
            userRepository.save(currentUser);
        }

        return toResponse(alunoRepository.save(aluno));
    }

    private User getCurrentUser(Authentication auth) {
        return userRepository.findByEmail(auth.getName())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, mensagens.get("usuario.nao.encontrado")));
    }

    private void validatePlanoAcademia(Plano plano, Academia academia) {
        if (plano.getAcademia() == null || !plano.getAcademia().getId().equals(academia.getId())) {
            throw new AccessDeniedException(mensagens.get("aluno.plano.nao.pertence.academia"));
        }
    }

    private void validatePersonalAcademia(Personal personal, Academia academia) {
        if (personal.getAcademia() == null || !personal.getAcademia().getId().equals(academia.getId())) {
            throw new AccessDeniedException(mensagens.get("aluno.personal.nao.pertence.academia"));
        }
    }

    private AlunoDetalheResponse toResponse(Aluno aluno) {
        AlunoDetalheResponse r = new AlunoDetalheResponse();
        r.setId(aluno.getId());
        r.setUserId(aluno.getUsuario().getId());
        r.setNome(aluno.getUsuario().getName());
        r.setEmail(aluno.getUsuario().getEmail());
        r.setTelefone(aluno.getTelefone());
        r.setFotoUrl(aluno.getFotoUrl());
        r.setAcademiaId(aluno.getAcademia().getId());
        r.setAcademiaNome(aluno.getAcademia().getName());
        r.setObjetivo(aluno.getObjetivo());
        r.setAtivo(aluno.getAtivo());
        if (aluno.getPersonal() != null) {
            r.setPersonalId(aluno.getPersonal().getId());
            r.setPersonalNome(aluno.getPersonal().getUser().getName());
        }
        if (aluno.getPlano() != null) {
            r.setPlanoId(aluno.getPlano().getId());
            r.setPlanoNome(aluno.getPlano().getNome());
        }
        return r;
    }
}
