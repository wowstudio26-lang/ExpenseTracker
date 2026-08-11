package com.wowstudio.expensetracker.data

import org.junit.Assert.assertEquals
import org.junit.Test

class FinanceSummaryTest {
    @Test
    fun contribution_is_not_counted_as_personal_income() {
        val transactions = listOf(
            FinanceTransaction(1, TransactionType.INCOME, "Mine", 100000.0, "Salary", "Salary", 1L, 1L, false),
            FinanceTransaction(2, TransactionType.CONTRIBUTION, "Wife", 20000.0, "Contribution", "Wife", 2L, 2L, false),
            FinanceTransaction(3, TransactionType.EXPENSE, "Household", 5000.0, "Food", "Food", 3L, 3L, false)
        )

        val summary = calculateFinanceSummary(transactions, emptyList())

        assertEquals(100000.0, summary.personalIncome, 0.001)
        assertEquals(20000.0, summary.contributions, 0.001)
        assertEquals(5000.0, summary.expenses, 0.001)
    }

    @Test
    fun expenses_are_aggregated_without_owner_split() {
        val transactions = listOf(
            FinanceTransaction(1, TransactionType.EXPENSE, "Mine", 1000.0, "Food", "Mine", 1L, 1L, false),
            FinanceTransaction(2, TransactionType.EXPENSE, "Wife", 2000.0, "Home", "Wife", 2L, 2L, false)
        )

        val summary = calculateFinanceSummary(transactions, emptyList())

        assertEquals(3000.0, summary.expenses, 0.001)
    }

    @Test
    fun monthly_emi_and_debt_ignore_pay_later_from_emi_total() {
        val loans = listOf(
            Loan(
                id = 1L,
                lender = "Amazon",
                type = LoanType.EMI,
                product = "Phone",
                originalAmount = 30000.0,
                monthlyPayment = 3000.0,
                tenureMonths = 10,
                paidMonths = 2,
                startDate = 1L,
                nextDueDate = 2L
            ),
            Loan(
                id = 2L,
                lender = "Amazon Pay Later",
                type = LoanType.PAY_LATER,
                product = "Accessory",
                originalAmount = 5000.0,
                monthlyPayment = 0.0,
                tenureMonths = 0,
                paidMonths = 0,
                startDate = 1L,
                nextDueDate = 3L
            )
        )

        val summary = calculateFinanceSummary(emptyList(), loans)

        assertEquals(24000.0, summary.outstandingDebt, 0.001)
        assertEquals(3000.0, summary.monthlyEmi, 0.001)
    }
}
