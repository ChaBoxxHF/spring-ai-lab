package dev.crochard.spring_ai_lab.common;

/**
 * Requête incompatible avec l'état actuel d'une ressource (unicité, transition de statut...). Traduite en
 * HTTP 409 par {@link GlobalExceptionHandler}.
 */
public class ConflictException extends RuntimeException {

	/**
	 * @param message description du conflit, renvoyée au client
	 */
	public ConflictException(String message) {
		super(message);
	}

}
