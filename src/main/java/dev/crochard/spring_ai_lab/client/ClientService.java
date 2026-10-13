package dev.crochard.spring_ai_lab.client;

import java.util.List;
import java.util.Locale;

import dev.crochard.spring_ai_lab.common.ConflictException;
import dev.crochard.spring_ai_lab.common.NotFoundException;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Logique métier des clients. Les méthodes sont en lecture seule par défaut.
 */
@Service
@Transactional(readOnly = true)
public class ClientService {

	private final ClientRepository clientRepository;

	/**
	 * @param clientRepository accès aux clients persistés
	 */
	public ClientService(ClientRepository clientRepository) {
		this.clientRepository = clientRepository;
	}

	/**
	 * Crée un client.
	 * <p>
	 * L'unicité de l'email ignore la casse : l'email est toujours enregistré en minuscules.
	 * @param request données du client à créer
	 * @return le client créé
	 * @throws ConflictException si un client existe déjà avec cet email
	 */
	@Transactional
	public ClientResponse create(CreateClientRequest request) {
		String email = request.email().toLowerCase(Locale.ROOT);
		if (clientRepository.existsByEmail(email)) {
			throw new ConflictException("Un client existe déjà avec l'email " + email);
		}
		Client client = clientRepository.save(new Client(request.firstName(), request.lastName(), email));
		return ClientResponse.from(client);
	}

	/**
	 * Récupère un client par son identifiant.
	 * @param id identifiant du client
	 * @return le client
	 * @throws NotFoundException si le client n'existe pas
	 */
	public ClientResponse findById(Long id) {
		return clientRepository.findById(id)
			.map(ClientResponse::from)
			.orElseThrow(() -> new NotFoundException("Client", id));
	}

	/**
	 * Liste tous les clients.
	 * @return l'ensemble des clients
	 */
	public List<ClientResponse> findAll() {
		return clientRepository.findAll().stream().map(ClientResponse::from).toList();
	}

}
