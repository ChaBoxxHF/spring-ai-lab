package dev.crochard.spring_ai_lab.client;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import dev.crochard.spring_ai_lab.common.ConflictException;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(ClientController.class)
class ClientControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private ClientService clientService;

	@Test
	void rejectsInvalidEmail() throws Exception {
		mockMvc.perform(post("/api/clients").contentType(MediaType.APPLICATION_JSON).content("""
				{"firstName": "Ada", "lastName": "Lovelace", "email": "pas-un-email"}
				"""))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.errors.email").exists());
		verifyNoInteractions(clientService);
	}

	@Test
	void rejectsBlankNames() throws Exception {
		mockMvc.perform(post("/api/clients").contentType(MediaType.APPLICATION_JSON).content("""
				{"firstName": " ", "lastName": "", "email": "ada@example.com"}
				"""))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.errors.firstName").exists())
			.andExpect(jsonPath("$.errors.lastName").exists());
	}

	@Test
	void mapsDuplicateEmailTo409() throws Exception {
		when(clientService.create(any())).thenThrow(new ConflictException("email déjà utilisé"));

		mockMvc.perform(post("/api/clients").contentType(MediaType.APPLICATION_JSON).content("""
				{"firstName": "Ada", "lastName": "Lovelace", "email": "ada@example.com"}
				"""))
			.andExpect(status().isConflict());
	}

	@Test
	void mapsConcurrentDuplicateEmailTo409WithoutLeakingSqlDetails() throws Exception {
		when(clientService.create(any()))
			.thenThrow(new DataIntegrityViolationException("duplicate key value violates uk_clients_email"));

		mockMvc.perform(post("/api/clients").contentType(MediaType.APPLICATION_JSON).content("""
				{"firstName": "Ada", "lastName": "Lovelace", "email": "ada@example.com"}
				"""))
			.andExpect(status().isConflict())
			.andExpect(jsonPath("$.detail").value("Violation d'une contrainte d'intégrité des données"));
	}

}
