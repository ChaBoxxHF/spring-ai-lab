package dev.crochard.spring_ai_lab.client;

public record ClientResponse(Long id, String firstName, String lastName, String email) {

	static ClientResponse from(Client client) {
		return new ClientResponse(client.getId(), client.getFirstName(), client.getLastName(), client.getEmail());
	}

}
