package repz.app.controller.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.RestController;
import repz.app.controller.UserController;
import repz.app.dto.request.AdminCreateRequest;
import repz.app.dto.request.UserCreateRequest;
import repz.app.dto.request.UserPutRequest;
import repz.app.dto.request.UserSelfUpdateRequest;
import repz.app.dto.response.UserGetResponse;
import repz.app.service.user.UserService;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class UserControllerImpl implements UserController {

    private final UserService userService;

    @Override
    public ResponseEntity<List<UserGetResponse>> findAll() {
        return ResponseEntity.ok(userService.findAll());
    }

    @Override
    public ResponseEntity<UserGetResponse> findById(Integer id) {
        return ResponseEntity.ok(userService.findById(id));
    }

    @Override
    public ResponseEntity<Void> criar(UserCreateRequest userCreateRequest, Authentication authentication) {
        userService.criarUsuario(userCreateRequest, authentication);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @Override
    public ResponseEntity<Void> criarAdmin(AdminCreateRequest adminCreateRequest) {
        userService.criarAdmin(adminCreateRequest);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @Override
    public ResponseEntity<UserGetResponse> obterMeuPerfil(Authentication authentication) {
        return ResponseEntity.ok(userService.obterMeuPerfil(authentication));
    }

    @Override
    public ResponseEntity<Void> atualizarMeuPerfil(UserSelfUpdateRequest userSelfUpdateRequest,
                                                   Authentication authentication) {
        userService.atualizarMeuPerfil(userSelfUpdateRequest, authentication);
        return ResponseEntity.ok().build();
    }

    @Override
    public ResponseEntity<Void> atualizar(Integer id, UserPutRequest userPutRequest) {
        userService.atualizar(id, userPutRequest);
        return ResponseEntity.ok().build();
    }

    @Override
    public ResponseEntity<Void> ativar(Integer id) {
        userService.ativar(id);
        return ResponseEntity.ok().build();
    }

    @Override
    public ResponseEntity<Void> desativar(Integer id) {
        userService.desativar(id);
        return ResponseEntity.ok().build();
    }
}
