package dev.crochard.spring_ai_lab.client;

import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/clients")
public class ClientController {

	private final ClientRepository clientRepository;

	public ClientController(ClientRepository clientRepository) {
		this.clientRepository = clientRepository;
	}

	@GetMapping
	public List<Client> getAll() {
		return clientRepository.findAll();
	}

	@GetMapping("/{id}")
	public Client getById(@PathVariable Long id) {
		return clientRepository.findById(id)
			.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Client not found"));
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public Client create(@RequestBody Client client) {
		if (clientRepository.existsByEmail(client.getEmail())) {
			throw new ResponseStatusException(HttpStatus.CONFLICT, "Email already exists");
		}
		return clientRepository.save(client);
	}
}
