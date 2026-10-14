package org.intelligentsia.monitoring.infrastructure.local;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.*;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;

@Configuration
@ConditionalOnProperty(name="app.local-configuration.enabled", havingValue="true")
public class LocalConfigurationBootstrap {
    @Bean public static BeanPostProcessor localConfigurationImport(ObjectProvider<LocalApplicationState> state) {
        return new BeanPostProcessor() {
            @Override public Object postProcessAfterInitialization(Object bean,String name) {
                if (name.equals("entityManagerFactory") && bean instanceof LocalContainerEntityManagerFactoryBean) state.getObject().load();
                return bean;
            }
        };
    }
}
