package pl.gesieniec.gsmseller.phone.scan;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

@ToString
@Getter
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class PhoneScanDto {
    private String model;
    private String ram;
    private String memory;
    private String color;
    private String imei1;
    private String imei2;
}
