package kr.banklearning.operations.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import java.time.Clock;
import javax.sql.DataSource;
import kr.banklearning.operations.repository.JdbcProfileRepository;
import kr.banklearning.operations.repository.ProfileRepository;
import kr.banklearning.operations.service.AuditService;
import kr.banklearning.operations.service.ProfileService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@Configuration
@EnableTransactionManagement(proxyTargetClass = true)
public class AppConfig {
    @Bean(destroyMethod = "close")
    public HikariDataSource dataSource() {
        try {
            HikariConfig config = new HikariConfig();
            config.setDriverClassName("org.postgresql.Driver");
            config.setJdbcUrl(env("DB_URL"));
            config.setUsername(env("DB_USER"));
            config.setPassword(env("DB_PASSWORD"));
            config.setMaximumPoolSize(5);
            config.setMinimumIdle(0);
            config.setConnectionTimeout(3000);
            config.setValidationTimeout(1000);
            config.setInitializationFailTimeout(-1);
            config.setPoolName("phase5-pool");
            config.addDataSourceProperty("connectTimeout", "3");
            config.addDataSourceProperty("socketTimeout", "5");
            return new HikariDataSource(config);
        } catch (RuntimeException exception) {
            throw new IllegalStateException("학습용 DB 환경변수를 확인하세요.");
        }
    }

    @Bean
    public ProfileRepository profileRepository(DataSource source) {
        return new JdbcProfileRepository(source);
    }

    @Bean
    public DataSourceTransactionManager transactionManager(DataSource source) {
        return new DataSourceTransactionManager(source);
    }

    @Bean
    public Clock clock() {
        return Clock.systemUTC();
    }

    @Bean
    public AuditService auditService(
            ProfileRepository repository, Clock clock, PlatformTransactionManager manager) {
        return new AuditService(repository, clock, manager);
    }

    @Bean
    public ProfileService profileService(ProfileRepository repository, AuditService audit) {
        return new ProfileService(repository, audit);
    }

    private static String env(String name) {
        String value = System.getenv(name);
        if (value == null || value.isBlank()) throw new IllegalStateException("필수 환경변수가 없습니다.");
        return value;
    }
}
