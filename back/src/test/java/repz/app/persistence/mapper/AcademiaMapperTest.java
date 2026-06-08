package repz.app.persistence.mapper;

import org.junit.jupiter.api.Test;
import repz.app.dto.request.AcademiaCreateRequest;
import repz.app.dto.request.AcademiaUpdateRequest;
import repz.app.persistence.entity.Academia;
import repz.app.persistence.entity.User;
import repz.app.persistence.entity.UserRole;

import static org.assertj.core.api.Assertions.assertThat;
import static repz.app.unit.UnitTestData.academia;
import static repz.app.unit.UnitTestData.user;

class AcademiaMapperTest {

    private final AcademiaMapper mapper = new AcademiaMapper();

    @Test
    void toResponseDTOMapeiaCorretamente() {
        User u = user(1L, UserRole.GERENTE);
        Academia entity = academia(10L, u);
        entity.setCnpj("12345678000100");
        entity.setPhone("11999999999");
        entity.setEmail("g@repz.com");
        entity.setTotalStudents(5);
        entity.setTotalInstructors(2);

        var resp = mapper.toResponseDTO(entity);

        assertThat(resp.getId()).isEqualTo(10L);
        assertThat(resp.getCnpj()).isEqualTo("12345678000100");
        assertThat(resp.getName()).isEqualTo("Academia 10");
        assertThat(resp.getPhone()).isEqualTo("11999999999");
        assertThat(resp.getEmail()).isEqualTo("g@repz.com");
        assertThat(resp.getActive()).isTrue();
        assertThat(resp.getTotalStudents()).isEqualTo(5);
        assertThat(resp.getTotalInstructors()).isEqualTo(2);
    }

    @Test
    void toResponseDTOComEntityNulaRetornaNull() {
        assertThat(mapper.toResponseDTO(null)).isNull();
    }

    @Test
    void toEntityMapeiaCorretamente() {
        var req = new AcademiaCreateRequest();
        req.setCnpj("12345678000100");
        req.setName("Nova Academia");
        req.setAddress("Rua X, 100");
        req.setResponsible("João");
        req.setEmail("j@repz.com");
        req.setPhone("11999999999");

        Academia entity = mapper.toEntity(req);

        assertThat(entity.getCnpj()).isEqualTo("12345678000100");
        assertThat(entity.getName()).isEqualTo("Nova Academia");
        assertThat(entity.getAddress()).isEqualTo("Rua X, 100");
        assertThat(entity.getResponsible()).isEqualTo("João");
        assertThat(entity.getActive()).isTrue();
        assertThat(entity.getTotalStudents()).isEqualTo(0);
        assertThat(entity.getTotalInstructors()).isEqualTo(0);
    }

    @Test
    void toEntityComRequestNulaRetornaNull() {
        assertThat(mapper.toEntity(null)).isNull();
    }

    @Test
    void updateEntityAtualizaCampos() {
        User u = user(1L, UserRole.GERENTE);
        Academia entity = academia(10L, u);
        entity.setCnpj("12345678000100");

        var dto = new AcademiaUpdateRequest();
        dto.setCnpj("12345678000100");
        dto.setName("Academia Atualizada");
        dto.setAddress("Rua Nova, 200");
        dto.setResponsible("Maria");
        dto.setEmail("m@repz.com");
        dto.setPhone("11888888888");

        mapper.updateEntity(dto, entity);

        assertThat(entity.getName()).isEqualTo("Academia Atualizada");
        assertThat(entity.getAddress()).isEqualTo("Rua Nova, 200");
        assertThat(entity.getResponsible()).isEqualTo("Maria");
        assertThat(entity.getEmail()).isEqualTo("m@repz.com");
        assertThat(entity.getPhone()).isEqualTo("11888888888");
    }

    @Test
    void updateEntityComArgumentosNulosNaoLancaExcecao() {
        User u = user(1L, UserRole.GERENTE);
        Academia entity = academia(10L, u);

        mapper.updateEntity(null, entity);   // dto nulo
        mapper.updateEntity(new AcademiaUpdateRequest(), null); // entity nula
        // sem exceção
    }
}
