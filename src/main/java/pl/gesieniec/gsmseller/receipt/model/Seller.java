package pl.gesieniec.gsmseller.receipt.model;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public class Seller {
    String name;
    String street;
    String postalCode;
    String city;
    String nip;
}
