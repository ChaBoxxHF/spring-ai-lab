package dev.crochard.spring_ai_lab.common;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

/**
 * Traduit les exceptions levées par les contrôleurs en réponses HTTP au format Problem Details (RFC 9457).
 */
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

	/**
	 * @param ex ressource introuvable
	 * @return une réponse 404
	 */
	@ExceptionHandler(NotFoundException.class)
	ProblemDetail handleNotFound(NotFoundException ex) {
		return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
	}

	/**
	 * @param ex conflit avec l'état de la ressource
	 * @return une réponse 409
	 */
	@ExceptionHandler(ConflictException.class)
	ProblemDetail handleConflict(ConflictException ex) {
		return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
	}

	/**
	 * @param ex règle métier enfreinte
	 * @return une réponse 422
	 */
	@ExceptionHandler(BusinessRuleViolationException.class)
	ProblemDetail handleBusinessRuleViolation(BusinessRuleViolationException ex) {
		return ProblemDetail.forStatusAndDetail(HttpStatus.UNPROCESSABLE_CONTENT, ex.getMessage());
	}

	/**
	 * Filet de sécurité pour les contraintes de base de données (ex. doublon inséré en concurrence malgré la
	 * vérification préalable du service). Le détail technique n'est pas exposé.
	 * @param ex violation de contrainte d'intégrité
	 * @return une réponse 409
	 */
	@ExceptionHandler(DataIntegrityViolationException.class)
	ProblemDetail handleDataIntegrityViolation(DataIntegrityViolationException ex) {
		return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT,
				"Violation d'une contrainte d'intégrité des données");
	}

	/**
	 * @param ex modification concurrente détectée par le verrouillage optimiste
	 * @return une réponse 409
	 */
	@ExceptionHandler(OptimisticLockingFailureException.class)
	ProblemDetail handleOptimisticLockingFailure(OptimisticLockingFailureException ex) {
		return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT,
				"La ressource a été modifiée simultanément, veuillez réessayer");
	}

	/**
	 * Renvoie une réponse 400 dont la propriété {@code errors} associe chaque champ invalide à son premier
	 * message d'erreur.
	 */
	@Override
	protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex,
			HttpHeaders headers, HttpStatusCode status, WebRequest request) {
		Map<String, String> errors = new LinkedHashMap<>();
		ex.getBindingResult().getFieldErrors()
			.forEach(error -> errors.putIfAbsent(error.getField(), error.getDefaultMessage()));
		ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Requête invalide");
		problem.setProperty("errors", errors);
		return ResponseEntity.badRequest().body(problem);
	}

}
