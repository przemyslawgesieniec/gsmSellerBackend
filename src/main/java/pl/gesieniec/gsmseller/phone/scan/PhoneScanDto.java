package pl.gesieniec.gsmseller.phone.scan;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import pl.gesieniec.gsmseller.phone.stock.model.PurchaseType;

@ToString
@Getter
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class PhoneScanDto {
    private String model;
    private String ram;
    private String memory;
    private String color;
    private String imei;
    @Setter
    private String source;
    @Setter
    private String initialPrice;
    @Setter
    private String sellingPrice;
    @Setter
    private String name;
    @Setter
    private PurchaseType purchaseType;
    @Setter
    private String comment;
    @Setter
    private String description;
    @Setter
    private String batteryCondition;
    @Setter
    private boolean used;


    public PhoneScanDto(String model, String ram, String memory, String color, String imei) {
        this.model = model;
        this.ram = ram;
        this.memory = memory;
        this.color = color;
        this.imei = imei;
    }

    public void normalizeData(){
        if (ram != null) {
            ram = ram.replaceAll("\\D", "");
        }
        if (memory != null) {
            memory = memory.replaceAll("\\D", "");
        }
    }

}
