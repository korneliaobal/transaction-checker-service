package com.korneliawolniak.transactionchecker.checker;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;

import com.korneliawolniak.paymentprocessing.avro.TransactionValidationRequest;
import com.korneliawolniak.paymentprocessing.avro.TransactionValidationResult;
import com.korneliawolniak.transactionchecker.kafka.TransactionValidationResultPublisher;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

class TransactionCheckerTest {

  private final TransactionValidationResultPublisher publisher =
      Mockito.mock(TransactionValidationResultPublisher.class);

  private final TransactionChecker transactionChecker = new TransactionChecker(publisher);

  @Test
  void shouldPublishOkForValidTransaction() {
    TransactionValidationRequest request =
        TransactionValidationRequest.newBuilder()
            .setTransactionId("transaction-1")
            .setPaymentId("payment-1")
            .setCreditorName("ABC Company")
            .setCreditorAccountNumber("PL10105000997603123456789123")
            .setAmount("100.00")
            .setCurrency("PLN")
            .build();

    transactionChecker.handle(request);

    ArgumentCaptor<TransactionValidationResult> captor =
        ArgumentCaptor.forClass(TransactionValidationResult.class);

    verify(publisher).publish(captor.capture());

    TransactionValidationResult result = captor.getValue();

    assertEquals("transaction-1", result.getTransactionId().toString());
    assertEquals("payment-1", result.getPaymentId().toString());
    assertEquals("OK", result.getStatus().toString());
  }

  @Test
  void shouldPublishNotOkWhenCreditorNameIsBlank() {
    TransactionValidationRequest request =
        TransactionValidationRequest.newBuilder()
            .setTransactionId("transaction-1")
            .setPaymentId("payment-1")
            .setCreditorName("")
            .setCreditorAccountNumber("PL10105000997603123456789123")
            .setAmount("100.00")
            .setCurrency("PLN")
            .build();

    transactionChecker.handle(request);

    ArgumentCaptor<TransactionValidationResult> captor =
        ArgumentCaptor.forClass(TransactionValidationResult.class);

    verify(publisher).publish(captor.capture());

    assertEquals("NOT_OK", String.valueOf(captor.getValue().getStatus()));
    assertEquals(
        java.util.List.of("CREDITOR_NAME_REQUIRED"),
        captor.getValue().getReasonCodes().stream().map(Object::toString).toList());
  }

  @Test
  void shouldPublishNotOkWhenCreditorAccountNumberIsInvalid() {
    TransactionValidationRequest request =
        TransactionValidationRequest.newBuilder()
            .setTransactionId("transaction-1")
            .setPaymentId("payment-1")
            .setCreditorName("ABC Company")
            .setCreditorAccountNumber("INVALID")
            .setAmount("100.00")
            .setCurrency("PLN")
            .build();

    transactionChecker.handle(request);

    ArgumentCaptor<TransactionValidationResult> captor =
        ArgumentCaptor.forClass(TransactionValidationResult.class);

    verify(publisher).publish(captor.capture());

    assertEquals("NOT_OK", String.valueOf(captor.getValue().getStatus()));
    assertEquals(
        java.util.List.of("CREDITOR_ACCOUNT_INVALID"),
        captor.getValue().getReasonCodes().stream().map(Object::toString).toList());
  }

  @Test
  void shouldPublishNotOkWhenAmountIsFiveOrLess() {
    TransactionValidationRequest request =
        TransactionValidationRequest.newBuilder()
            .setTransactionId("transaction-1")
            .setPaymentId("payment-1")
            .setCreditorName("ABC Company")
            .setCreditorAccountNumber("PL10105000997603123456789123")
            .setAmount("5.00")
            .setCurrency("PLN")
            .build();

    transactionChecker.handle(request);

    ArgumentCaptor<TransactionValidationResult> captor =
        ArgumentCaptor.forClass(TransactionValidationResult.class);

    verify(publisher).publish(captor.capture());

    assertEquals("NOT_OK", String.valueOf(captor.getValue().getStatus()));
    assertEquals(
        java.util.List.of("AMOUNT_BELOW_MINIMUM"),
        captor.getValue().getReasonCodes().stream().map(Object::toString).toList());
  }

  @Test
  void reportsAllViolationsWithoutThrowingOnMalformedAmount() {
    var request =
        TransactionValidationRequest.newBuilder()
            .setPaymentId("payment-1")
            .setTransactionId("transaction-1")
            .setCreditorName("")
            .setCreditorAccountNumber("PL62109010140000071219812874")
            .setAmount("invalid")
            .setCurrency("PLN")
            .build();
    transactionChecker.handle(request);
    var captor = ArgumentCaptor.forClass(TransactionValidationResult.class);
    verify(publisher).publish(captor.capture());
    assertEquals("NOT_OK", captor.getValue().getStatus().toString());
    assertEquals(
        java.util.List.of("CREDITOR_NAME_REQUIRED", "CREDITOR_ACCOUNT_INVALID", "AMOUNT_INVALID"),
        captor.getValue().getReasonCodes().stream().map(Object::toString).toList());
  }
}
