package repz.app.controller.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.RestController;
import repz.app.controller.PlanoController;
import repz.app.dto.request.PlanoPostRequest;
import repz.app.dto.request.PlanoPutRequest;
import repz.app.dto.response.PlanoResponse;
import repz.app.service.plano.PlanoService;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class PlanoControllerImpl implements PlanoController {

    private final PlanoService planoService;

    @Override
    public ResponseEntity<Void> criar(PlanoPostRequest dto, Long academiaId, Authentication auth) {
        planoService.criar(dto, academiaId, auth);
        return ResponseEntity.status(201).build();
    }

    @Override
    public ResponseEntity<List<PlanoResponse>> findAll(Long academiaId, Authentication auth) {
        return ResponseEntity.ok(planoService.findAll(academiaId, auth));
    }

    @Override
    public ResponseEntity<PlanoResponse> findById(Integer id, Long academiaId, Authentication auth) {
        return ResponseEntity.ok(planoService.findById(id, academiaId, auth));
    }

    @Override
    public ResponseEntity<Void> atualizar(Integer id, PlanoPutRequest dto, Long academiaId, Authentication auth) {
        planoService.atualizar(id, dto, academiaId, auth);
        return ResponseEntity.ok().build();
    }

    @Override
    public ResponseEntity<Void> ativar(Integer id, Long academiaId, Authentication auth) {
        planoService.ativar(id, academiaId, auth);
        return ResponseEntity.ok().build();
    }

    @Override
    public ResponseEntity<Void> desativar(Integer id, Long academiaId, Authentication auth) {
        planoService.desativar(id, academiaId, auth);
        return ResponseEntity.ok().build();
    }
}
