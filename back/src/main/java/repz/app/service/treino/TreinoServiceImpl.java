package repz.app.service.treino;

import lombok.AllArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import repz.app.dto.request.ExercicioTreinoCreateRequest;
import repz.app.dto.request.TreinoCreateRequest;
import repz.app.dto.response.ExercicioTreinoResponse;
import repz.app.dto.response.TreinoResponse;
import repz.app.message.Mensagens;
import repz.app.persistence.entity.ExercicioTreino;
import repz.app.persistence.entity.Personal;
import repz.app.persistence.entity.Treino;
import repz.app.persistence.entity.User;
import repz.app.persistence.entity.UserRole;
import repz.app.persistence.repository.PersonalRepository;
import repz.app.persistence.repository.TreinoRepository;
import repz.app.persistence.repository.UserRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@Service
@AllArgsConstructor
public class TreinoServiceImpl implements TreinoService {

    private final TreinoRepository treinoRepository;
    private final UserRepository userRepository;
    private final PersonalRepository personalRepository;
    private final Mensagens mensagens;

    @Override
    @Transactional
    public TreinoResponse criar(TreinoCreateRequest request, Authentication auth) {
        User currentUser = getCurrentUser(auth);

        if (currentUser.getRole() != UserRole.PERSONAL) {
            throw new AccessDeniedException(mensagens.get("treino.apenas.personal.cria"));
        }

        Personal personal = personalRepository.findByUserId(currentUser.getId())
                .orElseThrow(() -> new RuntimeException(mensagens.get("personal.nao.encontrado")));

        User aluno = userRepository.findById(Math.toIntExact(request.getAlunoId()))
                .orElseThrow(() -> new RuntimeException(mensagens.get("aluno.nao.encontrado")));

        if (aluno.getRole() != UserRole.ALUNO) {
            throw new RuntimeException(mensagens.get("treino.aluno.role.invalida"));
        }

        Treino treino = new Treino();
        treino.setNome(request.getNome());
        treino.setDivisao(request.getDivisao());
        treino.setObjetivo(request.getObjetivo());
        treino.setObservacoes(request.getObservacoes());
        treino.setValidadeAte(request.getValidadeAte());
        treino.setAtivo(true);
        treino.setAluno(aluno);
        treino.setPersonal(personal);
        treino.setAcademia(personal.getAcademia());
        treino.setNmUsuario(currentUser.getName());

        List<ExercicioTreino> exercicios = montarExercicios(request.getExercicios(), treino, currentUser.getName());
        treino.setExercicios(exercicios);

        Treino salvo = treinoRepository.save(treino);
        return toDTO(salvo);
    }

    @Override
    public List<TreinoResponse> obterMinhaFichaAtiva(Authentication auth) {
        User currentUser = getCurrentUser(auth);
        return treinoRepository.findByAluno_IdAndAtivoTrueOrderByDivisaoAsc(currentUser.getId())
                .stream().map(this::toDTO).collect(Collectors.toList());
    }

    @Override
    public List<TreinoResponse> obterMeuHistorico(Authentication auth) {
        User currentUser = getCurrentUser(auth);
        return treinoRepository.findByAluno_IdAndAtivoFalseOrderByDtInclusaoDesc(currentUser.getId())
                .stream().map(this::toDTO).collect(Collectors.toList());
    }

    @Override
    public List<TreinoResponse> obterFichaAtivaDoAluno(Long alunoId, Authentication auth) {
        validarAcessoAoAluno(alunoId, auth);
        return treinoRepository.findByAluno_IdAndAtivoTrueOrderByDivisaoAsc(alunoId)
                .stream().map(this::toDTO).collect(Collectors.toList());
    }

    @Override
    public List<TreinoResponse> obterHistoricoDoAluno(Long alunoId, Authentication auth) {
        validarAcessoAoAluno(alunoId, auth);
        return treinoRepository.findByAluno_IdAndAtivoFalseOrderByDtInclusaoDesc(alunoId)
                .stream().map(this::toDTO).collect(Collectors.toList());
    }

    @Override
    public TreinoResponse findById(Long id, Authentication auth) {
        User currentUser = getCurrentUser(auth);
        Treino treino = treinoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException(mensagens.get("treino.nao.encontrado")));

