package com.xiaohezi.agent;

import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static com.xiaohezi.agent.AgentModels.Bill;

@Service
public class BillExplanationService {
    private static final BigDecimal ONE_HUNDRED = new BigDecimal("100");

    public Explanation explain(Bill current, Bill comparison) {
        BigDecimal amountDelta = current.amount.subtract(comparison.amount).setScale(2, RoundingMode.HALF_UP);
        BigDecimal amountDeltaRate = percent(amountDelta, comparison.amount);
        BigDecimal kwhDelta = current.kwh.subtract(comparison.kwh).setScale(1, RoundingMode.HALF_UP);
        BigDecimal currentDailyKwh = daily(current);
        BigDecimal comparisonDailyKwh = daily(comparison);
        BigDecimal dailyKwhDeltaRate = percent(
            currentDailyKwh.subtract(comparisonDailyKwh), comparisonDailyKwh);

        List<Driver> drivers = new ArrayList<>();
        addDriver(drivers, "用电量相关电费", "CONSUMPTION",
            lineItemDelta(current, comparison, "电度电费"),
            "bill-line:" + current.period + ":energy");
        addDriver(drivers, "费用调整项", "ADJUSTMENT",
            lineItemDelta(current, comparison, "调整项"),
            "bill-line:" + current.period + ":adjustment");
        addDriver(drivers, "附加费用", "SURCHARGE",
            lineItemDelta(current, comparison, "附加费用"),
            "bill-line:" + current.period + ":surcharge");

        BigDecimal explained = drivers.stream()
            .map(driver -> driver.amount)
            .reduce(BigDecimal.ZERO, BigDecimal::add)
            .setScale(2, RoundingMode.HALF_UP);
        BigDecimal unexplained = amountDelta.subtract(explained).setScale(2, RoundingMode.HALF_UP);
        if (unexplained.abs().compareTo(new BigDecimal("0.01")) > 0) {
            throw new IllegalStateException("账单解释未能与金额差额对齐");
        }

        Explanation result = new Explanation();
        result.current = current;
        result.comparison = comparison;
        result.amountDelta = amountDelta;
        result.amountDeltaRate = amountDeltaRate;
        result.kwhDelta = kwhDelta;
        result.currentDailyKwh = currentDailyKwh;
        result.comparisonDailyKwh = comparisonDailyKwh;
        result.dailyKwhDeltaRate = dailyKwhDeltaRate;
        result.drivers = drivers;
        result.unexplainedAmount = unexplained;
        result.generatedAt = LocalDateTime.now().withNano(0).toString();
        return result;
    }

    private BigDecimal daily(Bill bill) {
        return bill.kwh.divide(BigDecimal.valueOf(bill.billingDays), 2, RoundingMode.HALF_UP);
    }

    private BigDecimal percent(BigDecimal delta, BigDecimal base) {
        if (base.compareTo(BigDecimal.ZERO) == 0) return null;
        return delta.divide(base, 4, RoundingMode.HALF_UP)
            .multiply(ONE_HUNDRED).setScale(1, RoundingMode.HALF_UP);
    }

    private BigDecimal lineItemDelta(Bill current, Bill comparison, String type) {
        return current.lineItems.getOrDefault(type, BigDecimal.ZERO)
            .subtract(comparison.lineItems.getOrDefault(type, BigDecimal.ZERO))
            .setScale(2, RoundingMode.HALF_UP);
    }

    private void addDriver(List<Driver> drivers, String label, String type,
                           BigDecimal amount, String evidenceId) {
        if (amount.compareTo(BigDecimal.ZERO) != 0) {
            drivers.add(new Driver(type, label, amount, evidenceId));
        }
    }

    public static class Explanation {
        public Bill current;
        public Bill comparison;
        public BigDecimal amountDelta;
        public BigDecimal amountDeltaRate;
        public BigDecimal kwhDelta;
        public BigDecimal currentDailyKwh;
        public BigDecimal comparisonDailyKwh;
        public BigDecimal dailyKwhDeltaRate;
        public List<Driver> drivers;
        public BigDecimal unexplainedAmount;
        public String generatedAt;
    }

    public static class Driver {
        public final String type;
        public final String label;
        public final BigDecimal amount;
        public final String evidenceId;

        Driver(String type, String label, BigDecimal amount, String evidenceId) {
            this.type = type;
            this.label = label;
            this.amount = amount;
            this.evidenceId = evidenceId;
        }
    }
}
