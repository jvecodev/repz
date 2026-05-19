package repz.app.service.academia;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import repz.app.dto.request.AcademiaCreateRequest;
import repz.app.dto.request.AcademiaUpdateRequest;
import repz.app.dto.response.AcademiaDashboardResponse;
import repz.app.dto.response.AcademiaResponse;
import repz.app.message.Mensagens;
import repz.app.persistence.entity.Academia;
import repz.app.persistence.entity.User;
import repz.app.persistence.mapper.AcademiaMapper;
import repz.app.persistence.repository.AcademiaRepository;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class AcademiaServiceImpl implements AcademiaService {

    private final AcademiaRepository academiaRepository;
    private final AcademiaMapper academiaMapper;
    private final Mensagens mensagens;

    public AcademiaResponse criar(AcademiaCreateRequest dto) {
        if (academiaRepository.findByCnpj(dto.getCnpj()).isPresent()) {
            throw new IllegalArgumentException(mensagens.get("academia.cnpj.ja.existe"));
        }

        Academia academia = academiaMapper.toEntity(dto);
        Academia saved = academiaRepository.save(academia);
        return academiaMapper.toResponseDTO(saved);
    }

    @Transactional(readOnly = true)
    public List<AcademiaResponse> findAll() {
        return academiaRepository.findAll().stream()
                .map(academiaMapper::toResponseDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public AcademiaResponse findById(Long id) {
        Academia academia = academiaRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException(mensagens.get("academia.nao.encontrada")));
        return academiaMapper.toResponseDTO(academia);
    }

    public AcademiaResponse atualizar(Long id, AcademiaUpdateRequest dto) {
        Academia academia = academiaRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException(mensagens.get("academia.nao.encontrada")));

        if (!academia.getCnpj().equals(dto.getCnpj()) &&
                academiaRepository.findByCnpj(dto.getCnpj()).isPresent()) {
            throw new IllegalArgumentException(mensagens.get("academia.cnpj.duplicado"));
        }

        academia.setCnpj(dto.getCnpj());
        academiaMapper.updateEntity(dto, academia);
        Academia updated = academiaRepository.save(academia);
        return academiaMapper.toResponseDTO(updated);
    }

    public AcademiaResponse ativar(Long id) {
        return alterarStatus(id, true);
    }

    public AcademiaResponse desativar(Long id) {
        return alterarStatus(id, false);
    }

    private AcademiaResponse alterarStatus(Long id, boolean ativo) {
        Academia academia = academiaRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException(mensagens.get("academia.nao.encontrada")));

        academia.setActive(ativo);
        Academia updated = academiaRepository.save(academia);
        return academiaMapper.toResponseDTO(updated);
    }

    @Transactional(readOnly = true)
    public AcademiaResponse obterMinha(User currentUser) {
        List<Academia> academias = academiaRepository.findByResponsibleUserId(currentUser.getId());

        if (academias.isEmpty()) {
            throw new IllegalArgumentException(mensagens.get("academia.usuario.sem.academia.associada"));
        }

        return academiaMapper.toResponseDTO(academias.getFirst());
    }

    public AcademiaResponse atualizarMinha(User currentUser, AcademiaUpdateRequest dto) {
        List<Academia> academias = academiaRepository.findByResponsibleUserId(currentUser.getId());

        if (academias.isEmpty()) {
            throw new IllegalArgumentException(mensagens.get("academia.usuario.sem.academia.associada"));
        }

        Academia academia = academias.getFirst();

        if (!academia.getCnpj().equals(dto.getCnpj())) {
            throw new IllegalArgumentException(mensagens.get("academia.cnpj.alteracao.nao.permitida"));
        }

        academiaMapper.updateEntity(dto, academia);
        Academia updated = academiaRepository.save(academia);
        return academiaMapper.toResponseDTO(updated);
    }

    @Transactional(readOnly = true)
    public AcademiaDashboardResponse obterDashboard() {
        Object[] r = academiaRepository.dashboard().getFirst();

        return new AcademiaDashboardResponse(
                ((Number) r[0]).longValue(),
                ((Number) r[1]).intValue(),
                ((Number) r[2]).intValue(),
                ((Number) r[3]).intValue(),
                ((Number) r[4]).intValue(),
                ((Number) r[5]).doubleValue()
        );
    }
}
