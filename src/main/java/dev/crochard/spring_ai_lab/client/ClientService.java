package dev.crochard.spring_ai_lab.client;

import java.util.List;
import java.util.Locale;

import dev.crochard.spring_ai_lab.common.ConflictException;
import dev.crochard.spring_ai_lab.common.NotFoundException;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class ClientService {

	private final ClientRepository clientRepository;

	public ClientService(ClientRepository clientRepository) {
		this.clientRepository = clientRepository;
	}

	/**
	 * L'unicité de l'email ignore la casse : l'email est toujours enregistré en minuscules.
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

	public ClientResponse findById(Long id) {
		return clientRepository.findById(id)
			.map(ClientResponse::from)
			.orElseThrow(() -> new NotFoundException("Client", id));
	}

	public List<ClientResponse> findAll() {
		return clientRepository.findAll().stream().map(ClientResponse::from).toList();
	}

}
