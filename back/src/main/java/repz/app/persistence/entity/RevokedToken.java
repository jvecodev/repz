package repz.app.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Access token revogado por logout. Guarda apenas o identificador (jti) e o
 * momento em que o token expira, para que o registro possa ser limpo depois.
 */
@Entity
@Table(name = "revoked_token")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RevokedToken {

    @Id
    private String jti;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;
}
