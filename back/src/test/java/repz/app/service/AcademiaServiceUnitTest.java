package repz.app.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import repz.app.dto.request.AcademiaCreateRequest;
import repz.app.dto.request.AcademiaUpdateRequest;
import repz.app.persistence.entity.Academia;
import repz.app.persistence.entity.User;
import repz.app.persistence.entity.UserRole;
import repz.app.message.Mensagens;
import repz.app.persistence.mapper.AcademiaMapper;
import repz.app.persistence.repository.AcademiaRepository;
import repz.app.service.academia.AcademiaServiceImpl;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static repz.app.unit.UnitTestData.academia;
import static repz.app.unit.UnitTestData.user;

@ExtendWith(MockitoExtension.class)
class AcademiaServiceUnitTest {

    @Mock
    private AcademiaRepository academiaRepository;

    @Mock
    private AcademiaMapper academiaMapper;

    @Mock
    private Mensagens mensagens;

    @InjectMocks
    private AcademiaServiceImpl service;

    @Test
    void criarRejeitaCnpjDuplicado() {
        AcademiaCreateRequest request = new AcademiaCreateRequest(
                "12345678000199", "Repz", "Rua 1", "Eduardo", "contato@repz.com", "11999999999");
        when(academiaRepository.findByCnpj(request.getCnpj()))
                .thenReturn(Optional.of(academia(1L, user(1L, UserRole.GERENTE))));

        assertThrows(IllegalArgumentException.class, () -> service.criar(request));

        verify(academiaRepository, never()).save(any());
    }

    @Test
    void dashboardCalculaTotaisDeAcademias() {
        when(academiaRepository.dashboard())
                .thenReturn(List.<Object[]>of(new Object[]{2L, 30, 3, 1, 1, 15.0}));

        var response = service.obterDashboard();

        assertThat(response.getTotalAcademies()).isEqualTo(2);
        assertThat(response.getTotalStudents()).isEqualTo(30);
        assertThat(response.getTotalInstructors()).isEqualTo(3);
        assertThat(response.getTotalActiveAcademies()).isEqualTo(1);
        assertThat(response.getTotalInactiveAcademies()).isEqualTo(1);
        assertThat(response.getAverageStudentsPerAcademy()).isEqualTo(15.0);
    }

    // ─── criar (extra) ────────────────────────────────────────────────────────

    @Test
    void criarAcademiaSucesso() {
        User u = repz.app.unit.UnitTestData.user(1L, repz.app.persistence.entity.UserRole.ADMIN);
        repz.app.persistence.entity.Academia entity = repz.app.unit.UnitTestData.academia(1L, u);
        var req = new repz.app.dto.request.AcademiaCreateRequest();
        req.setCnpj("12345678000100");

        when(academiaRepository.findByCnpj("12345678000100")).thenReturn(java.util.Optional.empty());
        when(academiaMapper.toEntity(req)).thenReturn(entity);
        when(academiaRepository.save(entity)).thenReturn(entity);
        when(academiaMapper.toResponseDTO(entity)).thenReturn(
                new repz.app.dto.response.AcademiaResponse(1L, "12345678000100", "G", "R", "R", null, null, true, 0, 0, null, null));

        assertThat(service.criar(req).getId()).isEqualTo(1L);
    }

    @Test
    void criarRejeitaCnpjDuplicadoViaRequest() {
        User u = repz.app.unit.UnitTestData.user(1L, repz.app.persistence.entity.UserRole.ADMIN);
        repz.app.persistence.entity.Academia existente = repz.app.unit.UnitTestData.academia(1L, u);
        var req = new repz.app.dto.request.AcademiaCreateRequest();
        req.setCnpj("99999999000199");

        when(academiaRepository.findByCnpj("99999999000199")).thenReturn(java.util.Optional.of(existente));
        when(mensagens.get(any())).thenReturn("cnpj ja existe");

        assertThrows(IllegalArgumentException.class, () -> service.criar(req));
    }

