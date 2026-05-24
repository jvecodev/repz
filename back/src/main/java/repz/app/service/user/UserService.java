package repz.app.service.user;

import org.springframework.security.core.Authentication;
import repz.app.dto.request.AdminCreateRequest;
import repz.app.dto.request.UserCreateRequest;
import repz.app.dto.request.UserPutRequest;
import repz.app.dto.request.UserSelfUpdateRequest;
import repz.app.dto.response.UserGetResponse;

import java.util.List;

public interface UserService {
    List<UserGetResponse> findAll();

    UserGetResponse findById(Integer id);

    void updateLastLogin(String email);

    void criarUsuario(UserCreateRequest dto, Authentication auth);

    void criarAdmin(AdminCreateRequest dto);

    void atualizar(Integer id, UserPutRequest userPutRequest);

    UserGetResponse obterMeuPerfil(Authentication auth);

    void atualizarMeuPerfil(UserSelfUpdateRequest request, Authentication auth);

    void desativar(Integer id);

    void ativar(Integer id);
}
