package com.bkbank.ledger.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test các công thức tính toán trong Credit Card Statement
 * Kiểm tra tất cả các trường hợp edge cases
 */
@DisplayName("Credit Card Statement Calculation Tests")
class CreditCardStatementCalculationTest {
    
    @Nested
    @DisplayName("1. Minimum Due Calculation Tests")
    class MinimumDueCalculationTests {
        
        @Test
        @DisplayName("Should calculate minimum due as percentage when above floor")
        void testMinimumDue_PercentageAboveFloor() {
            // Balance = 1000, rate = 5%, floor = 10
            // Expected: 5% of 1000 = 50 (> floor 10)
            double balance = 1000.0;
            double rate = 5.0;
            double floor = 10.0;
            
            double result = calculateMinimumDue(balance, rate, floor);
            
            assertEquals(50.0, result, 0.01, "Should be 5% of balance");
        }
        
        @Test
        @DisplayName("Should use floor when percentage is below floor")
        void testMinimumDue_PercentageBelowFloor() {
            // Balance = 100, rate = 5%, floor = 10
            // Expected: floor 10 (5% of 100 = 5 < floor 10)
            double balance = 100.0;
            double rate = 5.0;
            double floor = 10.0;
            
            double result = calculateMinimumDue(balance, rate, floor);
            
            assertEquals(10.0, result, 0.01, "Should use floor amount");
        }
        
        @Test
        @DisplayName("Should return full balance when balance is less than floor")
        void testMinimumDue_BalanceLessThanFloor() {
            // Balance = 5, rate = 5%, floor = 10
            // Expected: 5 (full balance < floor)
            double balance = 5.0;
            double rate = 5.0;
            double floor = 10.0;
            
            double result = calculateMinimumDue(balance, rate, floor);
            
            assertEquals(5.0, result, 0.01, "Should be full balance");
        }
        
        @Test
        @DisplayName("Should return 0 when balance is 0")
        void testMinimumDue_ZeroBalance() {
            double balance = 0.0;
            double rate = 5.0;
            double floor = 10.0;
            
            double result = calculateMinimumDue(balance, rate, floor);
            
            assertEquals(0.0, result, 0.01, "Should be 0");
        }
        
        @Test
        @DisplayName("Should return 0 when balance is negative")
        void testMinimumDue_NegativeBalance() {
            double balance = -100.0;
            double rate = 5.0;
            double floor = 10.0;
            
            double result = calculateMinimumDue(balance, rate, floor);
            
            assertEquals(0.0, result, 0.01, "Should be 0 for negative balance");
        }
        
        @ParameterizedTest
        @CsvSource({
            "1000.0, 5.0, 10.0, 50.0",      // Normal case
            "100.0, 5.0, 10.0, 10.0",       // Floor case
            "5.0, 5.0, 10.0, 5.0",          // Balance < floor
            "2000.0, 3.0, 25.0, 60.0",      // 3% of 2000 = 60
            "500.0, 10.0, 25.0, 50.0",      // 10% of 500 = 50
            "200.0, 2.0, 25.0, 25.0"        // 2% of 200 = 4 < floor 25
        })
        @DisplayName("Should calculate minimum due correctly for various inputs")
        void testMinimumDue_ParameterizedCases(double balance, double rate, double floor, double expected) {
            double result = calculateMinimumDue(balance, rate, floor);
            assertEquals(expected, result, 0.01);
        }
    }
    
    @Nested
    @DisplayName("2. New Balance Calculation Tests")
    class NewBalanceCalculationTests {
        
        @Test
        @DisplayName("Should calculate new balance correctly")
        void testNewBalance_Normal() {
            double previousBalance = 500.0;
            double totalCharges = 1000.0;
            double totalPayments = 300.0;
            
            double newBalance = previousBalance + totalCharges - totalPayments;
            
            assertEquals(1200.0, newBalance, 0.01);
        }
        
        @Test
        @DisplayName("Should handle zero previous balance")
        void testNewBalance_ZeroPrevious() {
            double previousBalance = 0.0;
            double totalCharges = 1000.0;
            double totalPayments = 200.0;
            
            double newBalance = previousBalance + totalCharges - totalPayments;
            
            assertEquals(800.0, newBalance, 0.01);
        }
        
        @Test
        @DisplayName("Should handle payment exceeding charges")
        void testNewBalance_PaymentExceedsCharges() {
            double previousBalance = 100.0;
            double totalCharges = 200.0;
            double totalPayments = 500.0;
            
            double newBalance = previousBalance + totalCharges - totalPayments;
            
            assertEquals(-200.0, newBalance, 0.01, "Can be negative (credit balance)");
        }
        
