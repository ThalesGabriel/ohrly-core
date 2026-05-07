package org.ohrly.core.infra;

import org.ohrly.core.domain.Order;
import org.ohrly.core.domain.Payment;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class CsvLoader {

    public List<Order> loadOrders(String path) throws IOException {
        List<Order> orders = new ArrayList<>();

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

        try (BufferedReader br = Files.newBufferedReader(Paths.get(path))) {
            String line;
            br.readLine(); // skip header

            while ((line = br.readLine()) != null) {
                String[] fields = line.split(",", -1); // mantém campos vazios

                if (fields.length < 5) {
                    continue;
                }

                String orderId = fields[0];
                String purchaseRaw = fields[3];
                String approvedRaw = fields[4];

                LocalDateTime purchaseTimestamp =
                        LocalDateTime.parse(purchaseRaw, formatter);

                LocalDateTime approvedAt = null;

                if (approvedRaw != null && !approvedRaw.isBlank()) {
                    approvedAt = LocalDateTime.parse(approvedRaw, formatter);
                }

                orders.add(new Order(
                        orderId,
                        purchaseTimestamp,
                        approvedAt
                ));
            }
        }

        return orders;
    }

    public List<Payment> loadPayments(String path) throws IOException {
        List<Payment> payments = new ArrayList<>();

        try (BufferedReader br = Files.newBufferedReader(Paths.get(path))) {
            String line;
            br.readLine(); // skip header

            while ((line = br.readLine()) != null) {
                String[] fields = line.split(",", -1);

                if (fields.length < 3) {
                    continue;
                }

                String orderId = fields[0];
                String paymentType = fields[2];
                String paymentValue = fields[4];

                if (orderId == null || orderId.isBlank()) {
                    continue;
                }

                if (paymentType == null || paymentType.isBlank()) {
                    continue;
                }

                if (paymentValue == null || paymentValue.isBlank()) {
                    continue;
                }

                payments.add(new Payment(
                        orderId,
                        paymentType,
                        Double.parseDouble(paymentValue)
                ));
            }
        }

        return payments;
    }
}