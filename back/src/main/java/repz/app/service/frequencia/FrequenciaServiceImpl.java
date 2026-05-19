package repz.app.service.frequencia;

import lombok.AllArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import repz.app.dto.request.FrequenciaCreateRequest;
import repz.app.dto.response.AlunoInativoResponse;
import repz.app.dto.response.FrequenciaRelatorioResponse;
import repz.app.dto.response.FrequenciaResponse;
import repz.app.message.Mensagens;
import repz.app.persistence.entity.Academia;
import repz.app.persistence.entity.Frequencia;
import repz.app.persistence.entity.Personal;
import repz.app.persistence.entity.User;
import repz.app.persistence.entity.UserRole;
import repz.app.persistence.repository.AcademiaRepository;
import repz.app.persistence.repository.FrequenciaRepository;
import repz.app.persistence.repository.PersonalRepository;
import repz.app.persistence.repository.UserRepository;
import repz.app.service.academia.AcademiaContextService;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@AllArgsConstructor
@Service
public class FrequenciaServiceImpl implements FrequenciaService {
    private final FrequenciaRepository frequenciaRepository;
    private final UserRepository userRepository;
    private final AcademiaRepository academiaRepository;
    private final PersonalRepository personalRepository;
    private final AcademiaContextService academiaContextService;
    private final Mensagens mensagens;

    public FrequenciaResponse criar(FrequenciaCreateRequest request, Long academiaHeaderId, Authentication auth) {
        User currentUser = userRepository.findByEmail(auth.getName())
                .orElseThrow(() -> new RuntimeException(mensagens.get("usuario.nao.encontrado")));
        Long academiaId = resolveAcademiaFromRequest(request.getAcademiaId(), academiaHeaderId, auth);

        if (currentUser.getRole() == UserRole.ALUNO) {
            if (!currentUser.getId().equals(request.getAlunoId())) {
                throw new RuntimeException(mensagens.get("frequencia.aluno.apenas.propria"));
            }
        } else if (currentUser.getRole() != UserRole.PERSONAL) {
            throw new RuntimeException(mensagens.get("frequencia.perfil.nao.autorizado.registrar"));
        }

        User aluno = userRepository.findById(Math.toIntExact(request.getAlunoId()))
                .orElseThrow(() -> new RuntimeException(mensagens.get("aluno.nao.encontrado")));
        Academia academia = academiaRepository.findById(academiaId)
                .orElseThrow(() -> new RuntimeException(mensagens.get("academia.nao.encontrada")));
        Personal registradoPor = null;
        if (request.getPersonalId() != null) {
            registradoPor = personalRepository.findById(request.getPersonalId())
                    .orElseThrow(() -> new RuntimeException(mensagens.get("personal.nao.encontrado")));
        }

        Frequencia frequencia = new Frequencia();
        frequencia.setAluno(aluno);
        frequencia.setAcademia(academia);
        frequencia.setRegistradoPor(registradoPor);
        frequencia.setDataHora(request.getDataHora() != null ? request.getDataHora() : LocalDateTime.now());

        Frequencia saved = frequenciaRepository.save(frequencia);
        return toDTO(saved);
    }

