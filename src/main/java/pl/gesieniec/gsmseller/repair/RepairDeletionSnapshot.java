package pl.gesieniec.gsmseller.repair;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import pl.gesieniec.gsmseller.repair.model.RepairStatus;

record RepairDeletionSnapshot(
    Long id, UUID technicalId, String businessId, Client client, ServicePoint servicePoint,
    String manufacturer, String model, String imei, String deviceType, String color,
    String deviceCondition, String problemDescription, String remarks,
    boolean moistureTraces, boolean warrantyRepair, boolean turnsOn, boolean anonymous, String lockCode,
    LocalDateTime receiptDate, LocalDateTime estimatedRepairDate, BigDecimal estimatedCost, BigDecimal advancePayment,
    boolean forCustomer, UUID phoneTechnicalId, RepairStatus status, boolean archived,
    LocalDateTime createDateTime, LocalDateTime handoverDate, List<Note> notes,
    BigDecimal purchasePrice, BigDecimal repairPrice, String location, String repairDescription
) {
    static RepairDeletionSnapshot from(Repair repair) {
        var client = repair.getClient();
        var point = repair.getServicePoint();
        return new RepairDeletionSnapshot(
            repair.getId(), repair.getTechnicalId(), repair.getBusinessId(),
            client == null ? null : new Client(client.getId(), client.getTechnicalId(), client.getName(),
                client.getSurname(), client.getPhoneNumber()),
            point == null ? null : new ServicePoint(point.getId(), point.getTechnicalId(), point.getName()),
            repair.getManufacturer(), repair.getModel(), repair.getImei(), repair.getDeviceType(), repair.getColor(),
            repair.getDeviceCondition(), repair.getProblemDescription(), repair.getRemarks(),
            repair.isMoistureTraces(), repair.isWarrantyRepair(), repair.isTurnsOn(), repair.isAnonymous(), repair.getLockCode(),
            repair.getReceiptDate(), repair.getEstimatedRepairDate(), repair.getEstimatedCost(), repair.getAdvancePayment(),
            repair.isForCustomer(), repair.getPhoneTechnicalId(), repair.getStatus(), repair.isArchived(),
            repair.getCreateDateTime(), repair.getHandoverDate(),
            repair.getNotes().stream().map(note -> new Note(note.getId(), note.getContent(), note.getCreatedAt(),
                note.getAuthor().getId(), note.getAuthor().getUsername())).toList(),
            repair.getPurchasePrice(), repair.getRepairPrice(), repair.getLocation(), repair.getRepairDescription()
        );
    }

    record Client(Long id, UUID technicalId, String name, String surname, String phoneNumber) {}
    record ServicePoint(Long id, UUID technicalId, String name) {}
    record Note(Long id, String content, LocalDateTime createdAt, Long authorId, String authorUsername) {}
}
