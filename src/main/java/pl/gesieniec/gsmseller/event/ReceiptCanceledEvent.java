package pl.gesieniec.gsmseller.event;

import java.util.List;
import java.util.UUID;

public record ReceiptCanceledEvent(
        UUID receiptTechnicalId,
        List<UUID> phoneTechnicalIds,
        String canceledBy
) {}
