package repz.app.controller.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.RestController;
import repz.app.controller.PersonalController;
import repz.app.dto.request.PersonalUpdateRequest;
import repz.app.dto.response.PersonalAlunosResponse;
import repz.app.dto.response.PersonalResponse;
import repz.app.service.personal.PersonalService;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class PersonalControllerImpl implements PersonalController {

    private final PersonalService personalService;

    @Override
    public List<PersonalResponse> findAll(Long academiaId, Authentication auth) {
        return personalService.findAll(academiaId, auth);
    }

    @Override
    public PersonalResponse findById(Long id) {
        return personalService.findById(id);
    }

    @Override
    public PersonalResponse atualizar(Long id, PersonalUpdateRequest request, Long academiaId, Authentication auth) {
        return personalService.atualizar(id, request, academiaId, auth);
    }

    @Override
    public PersonalResponse ativar(Long id, Long academiaId, Authentication auth) {
        return personalService.ativar(id, academiaId, auth);
    }

    @Override
    public PersonalResponse desativar(Long id, Long academiaId, Authentication auth) {
        return personalService.desativar(id, academiaId, auth);
    }

    @Override
    public PersonalResponse obterMeuPerfil(Authentication auth) {
        return personalService.obterMeuPerfil(auth);
    }

    @Override
    public PersonalResponse atualizarMeuPerfil(PersonalUpdateRequest request, Authentication auth) {
        return personalService.atualizarMeuPerfil(request, auth);
    }

    @Override
    public PersonalAlunosResponse obterMeusAlunos(Authentication auth) {
        return personalService.obterMeusAlunos(auth);
    }

}
