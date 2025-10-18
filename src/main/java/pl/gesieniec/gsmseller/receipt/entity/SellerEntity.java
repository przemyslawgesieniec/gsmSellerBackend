package pl.gesieniec.gsmseller.receipt.entity;

import jakarta.persistence.Embeddable;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

@ToString
@Embeddable
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
public class SellerEntity {
    private String name;
    private String street;
    private String postalCode;
    private String city;
    private String nip;
}
