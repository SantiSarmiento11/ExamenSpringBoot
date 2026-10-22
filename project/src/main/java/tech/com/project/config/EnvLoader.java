package tech.com.project.config;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.logging.Logger;

/**
 * Cargador de variables de entorno desde archivos .env.
 * Permite centralizar la configuración local y confidencial sin depender
 * de variables del sistema operativo fijadas manualmente.
 * 
 * Busca el archivo .env en:
 * 1. Directorio de ejecución actual (user.dir)
 * 2. Directorio padre (..) por si la aplicación se ejecuta dentro del subdirectorio 'project'
 */
public final class EnvLoader {

    private static final Logger log = Logger.getLogger(EnvLoader.class.getName());

    private EnvLoader() {
        // Clase de utilidades
    }

    public static void loadEnv() {
        String userDir = System.getProperty("user.dir", ".");
        File envFile = new File(userDir, ".env");

        if (!envFile.exists()) {
            File parentEnv = new File(userDir, "../.env");
            if (parentEnv.exists()) {
                envFile = parentEnv;
            }
        }

        if (!envFile.exists() || !envFile.isFile()) {
            log.info("[EnvLoader] No se encontro archivo .env en " + userDir + " ni en el directorio superior.");
            return;
        }

        try {
            log.info("[EnvLoader] Cargando variables desde: " + envFile.getCanonicalPath());
            try (BufferedReader reader = new BufferedReader(new FileReader(envFile, StandardCharsets.UTF_8))) {
                String line;
                int count = 0;
                while ((line = reader.readLine()) != null) {
                    line = line.trim();
                    if (line.isEmpty() || line.startsWith("#")) {
                        continue;
                    }

                    int separatorIndex = line.indexOf('=');
                    if (separatorIndex > 0) {
                        String key = line.substring(0, separatorIndex).trim();
                        String value = line.substring(separatorIndex + 1).trim();

                        // Remover comillas envolventes si existen (" o ')
                        if ((value.startsWith("\"") && value.endsWith("\"")) ||
                            (value.startsWith("'") && value.endsWith("'"))) {
                            if (value.length() >= 2) {
                                value = value.substring(1, value.length() - 1);
                            }
                        }

                        // Establecer en System.getProperties() para que Spring Boot y JVM lo tomen
                        // Solo si no fue previamente fijada por variable de entorno OS
                        if (System.getenv(key) == null && System.getProperty(key) == null) {
                            System.setProperty(key, value);
                            count++;
                        }
                    }
                }
                log.info("[EnvLoader] " + count + " variables de entorno inyectadas exitosamente al sistema.");
            }
        } catch (IOException e) {
            log.warning("[EnvLoader] Advertencia al leer .env: " + e.getMessage());
        }
    }
}
