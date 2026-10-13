package dev.crochard.spring_ai_lab;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Point d'entrée de l'API REST de gestion de commandes.
 */
@SpringBootApplication
public class SpringAiLabApplication {

	/**
	 * Démarre l'application.
	 * @param args arguments de la ligne de commande
	 */
	public static void main(String[] args) {
		SpringApplication.run(SpringAiLabApplication.class, args);
	}

}