    // ─── findById ─────────────────────────────────────────────────────────────

    @Test
    void findByIdRetornaAcademia() {
        User u = repz.app.unit.UnitTestData.user(1L, repz.app.persistence.entity.UserRole.ADMIN);
        repz.app.persistence.entity.Academia entity = repz.app.unit.UnitTestData.academia(1L, u);
        when(academiaRepository.findById(1L)).thenReturn(java.util.Optional.of(entity));
        when(academiaMapper.toResponseDTO(entity)).thenReturn(
                new repz.app.dto.response.AcademiaResponse(1L, "12345678000100", "G", "R", "R", null, null, true, 0, 0, null, null));

        assertThat(service.findById(1L).getId()).isEqualTo(1L);
    }

    @Test
    void findByIdLancaExcecaoQuandoNaoExiste() {
        when(academiaRepository.findById(99L)).thenReturn(java.util.Optional.empty());
        when(mensagens.get(any())).thenReturn("nao encontrada");

        assertThrows(IllegalArgumentException.class, () -> service.findById(99L));
    }

    // ─── ativar / desativar ───────────────────────────────────────────────────

    @Test
    void ativarMudaStatusParaTrue() {
        User u = repz.app.unit.UnitTestData.user(1L, repz.app.persistence.entity.UserRole.ADMIN);
        repz.app.persistence.entity.Academia entity = repz.app.unit.UnitTestData.academia(1L, u);
        entity.setActive(false);

        when(academiaRepository.findById(1L)).thenReturn(java.util.Optional.of(entity));
        when(academiaRepository.save(entity)).thenReturn(entity);
        when(academiaMapper.toResponseDTO(entity)).thenReturn(
                new repz.app.dto.response.AcademiaResponse(1L, "12345678000100", "G", "R", "R", null, null, true, 0, 0, null, null));

        service.ativar(1L);
        assertThat(entity.getActive()).isTrue();
    }

    @Test
    void desativarMudaStatusParaFalse() {
        User u = repz.app.unit.UnitTestData.user(1L, repz.app.persistence.entity.UserRole.ADMIN);
        repz.app.persistence.entity.Academia entity = repz.app.unit.UnitTestData.academia(1L, u);

        when(academiaRepository.findById(1L)).thenReturn(java.util.Optional.of(entity));
        when(academiaRepository.save(entity)).thenReturn(entity);
        when(academiaMapper.toResponseDTO(entity)).thenReturn(
                new repz.app.dto.response.AcademiaResponse(1L, "12345678000100", "G", "R", "R", null, null, false, 0, 0, null, null));

        service.desativar(1L);
        assertThat(entity.getActive()).isFalse();
    }

    // ─── obterMinha ───────────────────────────────────────────────────────────

    @Test
    void gerenteObtemPropriaAcademia() {
        User gerente = repz.app.unit.UnitTestData.user(3L, repz.app.persistence.entity.UserRole.GERENTE);
        repz.app.persistence.entity.Academia entity = repz.app.unit.UnitTestData.academia(1L, gerente);

        when(academiaRepository.findByResponsibleUserId(gerente.getId())).thenReturn(java.util.List.of(entity));
        when(academiaMapper.toResponseDTO(entity)).thenReturn(
                new repz.app.dto.response.AcademiaResponse(1L, "12345678000100", "G", "R", "R", null, null, true, 0, 0, null, null));

        assertThat(service.obterMinha(gerente)).isNotNull();
    }

    @Test
    void obterMinhaLancaExcecaoQuandoSemAcademia() {
        User gerente = repz.app.unit.UnitTestData.user(3L, repz.app.persistence.entity.UserRole.GERENTE);
        when(academiaRepository.findByResponsibleUserId(gerente.getId())).thenReturn(java.util.List.of());
        when(mensagens.get(any())).thenReturn("sem academia");

        assertThrows(IllegalArgumentException.class, () -> service.obterMinha(gerente));
    }

