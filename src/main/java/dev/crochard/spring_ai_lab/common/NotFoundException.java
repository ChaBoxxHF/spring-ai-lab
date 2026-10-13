package dev.crochard.spring_ai_lab.common;

/**
 * Ressource demandée inexistante. Traduite en HTTP 404 par {@link GlobalExceptionHandler}.
 */
public class NotFoundException extends RuntimeException {

	/**
	 * @param resource nom de la ressource, utilisé dans le message (ex. « Client »)
	 * @param id identifiant recherché
	 */
	public NotFoundException(String resource, Long id) {
		super(resource + " " + id + " introuvable");
	}

}
