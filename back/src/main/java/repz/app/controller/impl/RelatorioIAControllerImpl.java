package repz.app.controller.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.RestController;
import repz.app.controller.RelatorioIAController;
import repz.app.dto.response.RelatorioIAResponse;
import repz.app.service.relatorio.RelatorioIAService;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class RelatorioIAControllerImpl implements RelatorioIAController {

    private final RelatorioIAService relatorioIAService;

    @Override
    public RelatorioIAResponse iniciar(Long alunoId, Authentication auth) {
        return relatorioIAService.iniciar(alunoId, auth);
    }

    @Override
    public RelatorioIAResponse buscar(Long id) {
        return relatorioIAService.buscar(id);
    }

    @Override
    public List<RelatorioIAResponse> listar(Long alunoId) {
        return relatorioIAService.listar(alunoId);
    }

    @Override
    public void cancelar(Long id) {
        relatorioIAService.cancelar(id);
    }
}
