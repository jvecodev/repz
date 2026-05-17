package repz.app.service.solicitacaoFicha;

import lombok.AllArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import repz.app.dto.request.SolicitacaoFichaCreateRequest;
import repz.app.dto.request.SolicitacaoFichaResponderRequest;
import repz.app.dto.response.SolicitacaoFichaResponse;
import repz.app.message.Mensagens;
import repz.app.persistence.entity.Personal;
import repz.app.persistence.entity.SolicitacaoFicha;
import repz.app.persistence.entity.SolicitacaoFichaStatus;
import repz.app.persistence.entity.User;
import repz.app.persistence.entity.UserRole;
import repz.app.persistence.repository.PersonalRepository;
import repz.app.persistence.repository.SolicitacaoFichaRepository;
import repz.app.persistence.repository.UserRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@AllArgsConstructor
public class SolicitacaoFichaServiceImpl implements SolicitacaoFichaService {

    private final SolicitacaoFichaRepository repository;
    private final UserRepository userRepository;
    private final PersonalRepository personalRepository;
    private final Mensagens mensagens;

    @Override
    public SolicitacaoFichaResponse criar(SolicitacaoFichaCreateRequest request, Authentication auth) {
        User aluno = userRepository.findByEmail(auth.getName())
                .orElseThrow(() -> new RuntimeException(mensagens.get("usuario.nao.encontrado")));

        if (aluno.getRole() != UserRole.USUARIO) {
            throw new RuntimeException(mensagens.get("solicitacao.apenas.aluno.cria"));
        }

        if (repository.existsByAluno_IdAndStatus(aluno.getId(), SolicitacaoFichaStatus.PENDENTE)) {
            throw new RuntimeException(mensagens.get("solicitacao.ja.pendente"));
        }

        Personal personal = null;
        if (request.getPersonalId() != null) {
            personal = personalRepository.findById(request.getPersonalId())
                    .orElseThrow(() -> new RuntimeException(mensagens.get("personal.nao.encontrado")));
        }

        SolicitacaoFicha solicitacao = new SolicitacaoFicha();
        solicitacao.setAluno(aluno);
        solicitacao.setPersonal(personal);
        solicitacao.setMensagem(request.getMensagem());
        solicitacao.setStatus(SolicitacaoFichaStatus.PENDENTE);
        solicitacao.setCriadaEm(LocalDateTime.now());

        return toDTO(repository.save(solicitacao));
    }

    @Override
    public SolicitacaoFichaResponse cancelar(Long id, Authentication auth) {
        User aluno = userRepository.findByEmail(auth.getName())
                .orElseThrow(() -> new RuntimeException(mensagens.get("usuario.nao.encontrado")));

        SolicitacaoFicha solicitacao = repository.findById(id)
                .orElseThrow(() -> new RuntimeException(mensagens.get("solicitacao.nao.encontrada")));

        if (!solicitacao.getAluno().getId().equals(aluno.getId())) {
            throw new RuntimeException(mensagens.get("solicitacao.acesso.negado"));
        }

        if (solicitacao.getStatus() != SolicitacaoFichaStatus.PENDENTE) {
            throw new RuntimeException(mensagens.get("solicitacao.nao.pode.cancelar"));
        }

        solicitacao.setStatus(SolicitacaoFichaStatus.CANCELADA);
        solicitacao.setRespondidaEm(LocalDateTime.now());
        return toDTO(repository.save(solicitacao));
    }

    @Override
    public List<SolicitacaoFichaResponse> listarMinhas(Authentication auth) {
        User aluno = userRepository.findByEmail(auth.getName())
                .orElseThrow(() -> new RuntimeException(mensagens.get("usuario.nao.encontrado")));
        return repository.findByAluno_IdOrderByCriadaEmDesc(aluno.getId())
                .stream().map(this::toDTO).collect(Collectors.toList());
    }

