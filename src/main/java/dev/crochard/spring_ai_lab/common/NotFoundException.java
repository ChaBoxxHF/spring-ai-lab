package dev.crochard.spring_ai_lab.common;

public class NotFoundException extends RuntimeException {

	public NotFoundException(String resource, Long id) {
		super(resource + " " + id + " introuvable");
	}

}
