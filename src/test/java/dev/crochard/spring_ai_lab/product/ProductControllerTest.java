package dev.crochard.spring_ai_lab.product;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;

import dev.crochard.spring_ai_lab.common.NotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(ProductController.class)
class ProductControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private ProductService productService;

	@Test
	void createReturns201WithLocation() throws Exception {
		when(productService.create(any()))
			.thenReturn(new ProductResponse(3L, "KB-01", "Clavier", new BigDecimal("49.99"), true));

		mockMvc.perform(post("/api/products").contentType(MediaType.APPLICATION_JSON).content("""
				{"reference": "KB-01", "name": "Clavier", "price": 49.99, "active": true}
				"""))
			.andExpect(status().isCreated())
			.andExpect(header().string("Location", "/api/products/3"))
			.andExpect(jsonPath("$.price").value(49.99));
	}

	@Test
	void rejectsMissingOrBlankFields() throws Exception {
		mockMvc.perform(post("/api/products").contentType(MediaType.APPLICATION_JSON).content("""
				{"reference": "", "name": "Clavier"}
				"""))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.errors.reference").exists())
			.andExpect(jsonPath("$.errors.price").exists())
			.andExpect(jsonPath("$.errors.active").exists());
		verifyNoInteractions(productService);
	}

	@ParameterizedTest(name = "prix {0}")
	@ValueSource(strings = { "0", "-1.50" })
	void rejectsNonPositivePrice(String price) throws Exception {
		mockMvc.perform(post("/api/products").contentType(MediaType.APPLICATION_JSON).content("""
				{"reference": "KB-01", "name": "Clavier", "price": %s, "active": true}
				""".formatted(price)))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.errors.price").exists());
		verifyNoInteractions(productService);
	}

	@Test
	void rejectsPriceWithMoreThanTwoDecimals() throws Exception {
		mockMvc.perform(post("/api/products").contentType(MediaType.APPLICATION_JSON).content("""
				{"reference": "KB-01", "name": "Clavier", "price": 49.999, "active": true}
				"""))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.errors.price").exists());
		verifyNoInteractions(productService);
	}

	@Test
	void rejectsStatusUpdateWithoutActiveFlag() throws Exception {
		mockMvc.perform(patch("/api/products/3/status").contentType(MediaType.APPLICATION_JSON).content("{}"))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.errors.active").exists());
		verifyNoInteractions(productService);
	}

	@Test
	void mapsUnknownProductTo404() throws Exception {
		when(productService.updateStatus(eq(99L), any())).thenThrow(new NotFoundException("Produit", 99L));

		mockMvc.perform(patch("/api/products/99/status").contentType(MediaType.APPLICATION_JSON).content("""
				{"active": false}
				"""))
			.andExpect(status().isNotFound());
	}

}
