package dev.crochard.spring_ai_lab.client;

import java.net.URI;
import java.util.List;

import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/clients")
public class ClientController {

	private final ClientService clientService;

	public ClientController(ClientService clientService) {
		this.clientService = clientService;
	}

	@PostMapping
	public ResponseEntity<ClientResponse> create(@Valid @RequestBody CreateClientRequest request) {
		ClientResponse created = clientService.create(request);
		return ResponseEntity.created(URI.create("/api/clients/" + created.id())).body(created);
	}

	@GetMapping("/{id}")
	public ClientResponse findById(@PathVariable Long id) {
		return clientService.findById(id);
	}

	@GetMapping
	public List<ClientResponse> findAll() {
		return clientService.findAll();
	}

}
