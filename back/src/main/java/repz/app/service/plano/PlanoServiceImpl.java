package repz.app.service.plano;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import repz.app.dto.request.PlanoPostRequest;
import repz.app.dto.request.PlanoPutRequest;
import repz.app.dto.response.PlanoResponse;
import repz.app.message.Mensagens;
import repz.app.persistence.entity.Academia;
import repz.app.persistence.entity.Plano;
import repz.app.persistence.repository.AcademiaRepository;
import repz.app.persistence.repository.PlanoRepository;
import repz.app.service.academia.AcademiaContextService;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PlanoServiceImpl implements PlanoService {

    private final PlanoRepository planoRepository;
    private final AcademiaRepository academiaRepository;
    private final AcademiaContextService academiaContextService;
    private final Mensagens mensagens;

    @Override
    public void criar(PlanoPostRequest dto, Long academiaHeaderId, Authentication auth) {

        Academia academia = resolveRequiredAcademia(academiaHeaderId, auth);

        Plano plano = Plano.builder()
                .nome(dto.nome())
                .duracaoDias(dto.duracaoDias())
                .valor(dto.valor())
                .ativo(true)
                .academia(academia)
                .build();

        planoRepository.save(plano);
    }

    @Override
    public List<PlanoResponse> findAll(Long academiaHeaderId, Authentication auth) {

        Long academiaId = academiaContextService.resolveOptional(auth, academiaHeaderId);

        List<Plano> planos = academiaId == null
                ? planoRepository.findAll()
                : planoRepository.findByAcademiaId(academiaId);

        return planos
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    public PlanoResponse findById(Integer id, Long academiaHeaderId, Authentication auth) {
        Plano plano = findPlanoInScope(id, academiaHeaderId, auth);

        return toResponse(plano);
    }

    @Override
    public void atualizar(Integer id, PlanoPutRequest dto, Long academiaHeaderId, Authentication auth) {

        Plano plano = findPlanoInScope(id, academiaHeaderId, auth);

        plano.setNome(dto.nome());
        plano.setDuracaoDias(dto.duracaoDias());
        plano.setValor(dto.valor());

        planoRepository.save(plano);
    }

    @Override
    public void ativar(Integer id, Long academiaHeaderId, Authentication auth) {
        alterarStatus(id, academiaHeaderId, auth, true);
    }

    @Override
    public void desativar(Integer id, Long academiaHeaderId, Authentication auth) {
        alterarStatus(id, academiaHeaderId, auth, false);
    }

    private void alterarStatus(Integer id, Long academiaHeaderId, Authentication auth, boolean ativo) {

        Plano plano = findPlanoInScope(id, academiaHeaderId, auth);

        plano.setAtivo(ativo);

        planoRepository.save(plano);
    }

    private Academia resolveRequiredAcademia(Long academiaHeaderId, Authentication auth) {
        Long academiaId = academiaContextService.resolveRequired(auth, academiaHeaderId);
        return academiaRepository.findById(academiaId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, mensagens.get("academia.nao.encontrada")));
    }

    private Plano findPlanoInScope(Integer id, Long academiaHeaderId, Authentication auth) {
        Long academiaId = academiaContextService.resolveOptional(auth, academiaHeaderId);
        if (academiaId == null) {
            return planoRepository.findById(id)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, mensagens.get("plano.nao.encontrado")));
        }
        return planoRepository.findByIdAndAcademiaId(id, academiaId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, mensagens.get("plano.nao.encontrado")));
    }

    private PlanoResponse toResponse(Plano plano) {
        return new PlanoResponse(
                plano.getId(),
                plano.getNome(),
                plano.getDuracaoDias(),
                plano.getValor(),
                plano.getAtivo());
    }
}
