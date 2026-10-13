package dev.crochard.spring_ai_lab.client;

import java.net.URI;
import java.util.List;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Point d'entrée REST de la ressource client ({@code /api/clients}).
 */
@RestController
@RequestMapping("/api/clients")
@Tag(name = "Clients", description = "Création et consultation des clients")
public class ClientController {

	private final ClientService clientService;

	/**
	 * @param clientService service métier des clients
	 */
	public ClientController(ClientService clientService) {
		this.clientService = clientService;
	}

	/**
	 * Crée un client.
	 * @param request données du client à créer
	 * @return le client créé (201), avec son URI dans l'en-tête {@code Location}
	 */
	@PostMapping
	@Operation(summary = "Créer un client",
			description = "L'email est enregistré en minuscules et doit être unique sans tenir compte de la casse.")
	@ApiResponse(responseCode = "201", description = "Client créé")
	@ApiResponse(responseCode = "400", description = "Requête invalide")
	@ApiResponse(responseCode = "409", description = "Un client existe déjà avec cet email")
	public ResponseEntity<ClientResponse> create(@Valid @RequestBody CreateClientRequest request) {
		ClientResponse created = clientService.create(request);
		return ResponseEntity.created(URI.create("/api/clients/" + created.id())).body(created);
	}

	/**
	 * Récupère un client par son identifiant.
	 * @param id identifiant du client
	 * @return le client
	 */
	@GetMapping("/{id}")
	@Operation(summary = "Consulter un client")
	@ApiResponse(responseCode = "200", description = "Client trouvé")
	@ApiResponse(responseCode = "404", description = "Client introuvable")
	public ClientResponse findById(@Parameter(description = "Identifiant du client") @PathVariable Long id) {
		return clientService.findById(id);
	}

	/**
	 * Liste tous les clients.
	 * @return l'ensemble des clients
	 */
	@GetMapping
	@Operation(summary = "Lister les clients")
	@ApiResponse(responseCode = "200", description = "Liste des clients")
	public List<ClientResponse> findAll() {
		return clientService.findAll();
	}

}