    public List<FrequenciaResponse> filtrarPorPeriodo(
            Long alunoId,
            Long academiaId,
            LocalDateTime inicio,
            LocalDateTime fim,
            Authentication auth) {
        Long resolvedAcademiaId = academiaContextService.resolveOptional(auth, academiaId);
        List<Frequencia> frequencias = frequenciaRepository.findByAlunoIdAndPeriodo(alunoId, inicio, fim);
        return frequencias.stream()
                .filter(f -> resolvedAcademiaId == null || f.getAcademia().getId().equals(resolvedAcademiaId))
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    public List<FrequenciaResponse> filtrarPorAcademiaEPeriodo(
            Long academiaId,
            LocalDateTime inicio,
            LocalDateTime fim,
            Authentication auth) {
        Long resolvedAcademiaId = academiaContextService.resolveRequired(auth, academiaId);
        List<Frequencia> frequencias = frequenciaRepository.findByAcademiaIdAndPeriodo(resolvedAcademiaId, inicio, fim);
        return frequencias.stream().map(this::toDTO).collect(Collectors.toList());
    }

    public FrequenciaResponse findById(Long id) {
        Frequencia frequencia = frequenciaRepository.findById(id)
                .orElseThrow(() -> new RuntimeException(mensagens.get("frequencia.nao.encontrada")));

        return toDTO(frequencia);
    }

    public List<FrequenciaResponse> meuHistorico(Long alunoId) {
        List<Frequencia> frequencias = frequenciaRepository.findByAluno_IdOrderByDataHoraDesc(alunoId);
        return frequencias.stream().map(this::toDTO).collect(Collectors.toList());
    }

    public List<AlunoInativoResponse> obterAlunosInativos(Long academiaId, Authentication auth) {
        Long resolvedAcademiaId = academiaContextService.resolveRequired(auth, academiaId);
        academiaRepository.findById(resolvedAcademiaId)
                .orElseThrow(() -> new RuntimeException(mensagens.get("academia.nao.encontrada")));

        return frequenciaRepository.alunosInativos(resolvedAcademiaId, 7).stream()
                .map(r -> new AlunoInativoResponse(
                        ((Number) r[0]).longValue(),
                        (String) r[1],
                        (String) r[2],
                        ((Number) r[3]).longValue(),
                        (Boolean) r[4]
                ))
                .collect(Collectors.toList());
    }

    public FrequenciaRelatorioResponse obterRelatorio(Long academiaId, LocalDateTime inicio, LocalDateTime fim, Authentication auth) {
        String email = ((org.springframework.security.core.userdetails.UserDetails) Objects.requireNonNull(auth.getPrincipal())).getUsername();
        User currentUser = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException(mensagens.get("usuario.nao.encontrado")));

        if (currentUser.getRole() != UserRole.GERENTE && currentUser.getRole() != UserRole.ADMIN) {
            throw new RuntimeException(mensagens.get("frequencia.acesso.relatorio.negado"));
        }

        Long resolvedAcademiaId = academiaContextService.resolveRequired(auth, academiaId);

        Map<String, Long> frequenciaPorAluno = new LinkedHashMap<>();
        long total = 0;
        for (Object[] row : frequenciaRepository.relatorioFrequencia(resolvedAcademiaId, inicio, fim)) {
            long count = ((Number) row[1]).longValue();
            frequenciaPorAluno.put((String) row[0], count);
            total += count;
        }

        FrequenciaRelatorioResponse response = new FrequenciaRelatorioResponse();
        response.setAcademiaId(resolvedAcademiaId);
        response.setPeriodo(java.util.Map.of("inicio", inicio, "fim", fim));
        response.setTotalFrequencias(total);
        response.setFrequenciaPorAluno(frequenciaPorAluno);

        return response;
    }

    public void ativar(Long id) {
        alterarStatus(id, true);
    }

    public void desativar(Long id) {
        alterarStatus(id, false);
    }

    private void alterarStatus(Long id, boolean ativo) {
        Frequencia frequencia = frequenciaRepository.findById(id)
                .orElseThrow(() -> new RuntimeException(mensagens.get("frequencia.nao.encontrada")));

        frequencia.setAtivo(ativo);
        frequenciaRepository.save(frequencia);
    }

    private Long resolveAcademiaFromRequest(Long requestAcademiaId, Long headerAcademiaId, Authentication auth) {
        Long requestedAcademiaId = headerAcademiaId != null ? headerAcademiaId : requestAcademiaId;
        Long resolvedAcademiaId = academiaContextService.resolveRequired(auth, requestedAcademiaId);

        if (requestAcademiaId != null && !requestAcademiaId.equals(resolvedAcademiaId)) {
            throw new RuntimeException(mensagens.get("frequencia.academia.corpo.difere"));
        }

        return resolvedAcademiaId;
    }


    private FrequenciaResponse toDTO(Frequencia frequencia) {
        return new FrequenciaResponse(
                frequencia.getId(),
                frequencia.getDataHora(),
                frequencia.getAluno().getId(),
                frequencia.getAluno().getName(),
                frequencia.getAcademia().getId(),
                frequencia.getAcademia().getName(),
                frequencia.getRegistradoPor() != null ? frequencia.getRegistradoPor().getUser().getId() : null,
                frequencia.getRegistradoPor() != null ? frequencia.getRegistradoPor().getUser().getName() : null
        );
    }
}