    @Override
    public SolicitacaoFichaResponse pendente(Authentication auth) {
        User aluno = userRepository.findByEmail(auth.getName())
                .orElseThrow(() -> new RuntimeException(mensagens.get("usuario.nao.encontrado")));
        return repository.findFirstByAluno_IdAndStatusOrderByCriadaEmDesc(aluno.getId(), SolicitacaoFichaStatus.PENDENTE)
                .map(this::toDTO)
                .orElse(null);
    }

    @Override
    public List<SolicitacaoFichaResponse> listarParaPersonal(SolicitacaoFichaStatus status, Authentication auth) {
        User user = userRepository.findByEmail(auth.getName())
                .orElseThrow(() -> new RuntimeException(mensagens.get("usuario.nao.encontrado")));

        if (user.getRole() != UserRole.PERSONAL) {
            throw new RuntimeException(mensagens.get("solicitacao.apenas.personal.visualiza"));
        }

        Personal personal = personalRepository.findAll().stream()
                .filter(p -> p.getUser().getId().equals(user.getId()))
                .findFirst()
                .orElseThrow(() -> new RuntimeException(mensagens.get("personal.nao.encontrado")));

        List<SolicitacaoFicha> lista = status != null
                ? repository.findByPersonal_IdAndStatusOrderByCriadaEmDesc(personal.getId(), status)
                : repository.findByPersonal_IdOrderByCriadaEmDesc(personal.getId());

        return lista.stream().map(this::toDTO).collect(Collectors.toList());
    }

    @Override
    public SolicitacaoFichaResponse responder(Long id, SolicitacaoFichaResponderRequest request, Authentication auth) {
        User user = userRepository.findByEmail(auth.getName())
                .orElseThrow(() -> new RuntimeException(mensagens.get("usuario.nao.encontrado")));

        if (user.getRole() != UserRole.PERSONAL) {
            throw new RuntimeException(mensagens.get("solicitacao.apenas.personal.responde"));
        }

        SolicitacaoFicha solicitacao = repository.findById(id)
                .orElseThrow(() -> new RuntimeException(mensagens.get("solicitacao.nao.encontrada")));

        Personal personal = personalRepository.findAll().stream()
                .filter(p -> p.getUser().getId().equals(user.getId()))
                .findFirst()
                .orElseThrow(() -> new RuntimeException(mensagens.get("personal.nao.encontrado")));

        if (solicitacao.getPersonal() != null && !solicitacao.getPersonal().getId().equals(personal.getId())) {
            throw new RuntimeException(mensagens.get("solicitacao.acesso.negado"));
        }

        if (solicitacao.getStatus() != SolicitacaoFichaStatus.PENDENTE) {
            throw new RuntimeException(mensagens.get("solicitacao.ja.respondida"));
        }

        SolicitacaoFichaStatus novoStatus = request.getStatus();
        if (novoStatus != SolicitacaoFichaStatus.APROVADA && novoStatus != SolicitacaoFichaStatus.REJEITADA) {
            throw new RuntimeException(mensagens.get("solicitacao.status.invalido"));
        }

        solicitacao.setStatus(novoStatus);
        solicitacao.setResposta(request.getResposta());
        solicitacao.setRespondidaEm(LocalDateTime.now());
        // Vincula o personal caso a solicitação foi enviada sem personalId
        if (solicitacao.getPersonal() == null) {
            solicitacao.setPersonal(personal);
        }

        return toDTO(repository.save(solicitacao));
    }

    private SolicitacaoFichaResponse toDTO(SolicitacaoFicha s) {
        return new SolicitacaoFichaResponse(
                s.getId(),
                s.getAluno().getId(),
                s.getAluno().getName(),
                s.getPersonal() != null ? s.getPersonal().getId() : null,
                s.getPersonal() != null ? s.getPersonal().getUser().getName() : null,
                s.getMensagem(),
                s.getStatus(),
                s.getResposta(),
                s.getCriadaEm(),
                s.getRespondidaEm()
        );
    }
}
