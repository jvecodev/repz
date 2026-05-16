package repz.app.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import repz.app.persistence.entity.common.AuditoriaBase;

import java.math.BigDecimal;

@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "exercicio_treino")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ExercicioTreino extends AuditoriaBase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "nome_exercicio", length = 100)
    private String nomeExercicio;

    @Column(name = "grupo_muscular", length = 100)
    private String grupoMuscular;

    @Column(name = "series")
    private Integer series;

    @Column(name = "repeticoes", length = 50)
    private String repeticoes;

    @Column(name = "carga_kg", precision = 10, scale = 2)
    private BigDecimal cargaKg;

    @Column(name = "descanso_segundos")
    private Integer descansoSegundos;

    @Column(name = "ordem")
    private Integer ordem;

    @Column(name = "observacao", length = 500)
    private String observacao;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_treino", nullable = false)
    private Treino treino;
}