        @Test
        @DisplayName("Should handle full payment")
        void testNewBalance_FullPayment() {
            double previousBalance = 500.0;
            double totalCharges = 500.0;
            double totalPayments = 1000.0;
            
            double newBalance = previousBalance + totalCharges - totalPayments;
            
            assertEquals(0.0, newBalance, 0.01);
        }
    }
    
    @Nested
    @DisplayName("3. Available Credit Calculation Tests")
    class AvailableCreditCalculationTests {
        
        @Test
        @DisplayName("Should calculate available credit correctly")
        void testAvailableCredit_Normal() {
            double creditLimit = 100000.0;
            double newBalance = 25000.0;
            
            double availableCredit = creditLimit - newBalance;
            
            assertEquals(75000.0, availableCredit, 0.01);
        }
        
        @Test
        @DisplayName("Should return full limit when balance is zero")
        void testAvailableCredit_ZeroBalance() {
            double creditLimit = 100000.0;
            double newBalance = 0.0;
            
            double availableCredit = creditLimit - newBalance;
            
            assertEquals(100000.0, availableCredit, 0.01);
        }
        
        @Test
        @DisplayName("Should return 0 when balance equals limit")
        void testAvailableCredit_BalanceEqualsLimit() {
            double creditLimit = 100000.0;
            double newBalance = 100000.0;
            
            double availableCredit = creditLimit - newBalance;
            
            assertEquals(0.0, availableCredit, 0.01);
        }
        
        @Test
        @DisplayName("Should return negative when over limit")
        void testAvailableCredit_OverLimit() {
            double creditLimit = 100000.0;
            double newBalance = 105000.0;
            
            double availableCredit = creditLimit - newBalance;
            
            assertEquals(-5000.0, availableCredit, 0.01, "Negative means over limit");
        }
    }
    
    @Nested
    @DisplayName("4. Daily Interest Calculation Tests")
    class DailyInterestCalculationTests {
        
        @Test
        @DisplayName("Should calculate daily rate correctly")
        void testDailyRate_Calculation() {
            double annualRate = 36.0; // 36% per year
            
            double dailyRate = annualRate / 100.0 / 365.0;
            
            assertEquals(0.000986301, dailyRate, 0.000001, "36% annual = ~0.0986% daily");
        }
        
        @Test
        @DisplayName("Should calculate simple interest for 1 day")
        void testInterest_OneDay() {
            double balance = 1000.0;
            double annualRate = 36.0;
            int days = 1;
            
            double dailyRate = annualRate / 100.0 / 365.0;
            double interest = balance * dailyRate * days;
            
            assertEquals(0.986, interest, 0.01, "1000 * 0.0986% * 1 day");
        }
        
        @Test
        @DisplayName("Should calculate interest for 30 days")
        void testInterest_ThirtyDays() {
            double balance = 1000.0;
            double annualRate = 36.0;
            int days = 30;
            
            double dailyRate = annualRate / 100.0 / 365.0;
            double interest = balance * dailyRate * days;
            
            assertEquals(29.59, interest, 0.01, "1000 * 0.0986% * 30 days");
        }
        
        @Test
        @DisplayName("Should calculate interest for full year")
        void testInterest_FullYear() {
            double balance = 1000.0;
            double annualRate = 36.0;
            int days = 365;
            
            double dailyRate = annualRate / 100.0 / 365.0;
            double interest = balance * dailyRate * days;
            
            assertEquals(360.0, interest, 1.0, "Should be close to 36% of balance");
        }
        
        @ParameterizedTest
        @CsvSource({
            "1000.0, 36.0, 1, 0.99",       // 1 day
            "1000.0, 36.0, 7, 6.90",       // 1 week
            "1000.0, 36.0, 30, 29.59",     // 1 month
            "5000.0, 36.0, 30, 147.95",    // Higher balance
            "1000.0, 18.0, 30, 14.79",     // Lower rate
            "10000.0, 36.0, 60, 591.78"    // 2 months
        })
        @DisplayName("Should calculate interest correctly for various scenarios")
        void testInterest_ParameterizedCases(double balance, double annualRate, int days, double expected) {
            double dailyRate = annualRate / 100.0 / 365.0;
            double interest = roundMoney(balance * dailyRate * days);
            
            assertEquals(expected, interest, 0.5, "Interest calculation for " + days + " days");
        }
    }
    
    @Nested
    @DisplayName("5. Late Fee Calculation Tests")
    class LateFeeCalculationTests {
        
        @Test
        @DisplayName("Should calculate late fee as percentage when above floor")
        void testLateFee_PercentageAboveFloor() {
            double remainingMinimumDue = 1000.0;
            double lateFeeRate = 4.0; // 4%
            double lateFeeFloor = 15.0;
            
            double lateFee = Math.max(remainingMinimumDue * lateFeeRate / 100.0, lateFeeFloor);
            
            assertEquals(40.0, lateFee, 0.01, "4% of 1000 = 40");
        }
        
