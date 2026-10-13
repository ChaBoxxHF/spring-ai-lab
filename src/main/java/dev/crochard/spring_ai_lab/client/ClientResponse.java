package dev.crochard.spring_ai_lab.client;

/**
 * Représentation d'un client renvoyée par l'API.
 * @param id identifiant du client
 * @param firstName prénom
 * @param lastName nom
 * @param email adresse email, en minuscules
 */
public record ClientResponse(Long id, String firstName, String lastName, String email) {

	/**
	 * @param client entité à convertir
	 * @return la représentation API du client
	 */
	static ClientResponse from(Client client) {
		return new ClientResponse(client.getId(), client.getFirstName(), client.getLastName(), client.getEmail());
	}

}
