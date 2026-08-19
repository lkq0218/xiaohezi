package com.xiaohezi.agent;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class BillExplanationServiceTest {
    private final BillExplanationService service = new BillExplanationService();

    @Test
    void reconcilesEveryDriverToTheBillDelta() {
        AgentModels.Bill current = bill("2026-07", "128.60", "186.0", 31,
            "112.60", "12.00", "4.00");
        AgentModels.Bill previous = bill("2026-06", "93.40", "132.0", 30,
            "82.40", "11.00", "0.00");

        BillExplanationService.Explanation result = service.explain(current, previous);

        assertThat(result.amountDelta).isEqualByComparingTo("35.20");
        assertThat(result.kwhDelta).isEqualByComparingTo("54.0");
        assertThat(result.amountDeltaRate).isEqualByComparingTo("37.7");
        assertThat(result.drivers).extracting(driver -> driver.amount)
            .containsExactly(new BigDecimal("30.20"), new BigDecimal("4.00"), new BigDecimal("1.00"));
        assertThat(result.unexplainedAmount).isEqualByComparingTo("0.00");
    }

    @Test
    void rejectsAnExplanationThatCannotReconcile() {
        AgentModels.Bill current = bill("2026-07", "128.60", "186.0", 31,
            "100.00", "12.00", "4.00");
        AgentModels.Bill previous = bill("2026-06", "93.40", "132.0", 30,
            "82.40", "11.00", "0.00");

        org.assertj.core.api.Assertions.assertThatThrownBy(() -> service.explain(current, previous))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("未能与金额差额对齐");
    }

    private AgentModels.Bill bill(String period, String amount, String kwh, int days,
                                  String energy, String surcharge, String adjustment) {
        return new AgentModels.Bill("BILL-" + period, period, amount, kwh, days, "UNPAID")
            .lineItem("电度电费", energy)
            .lineItem("附加费用", surcharge)
            .lineItem("调整项", adjustment);
    }
}
