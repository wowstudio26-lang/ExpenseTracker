package com.wowstudio.expensetracker.data

/**
 * Pure financial aggregation logic. Kept outside Compose so it can be tested without Android.
 */
data class FinanceSummary(
    val personalIncome: Double,
    val contributions: Double,
    val expenses: Double,
    val outstandingDebt: Double,
    val monthlyEmi: Double,
    val availableAfterOneEmi: Double
)

fun calculateFinanceSummary(
    transactions: List<FinanceTransaction>,
    loans: List<Loan>
): FinanceSummary {
    val personalIncome = transactions
        .filter { it.type == TransactionType.INCOME && it.owner == "Mine" }
        .sumOf { it.amount }

    // Contributions are deliberately NOT income. They are tracked separately.
    val contributions = transactions
        .filter { it.type == TransactionType.CONTRIBUTION }
        .sumOf { it.amount }

    // Expenses are household-wide; do not split them by owner.
    val expenses = transactions
        .filter { it.type == TransactionType.EXPENSE }
        .sumOf { it.amount }

    // Pay Later is tracked separately from loan/EMI debt and must not inflate
    // the overall loan-debt figure or the recurring EMI commitment.
    val outstandingDebt = loans
        .filter { it.type != LoanType.PAY_LATER }
        .sumOf { it.remainingAmount }

    val monthlyEmi = loans
        .filter { it.type != LoanType.PAY_LATER && it.remainingMonths > 0 }
        .sumOf { it.monthlyPayment }

    val availableAfterOneEmi = personalIncome + contributions - expenses - monthlyEmi

    return FinanceSummary(
        personalIncome = personalIncome,
        contributions = contributions,
        expenses = expenses,
        outstandingDebt = outstandingDebt,
        monthlyEmi = monthlyEmi,
        availableAfterOneEmi = availableAfterOneEmi
    )
}
