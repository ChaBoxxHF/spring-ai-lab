package dev.crochard.spring_ai_lab.common;

/**
 * Requête syntaxiquement valide mais qui enfreint une règle métier. Traduite en HTTP 422 par
 * {@link GlobalExceptionHandler}.
 */
public class BusinessRuleViolationException extends RuntimeException {

	/**
	 * @param message description de la règle enfreinte, renvoyée au client
	 */
	public BusinessRuleViolationException(String message) {
		super(message);
	}

}
