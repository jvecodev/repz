package repz.app.persistence.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import org.jspecify.annotations.NullMarked;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import repz.app.persistence.entity.common.AuditoriaBase;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "usuario")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class User extends AuditoriaBase implements UserDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "nome")
    private String name;

    @Column(unique = true)
    private String email;

    @Column(name = "senha")
    private String password;

    @Column(name = "ultimo_login")
    @JsonFormat(pattern = "dd/MM/yyyy HH:mm:ss")
    private LocalDateTime lastLogin;

    @Column(name = "perfil")
    @Enumerated(EnumType.STRING)
    private UserRole role;

    @Column(name = "ativo")
    private Boolean active = true;

    @Column(name = "data_delecao")
    private LocalDateTime deletedAt;

    @NonNull
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return switch (this.role) {
            case ADMIN -> List.of(new SimpleGrantedAuthority("ROLE_ADMIN"));
            case ACADEMIA -> List.of(new SimpleGrantedAuthority("ROLE_ACADEMIA"));
            case PERSONAL -> List.of(new SimpleGrantedAuthority("ROLE_PERSONAL"));
            case USUARIO -> List.of(new SimpleGrantedAuthority("ROLE_USUARIO"));
        };
    }

    @NullMarked
    @Override
    @JsonIgnore
    public String getUsername() {
        return email;
    }

    @Override
    @JsonIgnore
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    @JsonIgnore
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    @JsonIgnore
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    @JsonIgnore
    public boolean isEnabled() {
        return active;
    }
}
