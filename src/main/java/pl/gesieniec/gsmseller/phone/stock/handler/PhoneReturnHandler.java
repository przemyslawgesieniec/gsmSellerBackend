package pl.gesieniec.gsmseller.phone.stock.handler;

import java.util.List;
import java.util.UUID;

public interface PhoneReturnHandler {
    void returnPhones(List<UUID> phoneTechnicalIds);
}