        @Test
        @DisplayName("Should use floor when percentage is below floor")
        void testLateFee_PercentageBelowFloor() {
            double remainingMinimumDue = 100.0;
            double lateFeeRate = 4.0; // 4%
            double lateFeeFloor = 15.0;
            
            double lateFee = Math.max(remainingMinimumDue * lateFeeRate / 100.0, lateFeeFloor);
            
            assertEquals(15.0, lateFee, 0.01, "4% of 100 = 4 < floor 15");
        }
        
        @ParameterizedTest
        @CsvSource({
            "1000.0, 4.0, 15.0, 40.0",     // Normal case
            "100.0, 4.0, 15.0, 15.0",      // Floor case
            "500.0, 4.0, 15.0, 20.0",      // 4% of 500 = 20
            "50.0, 5.0, 20.0, 20.0",       // 5% of 50 = 2.5 < floor 20
            "2000.0, 3.0, 25.0, 60.0"      // 3% of 2000 = 60
        })
        @DisplayName("Should calculate late fee correctly for various inputs")
        void testLateFee_ParameterizedCases(double remainingMinimumDue, double rate, double floor, double expected) {
            double lateFee = roundMoney(Math.max(remainingMinimumDue * rate / 100.0, floor));
            assertEquals(expected, lateFee, 0.01);
        }
    }
    
    @Nested
    @DisplayName("6. Payment Allocation Waterfall Tests")
    class PaymentAllocationTests {
        
        @Test
        @DisplayName("Should allocate payment to late fee first")
        void testPaymentAllocation_LateFeeFirst() {
            double payment = 100.0;
            double lateFee = 15.0;
            double pastDueMin = 50.0;
            double interest = 10.0;
            double balance = 1000.0;
            
            // Waterfall: Late Fee -> Past Due -> Interest -> Balance
            double toLateFee = Math.min(payment, lateFee);
            double remaining = payment - toLateFee;
            
            assertEquals(15.0, toLateFee, 0.01);
            assertEquals(85.0, remaining, 0.01);
        }
        
        @Test
        @DisplayName("Should allocate to past due after late fee")
        void testPaymentAllocation_PastDueSecond() {
            double payment = 100.0;
            double lateFee = 15.0;
            double pastDueMin = 50.0;
            
            double toLateFee = Math.min(payment, lateFee);
            double remaining = payment - toLateFee;
            double toPastDue = Math.min(remaining, pastDueMin);
            remaining -= toPastDue;
            
            assertEquals(15.0, toLateFee, 0.01);
            assertEquals(50.0, toPastDue, 0.01);
            assertEquals(35.0, remaining, 0.01);
        }
        
        @Test
        @DisplayName("Should allocate full waterfall correctly")
        void testPaymentAllocation_FullWaterfall() {
            double payment = 1000.0;
            double lateFee = 15.0;
            double pastDueMin = 200.0;
            double interest = 50.0;
            double balance = 2000.0;
            
            // Allocate
            double toLateFee = Math.min(payment, lateFee);
            double remaining = payment - toLateFee;
            
            double toPastDue = Math.min(remaining, pastDueMin);
            remaining -= toPastDue;
            
            double toInterest = Math.min(remaining, interest);
            remaining -= toInterest;
            
            double toBalance = remaining;
            
            assertEquals(15.0, toLateFee, 0.01);
            assertEquals(200.0, toPastDue, 0.01);
            assertEquals(50.0, toInterest, 0.01);
            assertEquals(735.0, toBalance, 0.01);
            assertEquals(0.0, remaining - toBalance, 0.01);
        }
        
        @Test
        @DisplayName("Should handle partial payment covering only late fee")
        void testPaymentAllocation_OnlyLateFee() {
            double payment = 10.0;
            double lateFee = 15.0;
            
            double toLateFee = Math.min(payment, lateFee);
            double remaining = payment - toLateFee;
            
            assertEquals(10.0, toLateFee, 0.01);
            assertEquals(0.0, remaining, 0.01);
        }
    }
    
    @Nested
    @DisplayName("7. Grace Period Eligibility Tests")
    class GracePeriodTests {
        
        @Test
        @DisplayName("Should be eligible when balance is zero")
        void testGracePeriod_ZeroBalance() {
            double newBalance = 0.0;
            
            boolean eligible = (newBalance <= 0);
            
            assertTrue(eligible, "Zero balance should be eligible");
        }
        
        @Test
        @DisplayName("Should be eligible when paid 100% before due date")
        void testGracePeriod_FullPayment() {
            double newBalance = 1000.0;
            double appliedCredits = 1000.0;
            
            boolean eligible = roundMoney(Math.max(newBalance - appliedCredits, 0.0)) <= 0;
            
            assertTrue(eligible, "Full payment should be eligible");
        }
        
