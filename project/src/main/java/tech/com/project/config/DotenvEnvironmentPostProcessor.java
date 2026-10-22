package tech.com.project.config;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.ConfigurableEnvironment;

/**
 * Registrador temprano de variables de entorno para Spring Boot.
 * Garantiza que las variables definidas en .env se carguen ANTES de que
 * Spring intente resolver application.yaml, datasources o perfiles de prueba.
 */
@Order(Ordered.HIGHEST_PRECEDENCE)
public class DotenvEnvironmentPostProcessor implements EnvironmentPostProcessor {

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        EnvLoader.loadEnv();
    }
}
