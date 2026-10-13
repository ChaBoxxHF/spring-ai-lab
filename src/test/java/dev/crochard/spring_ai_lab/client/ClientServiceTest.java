package dev.crochard.spring_ai_lab.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import dev.crochard.spring_ai_lab.common.ConflictException;
import dev.crochard.spring_ai_lab.common.NotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ClientServiceTest {

	@Mock
	private ClientRepository clientRepository;

	@InjectMocks
	private ClientService clientService;

	@Test
	void createsClient() {
		when(clientRepository.existsByEmail("ada@example.com")).thenReturn(false);
		when(clientRepository.save(any(Client.class))).thenAnswer(invocation -> invocation.getArgument(0));

		ClientResponse response = clientService
			.create(new CreateClientRequest("Ada", "Lovelace", "ada@example.com"));

		assertThat(response.firstName()).isEqualTo("Ada");
		assertThat(response.lastName()).isEqualTo("Lovelace");
		assertThat(response.email()).isEqualTo("ada@example.com");
	}

	@Test
	void storesEmailInLowerCase() {
		when(clientRepository.existsByEmail("ada.lovelace@example.com")).thenReturn(false);
		when(clientRepository.save(any(Client.class))).thenAnswer(invocation -> invocation.getArgument(0));

		ClientResponse response = clientService
			.create(new CreateClientRequest("Ada", "Lovelace", "Ada.Lovelace@Example.COM"));

		assertThat(response.email()).isEqualTo("ada.lovelace@example.com");
	}

	@Test
	void rejectsDuplicateEmailIgnoringCase() {
		when(clientRepository.existsByEmail("ada@example.com")).thenReturn(true);

		assertThatThrownBy(
				() -> clientService.create(new CreateClientRequest("Ada", "Lovelace", "ADA@example.com")))
			.isInstanceOf(ConflictException.class);
		verify(clientRepository, never()).save(any());
	}

	@Test
	void rejectsDuplicateEmail() {
		when(clientRepository.existsByEmail("ada@example.com")).thenReturn(true);

		assertThatThrownBy(
				() -> clientService.create(new CreateClientRequest("Ada", "Lovelace", "ada@example.com")))
			.isInstanceOf(ConflictException.class);
		verify(clientRepository, never()).save(any());
	}

	@Test
	void findByIdFailsForUnknownClient() {
		when(clientRepository.findById(42L)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> clientService.findById(42L)).isInstanceOf(NotFoundException.class);
	}

	@Test
	void findByIdReturnsClient() {
		when(clientRepository.findById(1L)).thenReturn(Optional.of(new Client("Ada", "Lovelace", "ada@example.com")));

		ClientResponse response = clientService.findById(1L);

		assertThat(response.firstName()).isEqualTo("Ada");
		assertThat(response.email()).isEqualTo("ada@example.com");
	}

	@Test
	void findAllReturnsAllClients() {
		when(clientRepository.findAll()).thenReturn(List.of(new Client("Ada", "Lovelace", "ada@example.com"),
				new Client("Alan", "Turing", "alan@example.com")));

		assertThat(clientService.findAll()).extracting(ClientResponse::lastName).containsExactly("Lovelace", "Turing");
	}

}
