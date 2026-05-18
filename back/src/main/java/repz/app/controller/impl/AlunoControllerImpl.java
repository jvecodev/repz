package repz.app.controller.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.RestController;
import repz.app.controller.AlunoController;
import repz.app.dto.request.AlunoMeUpdateRequest;
import repz.app.dto.request.AlunoUpdateRequest;
import repz.app.dto.response.AlunoDetalheResponse;
import repz.app.service.aluno.AlunoService;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class AlunoControllerImpl implements AlunoController {

    private final AlunoService alunoService;

    @Override
    public List<AlunoDetalheResponse> findAll(Long academiaId, Authentication auth) {
        return alunoService.findAll(academiaId, auth);
    }

    @Override
    public AlunoDetalheResponse obterMeuPerfil(Authentication auth) {
        return alunoService.obterMeuPerfil(auth);
    }

    @Override
    public AlunoDetalheResponse atualizarMeuPerfil(AlunoMeUpdateRequest request, Authentication auth) {
        return alunoService.atualizarMeuPerfil(request, auth);
    }

    @Override
    public AlunoDetalheResponse findById(Long id, Authentication auth) {
        return alunoService.findById(id, auth);
    }

    @Override
    public AlunoDetalheResponse atualizar(Long id, AlunoUpdateRequest request, Long academiaId, Authentication auth) {
        return alunoService.atualizar(id, request, academiaId, auth);
    }

    @Override
    public void inativar(Long id, Long academiaId, Authentication auth) {
        alunoService.inativar(id, academiaId, auth);
    }
}