    // ─── findAll ──────────────────────────────────────────────────────────────

    @Test
    void findAllRetornaTodas() {
        User u = repz.app.unit.UnitTestData.user(1L, repz.app.persistence.entity.UserRole.ADMIN);
        var lista = java.util.List.of(
                repz.app.unit.UnitTestData.academia(1L, u),
                repz.app.unit.UnitTestData.academia(2L, u));
        when(academiaRepository.findAll()).thenReturn(lista);
        when(academiaMapper.toResponseDTO(any())).thenReturn(
                new repz.app.dto.response.AcademiaResponse(1L, "12345678000100", "G", "R", "R", null, null, true, 0, 0, null, null));

        assertThat(service.findAll()).hasSize(2);
    }

    // ─── atualizarMinha ───────────────────────────────────────────────────────

    @Test
    void gerenteAtualizaPropriaAcademia() {
        User gerente = repz.app.unit.UnitTestData.user(3L, repz.app.persistence.entity.UserRole.GERENTE);
        repz.app.persistence.entity.Academia entity = repz.app.unit.UnitTestData.academia(1L, gerente);
        entity.setCnpj("12345678000100");

        var dto = new repz.app.dto.request.AcademiaUpdateRequest();
        dto.setCnpj("12345678000100");
        dto.setName("Academia Atualizada");

        when(academiaRepository.findByResponsibleUserId(gerente.getId())).thenReturn(java.util.List.of(entity));
        when(academiaRepository.save(entity)).thenReturn(entity);
        when(academiaMapper.toResponseDTO(entity)).thenReturn(
                new repz.app.dto.response.AcademiaResponse(1L, "12345678000100", "Academia Atualizada", "R", "R", null, null, true, 0, 0, null, null));

        var resp = service.atualizarMinha(gerente, dto);
        verify(academiaMapper).updateEntity(dto, entity);
        assertThat(resp.getName()).isEqualTo("Academia Atualizada");
    }

    @Test
    void atualizarMinhaNaoPermiteMudarCnpj() {
        User gerente = repz.app.unit.UnitTestData.user(3L, repz.app.persistence.entity.UserRole.GERENTE);
        repz.app.persistence.entity.Academia entity = repz.app.unit.UnitTestData.academia(1L, gerente);
        entity.setCnpj("11111111000100");

        var dto = new repz.app.dto.request.AcademiaUpdateRequest();
        dto.setCnpj("99999999000199");

        when(academiaRepository.findByResponsibleUserId(gerente.getId())).thenReturn(java.util.List.of(entity));
        when(mensagens.get(any())).thenReturn("cnpj nao pode mudar");

        assertThrows(IllegalArgumentException.class, () -> service.atualizarMinha(gerente, dto));
    }

    // ─── atualizar (id) ───────────────────────────────────────────────────────

    @Test
    void atualizarAcademiaPorIdSucesso() {
        User u = repz.app.unit.UnitTestData.user(1L, repz.app.persistence.entity.UserRole.ADMIN);
        repz.app.persistence.entity.Academia entity = repz.app.unit.UnitTestData.academia(1L, u);
        entity.setCnpj("12345678000100");

        var dto = new repz.app.dto.request.AcademiaUpdateRequest();
        dto.setCnpj("12345678000100");
        dto.setName("Nova Nome");

        when(academiaRepository.findById(1L)).thenReturn(java.util.Optional.of(entity));
        when(academiaRepository.save(entity)).thenReturn(entity);
        when(academiaMapper.toResponseDTO(entity)).thenReturn(
                new repz.app.dto.response.AcademiaResponse(1L, "12345678000100", "Nova Nome", "R", "R", null, null, true, 0, 0, null, null));

        var resp = service.atualizar(1L, dto);
        verify(academiaMapper).updateEntity(dto, entity);
        assertThat(resp.getName()).isEqualTo("Nova Nome");
    }
}
