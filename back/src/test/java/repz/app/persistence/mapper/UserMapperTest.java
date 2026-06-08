package repz.app.persistence.mapper;

import org.junit.jupiter.api.Test;
import repz.app.persistence.entity.User;
import repz.app.persistence.entity.UserRole;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class UserMapperTest {

    private final UserMapper mapper = new UserMapper();

    @Test
    void toResponseMapeiaCorretamente() {
        User user = new User();
        user.setId(1L);
        user.setName("João");
        user.setEmail("joao@repz.com");
        user.setRole(UserRole.ALUNO);
        user.setActive(true);
        user.setLastLogin(LocalDateTime.of(2026, 6, 1, 10, 0));
        user.setFotoUrl("https://cdn.repz.com/foto.jpg");

        var resp = mapper.toResponse(user);

        assertThat(resp.id()).isEqualTo(1L);
        assertThat(resp.name()).isEqualTo("João");
        assertThat(resp.email()).isEqualTo("joao@repz.com");
        assertThat(resp.role()).isEqualTo(UserRole.ALUNO);
        assertThat(resp.active()).isTrue();
        assertThat(resp.lastLogin()).isEqualTo(LocalDateTime.of(2026, 6, 1, 10, 0));
        assertThat(resp.fotoUrl()).isEqualTo("https://cdn.repz.com/foto.jpg");
    }

    @Test
    void toResponseComCamposNulos() {
        User user = new User();
        user.setId(2L);
        user.setName("Maria");
        user.setEmail("maria@repz.com");
        user.setRole(UserRole.PERSONAL);
        user.setActive(false);

        var resp = mapper.toResponse(user);

        assertThat(resp.id()).isEqualTo(2L);
        assertThat(resp.lastLogin()).isNull();
        assertThat(resp.fotoUrl()).isNull();
        assertThat(resp.active()).isFalse();
    }

    @Test
    void toResponsePreservaRole() {
        for (UserRole role : UserRole.values()) {
            User user = new User();
            user.setId(1L);
            user.setName("Test");
            user.setEmail("t@r.com");
            user.setRole(role);
            user.setActive(true);

            assertThat(mapper.toResponse(user).role()).isEqualTo(role);
        }
    }
}