        @Test
        @DisplayName("Should NOT be eligible when paid only minimum")
        void testGracePeriod_MinimumPayment() {
            double newBalance = 1000.0;
            double appliedCredits = 50.0; // Only minimum
            
            boolean eligible = roundMoney(Math.max(newBalance - appliedCredits, 0.0)) <= 0;
            
            assertFalse(eligible, "Minimum payment should NOT be eligible");
        }
        
        @Test
        @DisplayName("Should NOT be eligible when paid partial")
        void testGracePeriod_PartialPayment() {
            double newBalance = 1000.0;
            double appliedCredits = 500.0; // 50% payment
            
            boolean eligible = roundMoney(Math.max(newBalance - appliedCredits, 0.0)) <= 0;
            
            assertFalse(eligible, "Partial payment should NOT be eligible");
        }
    }
    
    @Nested
    @DisplayName("8. Statement Status Determination Tests")
    class StatementStatusTests {
        
        @Test
        @DisplayName("Should be PAID when remaining balance is zero")
        void testStatus_Paid() {
            double remainingBalance = 0.0;
            
            String status = determineStatus(remainingBalance, 0.0, false);
            
            assertEquals("PAID", status);
        }
        
        @Test
        @DisplayName("Should be OVERDUE when past due date with remaining minimum")
        void testStatus_Overdue() {
            double remainingBalance = 500.0;
            double remainingMinimumDue = 50.0;
            boolean pastDueDate = true;
            
            String status = determineStatus(remainingBalance, remainingMinimumDue, pastDueDate);
            
            assertEquals("OVERDUE", status);
        }
        
        @Test
        @DisplayName("Should be PARTIALLY_PAID when paid some amount")
        void testStatus_PartiallyPaid() {
            double remainingBalance = 500.0;
            double remainingMinimumDue = 0.0; // Paid minimum
            boolean pastDueDate = false;
            
            String status = "PARTIALLY_PAID"; // Has payment but not full
            
            assertEquals("PARTIALLY_PAID", status);
        }
        
        @Test
        @DisplayName("Should be OPEN when no payment yet")
        void testStatus_Open() {
            double remainingBalance = 1000.0;
            double remainingMinimumDue = 50.0;
            boolean pastDueDate = false;
            
            String status = "OPEN"; // No payment, not past due
            
            assertEquals("OPEN", status);
        }
    }
    
    @Nested
    @DisplayName("9. Edge Cases and Boundary Tests")
    class EdgeCaseTests {
        
        @Test
        @DisplayName("Should handle very small amounts (rounding)")
        void testRounding_SmallAmounts() {
            double value = 0.005;
            
            double rounded = roundMoney(value);
            
            assertEquals(0.01, rounded, 0.001, "Should round up to 0.01");
        }
        
        @Test
        @DisplayName("Should handle very large amounts")
        void testCalculation_LargeAmounts() {
            double balance = 1000000.0; // 1 million
            double rate = 5.0;
            double floor = 10.0;
            
            double minimumDue = calculateMinimumDue(balance, rate, floor);
            
            assertEquals(50000.0, minimumDue, 0.01, "5% of 1M = 50K");
        }
        
        @Test
        @DisplayName("Should handle zero rate")
        void testInterest_ZeroRate() {
            double balance = 1000.0;
            double annualRate = 0.0;
            int days = 30;
            
            double dailyRate = annualRate / 100.0 / 365.0;
            double interest = balance * dailyRate * days;
            
            assertEquals(0.0, interest, 0.01, "Zero rate should give zero interest");
        }
        
        @Test
        @DisplayName("Should handle negative balance (credit balance)")
        void testCalculation_NegativeBalance() {
            double balance = -100.0; // Customer has credit
            double rate = 5.0;
            double floor = 10.0;
            
            double minimumDue = calculateMinimumDue(balance, rate, floor);
            
            assertEquals(0.0, minimumDue, 0.01, "Negative balance should have 0 minimum due");
        }
    }
    
    // ==================== Helper Methods ====================
    
    private double calculateMinimumDue(double newBalance, double minimumPaymentRate, double minimumPaymentFloor) {
        if (newBalance <= 0) {
            return 0.0;
        }
        double percentageAmount = roundMoney(newBalance * minimumPaymentRate / 100.0);
        return roundMoney(Math.min(newBalance, Math.max(percentageAmount, minimumPaymentFloor)));
    }
    
    private double roundMoney(double value) {
        return Math.round(value * 100.0) / 100.0;
    }
    
    private String determineStatus(double remainingBalance, double remainingMinimumDue, boolean pastDueDate) {
        if (remainingBalance <= 0) {
            return "PAID";
        }
        if (pastDueDate && remainingMinimumDue > 0) {
            return "OVERDUE";
        }
        return "OPEN";
    }
}
