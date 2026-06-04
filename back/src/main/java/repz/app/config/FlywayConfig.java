package repz.app.config;

import org.flywaydb.core.Flyway;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;

@Configuration
public class FlywayConfig {

    /**
     * Os seeds (db/seed) usam DDL/PLpgSQL específico do PostgreSQL e não rodam
     * no H2 dos testes. Por isso ficam numa location separada, habilitada apenas
     * fora do ambiente de teste (app.seed.enabled=false em src/test).
     */
    @Value("${app.seed.enabled:true}")
    private boolean seedEnabled;

    @Bean(initMethod = "migrate")
    public Flyway flyway(DataSource dataSource) {
        String[] locations = seedEnabled
                ? new String[]{"classpath:db/migration", "classpath:db/seed"}
                : new String[]{"classpath:db/migration"};

        return Flyway.configure()
                .dataSource(dataSource)
                .locations(locations)
                .baselineOnMigrate(true)
                .baselineVersion("0")
                .outOfOrder(true)
                .load();
    }
}