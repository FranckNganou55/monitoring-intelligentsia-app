package org.intelligentsia.monitoring.config;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;
import java.util.HashMap;
import java.util.Map;

@Configuration
@ConditionalOnProperty(name = "app.svfe.enabled", havingValue = "true")
@EnableJpaRepositories(
        basePackages = "org.intelligentsia.monitoring.infrastructure.svfe.repository",
        entityManagerFactoryRef = "svfeEntityManagerFactory",
        transactionManagerRef = "svfeTransactionManager"
)
public class SvfeDataSourceConfig {

    /** Optional Oracle schema prefix. Set app.svfe.datasource.schema=MXP if tables belong to another user. */
    @Value("${app.svfe.datasource.schema:}")
    private String svfeSchema;

    @Bean(name = "svfeDataSourceProperties")
    @ConfigurationProperties(prefix = "app.svfe.datasource")
    public DataSourceProperties svfeDataSourceProperties() {
        return new DataSourceProperties();
    }

    @Bean(name = "svfeDataSource")
    public DataSource svfeDataSource(
            @Qualifier("svfeDataSourceProperties") DataSourceProperties properties) {
        return properties.initializeDataSourceBuilder().build();
    }

    @Bean(name = "svfeEntityManagerFactory")
    public LocalContainerEntityManagerFactoryBean svfeEntityManagerFactory(
            @Qualifier("svfeDataSource") DataSource ds) {
        LocalContainerEntityManagerFactoryBean em = new LocalContainerEntityManagerFactoryBean();
        em.setDataSource(ds);
        em.setPackagesToScan("org.intelligentsia.monitoring.infrastructure.svfe.entity");
        HibernateJpaVendorAdapter adapter = new HibernateJpaVendorAdapter();
        adapter.setDatabasePlatform("org.hibernate.dialect.OracleDialect");
        em.setJpaVendorAdapter(adapter);
        Map<String, Object> props = new HashMap<>();
        props.put("hibernate.hbm2ddl.auto", "none");
        // Preserve Oracle column names exactly as declared in field names (no camelCase→snake_case)
        props.put("hibernate.physical_naming_strategy",
                "org.hibernate.boot.model.naming.PhysicalNamingStrategyStandardImpl");
        if (!svfeSchema.isBlank()) {
            props.put("hibernate.default_schema", svfeSchema);
        }
        em.setJpaPropertyMap(props);
        return em;
    }

    @Bean(name = "svfeTransactionManager")
    public PlatformTransactionManager svfeTransactionManager(
            @Qualifier("svfeEntityManagerFactory") LocalContainerEntityManagerFactoryBean emf) {
        JpaTransactionManager tm = new JpaTransactionManager();
        tm.setEntityManagerFactory(emf.getObject());
        return tm;
    }

    @Bean(name = "svfeJdbcTemplate")
    public NamedParameterJdbcTemplate svfeJdbcTemplate(
            @Qualifier("svfeDataSource") DataSource ds) {
        NamedParameterJdbcTemplate template = new NamedParameterJdbcTemplate(ds);
        // Oracle default fetch size = 10; 1000 reduces round trips on large extractions
        template.getJdbcTemplate().setFetchSize(1000);
        return template;
    }
}
