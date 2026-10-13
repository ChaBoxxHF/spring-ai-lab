package dev.crochard.spring_ai_lab.common;

/**
 * Requête incompatible avec l'état actuel d'une ressource (unicité, transition de statut...).
 */
public class ConflictException extends RuntimeException {

	public ConflictException(String message) {
		super(message);
	}

}
