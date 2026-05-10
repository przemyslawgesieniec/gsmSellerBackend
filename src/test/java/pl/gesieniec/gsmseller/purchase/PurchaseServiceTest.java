package pl.gesieniec.gsmseller.purchase;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import pl.gesieniec.gsmseller.offer.CloudflareImagesService;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PurchaseServiceTest {

    @Mock
    private PurchaseRepository purchaseRepository;

    @Mock
    private PurchasePhotoRepository purchasePhotoRepository;

    @Mock
    private CloudflareImagesService cloudflareImagesService;

    @InjectMocks
    private PurchaseService purchaseService;

    @Test
    void createPurchaseStoresCloudflareImageIdInsteadOfPhotoData() throws IOException {
        MockMultipartFile photo = new MockMultipartFile(
                "photos",
                "phone.jpg",
                "image/jpeg",
                "image-bytes".getBytes()
        );
        when(cloudflareImagesService.uploadImage(photo)).thenReturn("cloudflare-image-id");

        purchaseService.createPurchase("iPhone 15", "123456789", "Opis", List.of(photo));

        ArgumentCaptor<Purchase> purchaseCaptor = ArgumentCaptor.forClass(Purchase.class);
        verify(purchaseRepository).save(purchaseCaptor.capture());
        verify(cloudflareImagesService).uploadImage(photo);

        Purchase savedPurchase = purchaseCaptor.getValue();
        assertThat(savedPurchase.getPhotos()).hasSize(1);
        PurchasePhoto savedPhoto = savedPurchase.getPhotos().getFirst();
        assertThat(savedPhoto.getImageId()).isEqualTo("cloudflare-image-id");
        assertThat(savedPhoto.getData()).isNull();
        assertThat(savedPhoto.getContentType()).isEqualTo("image/jpeg");
    }

    @Test
    void agreePriceMovesPurchaseToPriceAgreedStatus() {
        Purchase purchase = new Purchase("iPhone 15", "123456789", "Opis", List.of());
        UUID technicalId = purchase.getTechnicalId();
        when(purchaseRepository.findByTechnicalId(technicalId)).thenReturn(Optional.of(purchase));

        purchaseService.agreePrice(technicalId, new BigDecimal("1200.00"), "seller@example.com");

        assertThat(purchase.getStatus()).isEqualTo(PurchaseStatus.PRICE_AGREED);
        assertThat(purchase.getAgreedPrice()).isEqualByComparingTo("1200.00");
        assertThat(purchase.getComments()).hasSize(1);
        assertThat(purchase.getComments().getFirst().getAuthorUsername()).isEqualTo("seller@example.com");
        assertThat(purchase.getComments().getFirst().getContent()).contains("Ustalono cenę skupu");
        verify(purchaseRepository).save(purchase);
    }

    @Test
    void markPurchasedMovesPurchaseToPurchasedStatus() {
        Purchase purchase = new Purchase("iPhone 15", "123456789", "Opis", List.of());
        UUID technicalId = purchase.getTechnicalId();
        when(purchaseRepository.findByTechnicalId(technicalId)).thenReturn(Optional.of(purchase));

        purchaseService.markPurchased(technicalId, new BigDecimal("1300.00"), "seller@example.com");

        assertThat(purchase.getStatus()).isEqualTo(PurchaseStatus.PURCHASED);
        assertThat(purchase.getAgreedPrice()).isEqualByComparingTo("1300.00");
        assertThat(purchase.getContactedCustomer()).isTrue();
        assertThat(purchase.getComments()).hasSize(1);
        assertThat(purchase.getComments().getFirst().getAuthorUsername()).isEqualTo("seller@example.com");
        assertThat(purchase.getComments().getFirst().getContent()).contains("Skup zakończony sukcesem");
        verify(purchaseRepository).save(purchase);
    }

    @Test
    void closePurchaseAddsAutomaticComment() {
        Purchase purchase = new Purchase("iPhone 15", "123456789", "Opis", List.of());
        UUID technicalId = purchase.getTechnicalId();
        when(purchaseRepository.findByTechnicalId(technicalId)).thenReturn(Optional.of(purchase));

        purchaseService.closePurchase(technicalId, "Klient odrzucił ofertę", true, "seller@example.com");

        assertThat(purchase.getStatus()).isEqualTo(PurchaseStatus.CLOSED);
        assertThat(purchase.getComments()).hasSize(1);
        assertThat(purchase.getComments().getFirst().getAuthorUsername()).isEqualTo("seller@example.com");
        assertThat(purchase.getComments().getFirst().getContent()).contains("Zamknięto skup bez powodzenia");
        verify(purchaseRepository).save(purchase);
    }

    @Test
    void addCommentStoresAuthorAndContent() {
        Purchase purchase = new Purchase("iPhone 15", "123456789", "Opis", List.of());
        UUID technicalId = purchase.getTechnicalId();
        when(purchaseRepository.findByTechnicalId(technicalId)).thenReturn(Optional.of(purchase));

        purchaseService.addComment(technicalId, "seller@example.com", "Klient prosi o kontakt po 17");

        assertThat(purchase.getComments()).hasSize(1);
        PurchaseComment comment = purchase.getComments().getFirst();
        assertThat(comment.getAuthorUsername()).isEqualTo("seller@example.com");
        assertThat(comment.getContent()).isEqualTo("Klient prosi o kontakt po 17");
        assertThat(comment.getCreatedAt()).isNotNull();
        verify(purchaseRepository).save(purchase);
    }
}
