package pl.gesieniec.gsmseller.receipt.model;

import java.time.LocalDate;

public record DateAndPlace(String place, LocalDate generateDate, LocalDate sellDate) {
}
