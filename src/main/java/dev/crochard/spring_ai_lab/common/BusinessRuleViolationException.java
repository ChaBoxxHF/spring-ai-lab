package dev.crochard.spring_ai_lab.common;

/**
 * Requête syntaxiquement valide mais qui enfreint une règle métier.
 */
public class BusinessRuleViolationException extends RuntimeException {

	public BusinessRuleViolationException(String message) {
		super(message);
	}

}
