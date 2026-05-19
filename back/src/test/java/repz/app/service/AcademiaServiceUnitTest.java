package repz.app.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import repz.app.dto.request.AcademiaCreateRequest;
import repz.app.persistence.entity.Academia;
import repz.app.persistence.entity.User;
import repz.app.persistence.entity.UserRole;
import repz.app.message.Mensagens;
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
}
