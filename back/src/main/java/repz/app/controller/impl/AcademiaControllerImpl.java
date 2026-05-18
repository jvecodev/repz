package repz.app.controller.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.RestController;
import repz.app.controller.AcademiaController;
import repz.app.dto.request.AcademiaCreateRequest;
import repz.app.dto.request.AcademiaUpdateRequest;
import repz.app.dto.response.AcademiaDashboardResponse;
import repz.app.dto.response.AcademiaResponse;
import repz.app.message.Mensagens;
import repz.app.persistence.entity.User;
import repz.app.persistence.entity.UserRole;
import repz.app.service.academia.AcademiaService;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class AcademiaControllerImpl implements AcademiaController {

    private final AcademiaService academiaService;
    private final Mensagens mensagens;

    @Override
    public ResponseEntity<AcademiaResponse> criar(AcademiaCreateRequest dto) {
        validateAdminRole();
        AcademiaResponse response = academiaService.criar(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Override
    public ResponseEntity<List<AcademiaResponse>> findAll() {
        validateAdminRole();
        List<AcademiaResponse> academias = academiaService.findAll();
        return ResponseEntity.ok(academias);
    }

    @Override
    public ResponseEntity<AcademiaResponse> findById(Long id) {
        validateAdminRole();
        AcademiaResponse academia = academiaService.findById(id);
        return ResponseEntity.ok(academia);
    }

    @Override
    public ResponseEntity<AcademiaResponse> atualizar(Long id, AcademiaUpdateRequest dto) {
        validateAdminRole();
        AcademiaResponse updated = academiaService.atualizar(id, dto);
        return ResponseEntity.ok(updated);
    }

    @Override
    public ResponseEntity<AcademiaResponse> ativar(Long id) {
        validateAdminRole();
        AcademiaResponse activated = academiaService.ativar(id);
        return ResponseEntity.ok(activated);
    }

    @Override
    public ResponseEntity<AcademiaResponse> desativar(Long id) {
        validateAdminRole();
        AcademiaResponse deactivated = academiaService.desativar(id);
        return ResponseEntity.ok(deactivated);
    }

    @Override
    public ResponseEntity<AcademiaResponse> obterMinha() {
        User currentUser = getCurrentUser();
        validateAcademiaRole(currentUser);
        AcademiaResponse academia = academiaService.obterMinha(currentUser);
        return ResponseEntity.ok(academia);
    }

    @Override
    public ResponseEntity<AcademiaResponse> atualizarMinha(AcademiaUpdateRequest dto) {
        User currentUser = getCurrentUser();
        validateAcademiaRole(currentUser);
        AcademiaResponse updated = academiaService.atualizarMinha(currentUser, dto);
        return ResponseEntity.ok(updated);
    }

    @Override
    public ResponseEntity<AcademiaDashboardResponse> obterDashboard() {
        validateAdminRole();
        AcademiaDashboardResponse dashboard = academiaService.obterDashboard();
        return ResponseEntity.ok(dashboard);
    }


    private User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new IllegalArgumentException(mensagens.get("auth.usuario.nao.autenticado"));
        }
        return (User) authentication.getPrincipal();
    }

    private void validateAdminRole() {
        User currentUser = getCurrentUser();
        if (!currentUser.getRole().equals(UserRole.ADMIN)) {
            throw new org.springframework.security.access.AccessDeniedException(mensagens.get("auth.admin.necessario"));
        }
    }

    private void validateAcademiaRole(User user) {
        if (!user.getRole().equals(UserRole.GERENTE)) {
            throw new org.springframework.security.access.AccessDeniedException(mensagens.get("auth.academia.necessario"));
        }
    }
}
