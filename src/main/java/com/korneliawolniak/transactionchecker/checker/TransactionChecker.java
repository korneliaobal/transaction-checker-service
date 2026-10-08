package com.korneliawolniak.transactionchecker.checker;

import com.korneliawolniak.paymentprocessing.avro.TransactionValidationRequest;
import com.korneliawolniak.paymentprocessing.avro.TransactionValidationResult;
import com.korneliawolniak.paymentprocessing.validation.BankAccountValidator;
import com.korneliawolniak.paymentprocessing.validation.ValidationReason;
import com.korneliawolniak.transactionchecker.kafka.TransactionValidationResultPublisher;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class TransactionChecker {
  private static final Logger log = LoggerFactory.getLogger(TransactionChecker.class);
  private final TransactionValidationResultPublisher publisher;

  public TransactionChecker(TransactionValidationResultPublisher publisher) {
    this.publisher = publisher;
  }

  @KafkaListener(topics = "transaction-validation-request", groupId = "transaction-checker")
  public void handle(TransactionValidationRequest request) {
    List<CharSequence> reasons = new ArrayList<>();
    if (request.getCreditorName() == null || request.getCreditorName().toString().isBlank())
      reasons.add(ValidationReason.CREDITOR_NAME_REQUIRED.name());
    if (!BankAccountValidator.isValid(
        request.getCreditorAccountNumber() == null
            ? null
            : request.getCreditorAccountNumber().toString()))
      reasons.add(ValidationReason.CREDITOR_ACCOUNT_INVALID.name());
    try {
      BigDecimal amount =
          new BigDecimal(request.getAmount() == null ? "" : request.getAmount().toString());
      if (amount.signum() <= 0) reasons.add(ValidationReason.AMOUNT_INVALID.name());
      else if (amount.compareTo(BigDecimal.valueOf(5)) <= 0)
        reasons.add(ValidationReason.AMOUNT_BELOW_MINIMUM.name());
    } catch (NumberFormatException error) {
      reasons.add(ValidationReason.AMOUNT_INVALID.name());
    }

    String status = reasons.isEmpty() ? "OK" : "NOT_OK";
    TransactionValidationResult result =
        TransactionValidationResult.newBuilder()
            .setTransactionId(request.getTransactionId())
            .setPaymentId(request.getPaymentId())
            .setStatus(status)
            .setReasonCodes(reasons)
            .build();
    publisher.publish(result);
    log.info(
        "Transaction {} validation result: {} ({})", request.getTransactionId(), status, reasons);
  }
}
