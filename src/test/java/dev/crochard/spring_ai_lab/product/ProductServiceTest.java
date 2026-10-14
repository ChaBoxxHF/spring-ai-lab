package dev.crochard.spring_ai_lab.product;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import dev.crochard.spring_ai_lab.common.BusinessRuleViolationException;
import dev.crochard.spring_ai_lab.common.ConflictException;
import dev.crochard.spring_ai_lab.common.NotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

	@Mock
	private ProductRepository productRepository;

	@InjectMocks
	private ProductService productService;

	@Test
	void createsProduct() {
		when(productRepository.save(any(Product.class))).thenAnswer(invocation -> invocation.getArgument(0));

		ProductResponse response = productService
			.create(new CreateProductRequest("KB-01", "Clavier", new BigDecimal("49.99"), true));

		assertThat(response.reference()).isEqualTo("KB-01");
		assertThat(response.price()).isEqualByComparingTo("49.99");
		assertThat(response.active()).isTrue();
	}

	@Test
	void rejectsDuplicateReference() {
		when(productRepository.existsByReference("KB-01")).thenReturn(true);

		assertThatThrownBy(() -> productService
			.create(new CreateProductRequest("KB-01", "Clavier", new BigDecimal("49.99"), true)))
			.isInstanceOf(ConflictException.class);
		verify(productRepository, never()).save(any());
	}

	@ParameterizedTest
	@ValueSource(strings = { "0", "0.00", "-1.50" })
	void rejectsNonPositivePrice(String price) {
		assertThatThrownBy(() -> productService
			.create(new CreateProductRequest("KB-01", "Clavier", new BigDecimal(price), true)))
			.isInstanceOf(BusinessRuleViolationException.class);
		verify(productRepository, never()).save(any());
	}

	@Test
	void deactivatesProduct() {
		Product product = new Product("KB-01", "Clavier", new BigDecimal("49.99"), true);
		when(productRepository.findById(1L)).thenReturn(Optional.of(product));

		ProductResponse response = productService.updateStatus(1L, new UpdateProductStatusRequest(false));

		assertThat(response.active()).isFalse();
		assertThat(product.isActive()).isFalse();
	}

	@Test
	void activatesProduct() {
		Product product = new Product("OLD-01", "Ancien", new BigDecimal("5.00"), false);
		when(productRepository.findById(1L)).thenReturn(Optional.of(product));

		ProductResponse response = productService.updateStatus(1L, new UpdateProductStatusRequest(true));

		assertThat(response.active()).isTrue();
		assertThat(product.isActive()).isTrue();
	}

	@Test
	void updateStatusFailsForUnknownProduct() {
		when(productRepository.findById(1L)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> productService.updateStatus(1L, new UpdateProductStatusRequest(false)))
			.isInstanceOf(NotFoundException.class);
	}

	@Test
	void findByIdReturnsProduct() {
		when(productRepository.findById(1L))
			.thenReturn(Optional.of(new Product("KB-01", "Clavier", new BigDecimal("49.99"), true)));

		ProductResponse response = productService.findById(1L);

		assertThat(response.reference()).isEqualTo("KB-01");
		assertThat(response.price()).isEqualByComparingTo("49.99");
	}

	@Test
	void findByIdFailsForUnknownProduct() {
		when(productRepository.findById(1L)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> productService.findById(1L)).isInstanceOf(NotFoundException.class);
	}

	@Test
	void findAllReturnsActiveAndInactiveProducts() {
		when(productRepository.findAll()).thenReturn(
				List.of(new Product("KB-01", "Clavier", new BigDecimal("49.99"), true),
						new Product("OLD-01", "Ancien", new BigDecimal("5.00"), false)));

		assertThat(productService.findAll()).extracting(ProductResponse::active).containsExactly(true, false);
	}

}