        if (currentUser.getRole() == UserRole.ALUNO
                && (treino.getAluno() == null || !treino.getAluno().getId().equals(currentUser.getId()))) {
            throw new AccessDeniedException(mensagens.get("treino.aluno.apenas.proprio"));
        }

        return toDTO(treino);
    }

    @Override
    public void ativar(Long id) {
        alterarStatus(id, true);
    }

    @Override
    public void desativar(Long id) {
        alterarStatus(id, false);
    }

    private void alterarStatus(Long id, boolean ativo) {
        Treino treino = treinoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException(mensagens.get("treino.nao.encontrado")));
        treino.setAtivo(ativo);
        treinoRepository.save(treino);
    }

    private void validarAcessoAoAluno(Long alunoId, Authentication auth) {
        User currentUser = getCurrentUser(auth);

        if (currentUser.getRole() == UserRole.ALUNO && !currentUser.getId().equals(alunoId)) {
            throw new AccessDeniedException(mensagens.get("treino.aluno.apenas.proprio"));
        }

        if (currentUser.getRole() == UserRole.PERSONAL) {
            personalRepository.findByUserId(currentUser.getId())
                    .orElseThrow(() -> new RuntimeException(mensagens.get("personal.nao.encontrado")));
        }
    }

    private List<ExercicioTreino> montarExercicios(
            List<ExercicioTreinoCreateRequest> requests, Treino treino, String nomeUsuario) {
        List<ExercicioTreino> exercicios = new ArrayList<>();
        if (requests == null) {
            return exercicios;
        }

        AtomicInteger ordemAuto = new AtomicInteger(1);
        for (ExercicioTreinoCreateRequest req : requests) {
            ExercicioTreino ex = new ExercicioTreino();
            ex.setNomeExercicio(req.getNomeExercicio());
            ex.setGrupoMuscular(req.getGrupoMuscular());
            ex.setSeries(req.getSeries());
            ex.setRepeticoes(req.getRepeticoes());
            ex.setCargaKg(req.getCargaKg());
            ex.setDescansoSegundos(req.getDescansoSegundos());
            ex.setOrdem(req.getOrdem() != null ? req.getOrdem() : ordemAuto.getAndIncrement());
            ex.setObservacao(req.getObservacao());
            ex.setTreino(treino);
            ex.setNmUsuario(nomeUsuario);
            exercicios.add(ex);
        }
        return exercicios;
    }

    private User getCurrentUser(Authentication auth) {
        if (auth == null || auth.getName() == null) {
            throw new AccessDeniedException(mensagens.get("auth.usuario.nao.autenticado"));
        }
        return userRepository.findByEmail(auth.getName())
                .orElseThrow(() -> new RuntimeException(mensagens.get("usuario.nao.encontrado")));
    }

    private TreinoResponse toDTO(Treino treino) {
        List<ExercicioTreinoResponse> exercicios = treino.getExercicios() == null
                ? List.of()
                : treino.getExercicios().stream()
                        .map(this::toExercicioDTO)
                        .collect(Collectors.toList());

        return new TreinoResponse(
                treino.getId(),
                treino.getNome(),
                treino.getDivisao(),
                treino.getObjetivo(),
                treino.getObservacoes(),
                treino.getAtivo(),
                treino.getValidadeAte(),
                treino.getAluno() != null ? treino.getAluno().getId() : null,
                treino.getAluno() != null ? treino.getAluno().getName() : null,
                treino.getPersonal() != null ? treino.getPersonal().getId() : null,
                treino.getPersonal() != null && treino.getPersonal().getUser() != null
                        ? treino.getPersonal().getUser().getName() : null,
                treino.getAcademia() != null ? treino.getAcademia().getId() : null,
                treino.getAcademia() != null ? treino.getAcademia().getName() : null,
                treino.getDtInclusao(),
                treino.getDtAlteracao(),
                exercicios
        );
    }

    private ExercicioTreinoResponse toExercicioDTO(ExercicioTreino ex) {
        return new ExercicioTreinoResponse(
                ex.getId(),
                ex.getNomeExercicio(),
                ex.getGrupoMuscular(),
                ex.getSeries(),
                ex.getRepeticoes(),
                ex.getCargaKg(),
                ex.getDescansoSegundos(),
                ex.getOrdem(),
                ex.getObservacao()
        );
    }
}
