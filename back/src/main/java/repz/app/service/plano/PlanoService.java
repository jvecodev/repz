package repz.app.service.plano;

import org.springframework.security.core.Authentication;
import repz.app.dto.request.PlanoPostRequest;
import repz.app.dto.request.PlanoPutRequest;
import repz.app.dto.response.PlanoResponse;

import java.util.List;

public interface PlanoService {

    void criar(PlanoPostRequest dto, Long academiaHeaderId, Authentication auth);

    List<PlanoResponse> findAll(Long academiaHeaderId, Authentication auth);

    PlanoResponse findById(Integer id, Long academiaHeaderId, Authentication auth);

    void atualizar(Integer id, PlanoPutRequest dto, Long academiaHeaderId, Authentication auth);

    void ativar(Integer id, Long academiaHeaderId, Authentication auth);

    void desativar(Integer id, Long academiaHeaderId, Authentication auth);
}
