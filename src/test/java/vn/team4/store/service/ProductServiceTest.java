package vn.team4.store.service;

import org.junit.jupiter.api.Test;
import vn.team4.store.dto.ProductRequest;
import vn.team4.store.model.Product;
import vn.team4.store.repository.ProductRepository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ProductServiceTest {
    @Test
    void createUsesRequestedIdAndPersistsProductFields() throws Exception {
        ProductRepository repository = mock(ProductRepository.class);
        ProductService service = new ProductService(repository);
        ProductRequest request = new ProductRequest();
        request.setId("prod_17739783938");
        request.setName("Sữa tươi Vinamilk 100% 1L");
        request.setPrice(36000);
        request.setImportPrice(28000);
        request.setStock(50);

        when(repository.save(
                org.mockito.ArgumentMatchers.any(Product.class),
                org.mockito.ArgumentMatchers.eq(request.getId())
        )).thenAnswer(invocation -> invocation.getArgument(0));

        Product created = service.create(request);

        assertEquals(request.getId(), created.getId());
        assertEquals(request.getName(), created.getName());
        assertEquals(50, created.getStock());
        verify(repository).save(
                org.mockito.ArgumentMatchers.any(Product.class),
                org.mockito.ArgumentMatchers.eq(request.getId())
        );
    }
}
