package pl.gesieniec.gsmseller.receipt.entity;

import jakarta.persistence.Embeddable;
import java.time.LocalDate;
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
public class DateAndPlaceEntity {
    private String place;
    private LocalDate generateDate;
    private LocalDate sellDate;

}
