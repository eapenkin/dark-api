package com.rbkmoney.dark.api.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.rbkmoney.damsel.base.Content;
import com.rbkmoney.damsel.base.InvalidRequest;
import com.rbkmoney.damsel.merch_stat.*;
import com.rbkmoney.dark.api.converter.StatPaymentToPaymentSearchResultConverter;
import com.rbkmoney.dark.api.converter.StatRefundToRefundSearchResultConverter;
import com.rbkmoney.dark.api.magista.dsl.MstDsl;
import com.rbkmoney.swag.dark_api.model.EnrichedSearchResult;
import com.rbkmoney.swag.dark_api.model.InlineResponse200;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.thrift.TException;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
@Service
@RequiredArgsConstructor
public class MagistaService {

    private final DarkMessiahStatisticsSrv.Iface magistaClient;

    public InlineResponse200 getPaymentsByQuery(String shopID,
                                                OffsetDateTime fromTime,
                                                OffsetDateTime toTime,
                                                Integer limit,
                                                String paymentStatus,
                                                String paymentFlow,
                                                String paymentMethod,
                                                String paymentTerminalProvider,
                                                String invoiceID,
                                                String paymentID,
                                                String payerEmail,
                                                String payerIP,
                                                String payerFingerprint,
                                                String customerID,
                                                String bin,
                                                String lastDigits,
                                                String bankCardTokenProvider,
                                                String bankCardPaymentSystem,
                                                Long paymentAmount,
                                                String continuationToken) {
        try {
            return fromStatResponse(magistaClient.getByQuery(new StatRequest()
                    .setDsl(MstDsl.createPaymentsRequest(shopID,
                            fromTime,
                            toTime,
                            limit,
                            paymentStatus,
                            paymentFlow,
                            paymentMethod,
                            paymentTerminalProvider,
                            invoiceID,
                            paymentID,
                            payerEmail,
                            payerIP,
                            payerFingerprint,
                            customerID,
                            bin,
                            lastDigits,
                            bankCardTokenProvider,
                            bankCardPaymentSystem,
                            paymentAmount))
                    .setContinuationToken(continuationToken)));
        } catch (BadToken | InvalidRequest e) {
            log.error("Invalid request to magista", e);
            throw new IllegalArgumentException(e);
        } catch (TException e) {
            log.error("Some TException while requesting magista", e);
            throw new RuntimeException(e);
        } catch (JsonProcessingException e) {
            log.error("JSON processing exception", e);
            throw new IllegalArgumentException(e);
        }
    }

    public InlineResponse200 getRefundsByQuery(String shopID,
                                               OffsetDateTime fromTime,
                                               OffsetDateTime toTime,
                                               Integer limit,
                                               String invoiceID,
                                               String paymentID,
                                               String refundID,
                                               String refundStatus,
                                               String continuationToken) {
        try {
            return fromStatResponse(magistaClient.getByQuery(new StatRequest()
                    .setDsl(MstDsl.createRefundsRequest(shopID,
                            fromTime,
                            toTime,
                            limit,
                            invoiceID,
                            paymentID,
                            refundID,
                            refundStatus)
                    )
                    .setContinuationToken(continuationToken)));
        } catch (BadToken | InvalidRequest e) {
            log.error("Invalid request to magista", e);
            throw new IllegalArgumentException(e);
        } catch (TException e) {
            log.error("Some TException while requesting magista", e);
            throw new RuntimeException(e);
        } catch (JsonProcessingException e) {
            log.error("JSON processing exception", e);
            throw new IllegalArgumentException(e);
        }
    }

    private InlineResponse200 fromStatResponse(StatResponse statResponse) {
        return new InlineResponse200()
                .continuationToken(statResponse.getContinuationToken())
                .result(statResponse.getData().getEnrichedInvoices()
                        .stream()
                        .map(enrichedStatInvoice -> {
                            Content invoiceContext = enrichedStatInvoice.getInvoice().getContext();

                            List<StatRefund> refunds = enrichedStatInvoice.getRefunds();
                            List<StatPayment> payments = enrichedStatInvoice.getPayments();
                            return new EnrichedSearchResult()
                                            .refund(refunds.isEmpty() ? null : StatRefundToRefundSearchResultConverter.convert(refunds.get(0)))
                                            .payment(StatPaymentToPaymentSearchResultConverter.convert(payments.get(0), invoiceContext));
                        }).collect(Collectors.toList()));
    }

}