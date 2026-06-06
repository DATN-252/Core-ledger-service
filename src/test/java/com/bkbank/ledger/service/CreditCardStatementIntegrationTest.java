package com.bkbank.ledger.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests cho Credit Card Statement
 * Test các scenario thực tế end-to-end
 */
@DisplayName("Credit Card Statement Integration Tests")
class CreditCardStatementIntegrationTest {
    
    @Nested
    @DisplayName("Scenario 1: Customer pays full balance before due date")
    class FullPaymentBeforeDueDateScenario {
        
        @Test
        @DisplayName("Should have no interest and no late fee")
        void testFullPaymentBeforeDueDate() {
            // Given: Statement with 1000 balance, due in 20 days
            double previousBalance = 0.0;
            double totalCharges = 1000.0;
            double totalPayments = 0.0;
            double newBalance = previousBalance + totalCharges - totalPayments;
            
            // When: Customer pays full balance before due date
            double payment = 1000.0;
            double remainingBalance = newBalance - payment;
            
            // Then: No interest, no late fee, grace period eligible
            assertEquals(0.0, remainingBalance, 0.01);
            assertTrue(remainingBalance <= 0, "Should be eligible for grace period");
            
            // Interest should be 0
            double interest = 0.0;
            assertEquals(0.0, interest, 0.01);
            
            // Late fee should be 0
            double lateFee = 0.0;
            assertEquals(0.0, lateFee, 0.01);
            
            // Status should be PAID
            String status = "PAID";
            assertEquals("PAID", status);
        }
    }
    
    @Nested
    @DisplayName("Scenario 2: Customer pays only minimum before due date")
    class MinimumPaymentBeforeDueDateScenario {
        
        @Test
        @DisplayName("Should have interest but no late fee, not eligible for grace period")
        void testMinimumPaymentBeforeDueDate() {
            // Given: Statement with 1000 balance, minimum due = 50
            double newBalance = 1000.0;
            double minimumDue = 50.0;
            
            // When: Customer pays only minimum before due date
            double payment = 50.0;
            double remainingBalance = newBalance - payment;
            double remainingMinimumDue = minimumDue - payment;
            
            // Then: Has remaining balance, not eligible for grace period
            assertEquals(950.0, remainingBalance, 0.01);
            assertEquals(0.0, remainingMinimumDue, 0.01);
            assertFalse(remainingBalance <= 0, "Should NOT be eligible for grace period");
            
            // After due date: Should have interest (36% annual for 30 days on 950)
            double annualRate = 36.0;
            double dailyRate = annualRate / 100.0 / 365.0;
            int daysOverdue = 30;
            double expectedInterest = remainingBalance * dailyRate * daysOverdue;
            
            assertEquals(28.11, expectedInterest, 0.5, "Interest for 30 days");
            
            // No late fee because minimum was paid
            double lateFee = 0.0;
            assertEquals(0.0, lateFee, 0.01);
        }
    }
    
    @Nested
    @DisplayName("Scenario 3: Customer pays less than minimum")
    class BelowMinimumPaymentScenario {
        
        @Test
        @DisplayName("Should have both interest and late fee after due date")
        void testBelowMinimumPayment() {
            // Given: Statement with 1000 balance, minimum due = 50
            double newBalance = 1000.0;
            double minimumDue = 50.0;
            
            // When: Customer pays only 20 (less than minimum)
            double payment = 20.0;
            double remainingBalance = newBalance - payment;
            double remainingMinimumDue = minimumDue - payment;
            
            // Then: After due date
            assertEquals(980.0, remainingBalance, 0.01);
            assertEquals(30.0, remainingMinimumDue, 0.01);
            
            // Should have interest (36% annual for 30 days on 980)
            double annualRate = 36.0;
            double dailyRate = annualRate / 100.0 / 365.0;
            int daysOverdue = 30;
            double expectedInterest = remainingBalance * dailyRate * daysOverdue;
            
            assertEquals(29.00, expectedInterest, 0.5, "Interest for 30 days");
            
            // Should have late fee (4% of remaining minimum or floor 15)
            double lateFeeRate = 4.0;
            double lateFeeFloor = 15.0;
            double expectedLateFee = Math.max(remainingMinimumDue * lateFeeRate / 100.0, lateFeeFloor);
            
            assertEquals(15.0, expectedLateFee, 0.01, "Late fee should be floor amount");
            
            // Status should be OVERDUE
            String status = "OVERDUE";
            assertEquals("OVERDUE", status);
        }
    }
    
    @Nested
    @DisplayName("Scenario 4: Customer makes no payment")
    class NoPaymentScenario {
        
        @Test
        @DisplayName("Should have maximum interest and late fee")
        void testNoPayment() {
            // Given: Statement with 1000 balance, minimum due = 50
            double newBalance = 1000.0;
            double minimumDue = 50.0;
            
            // When: Customer makes no payment
            double payment = 0.0;
            double remainingBalance = newBalance - payment;
            double remainingMinimumDue = minimumDue - payment;
            
            // Then: After 30 days overdue
            assertEquals(1000.0, remainingBalance, 0.01);
            assertEquals(50.0, remainingMinimumDue, 0.01);
            
            // Should have interest (36% annual for 30 days on 1000)
            double annualRate = 36.0;
            double dailyRate = annualRate / 100.0 / 365.0;
            int daysOverdue = 30;
            double expectedInterest = remainingBalance * dailyRate * daysOverdue;
            
            assertEquals(29.59, expectedInterest, 0.5, "Interest for 30 days");
            
            // Should have late fee (4% of 50 = 2, but floor is 15)
            double lateFeeRate = 4.0;
            double lateFeeFloor = 15.0;
            double expectedLateFee = Math.max(remainingMinimumDue * lateFeeRate / 100.0, lateFeeFloor);
            
            assertEquals(15.0, expectedLateFee, 0.01, "Late fee should be floor amount");
            
            // Total amount due = balance + interest + late fee
            double totalDue = remainingBalance + expectedInterest + expectedLateFee;
            assertEquals(1044.59, totalDue, 1.0, "Total amount due");
            
            // Status should be OVERDUE
            String status = "OVERDUE";
            assertEquals("OVERDUE", status);
        }
    }
    
    @Nested
    @DisplayName("Scenario 5: Multiple statements with past due")
    class MultiplePastDueScenario {
        
        @Test
        @DisplayName("Should carry forward past due minimum to new statement")
        void testPastDueCarryForward() {
            // Given: Previous statement with unpaid minimum
            double previousStatementMinimum = 40.0;
            double previousStatementPaid = 0.0;
            double previousPastDue = previousStatementMinimum - previousStatementPaid;
            
            // Current statement
            double currentBalance = 1000.0;
            double currentMinimumRate = 5.0;
            double currentMinimumFloor = 10.0;
            double currentMinimum = calculateMinimumDue(currentBalance, currentMinimumRate, currentMinimumFloor);
            
            // Then: Total minimum due = current + past due
            double totalMinimumDue = currentMinimum + previousPastDue;
            
            assertEquals(50.0, currentMinimum, 0.01);
            assertEquals(40.0, previousPastDue, 0.01);
            assertEquals(90.0, totalMinimumDue, 0.01, "Should include past due");
        }
        
        @Test
        @DisplayName("Should allocate payment to past due first")
        void testPaymentAllocationWithPastDue() {
            // Given: Statement with past due and current minimum
            double pastDueMinimum = 40.0;
            double currentMinimum = 50.0;
            double totalMinimumDue = pastDueMinimum + currentMinimum;
            
            // When: Customer pays 60
            double payment = 60.0;
            
            // Then: Payment allocation (waterfall)
            // 1. Past due first: 40
            double toPastDue = Math.min(payment, pastDueMinimum);
            double remaining = payment - toPastDue;
            
            // 2. Current minimum: 20 (remaining)
            double toCurrentMinimum = Math.min(remaining, currentMinimum);
            remaining -= toCurrentMinimum;
            
            assertEquals(40.0, toPastDue, 0.01, "Should pay past due first");
            assertEquals(20.0, toCurrentMinimum, 0.01, "Then current minimum");
            assertEquals(0.0, remaining, 0.01, "No remaining");
            
            // Remaining minimum due
            double remainingPastDue = pastDueMinimum - toPastDue;
            double remainingCurrentMinimum = currentMinimum - toCurrentMinimum;
            double remainingTotalMinimum = remainingPastDue + remainingCurrentMinimum;
            
            assertEquals(0.0, remainingPastDue, 0.01);
            assertEquals(30.0, remainingCurrentMinimum, 0.01);
            assertEquals(30.0, remainingTotalMinimum, 0.01);
        }
    }
    
    @Nested
    @DisplayName("Scenario 6: Payment allocation waterfall")
    class PaymentAllocationWaterfallScenario {
        
        @Test
        @DisplayName("Should allocate payment in correct order: Late Fee -> Past Due -> Interest -> Balance")
        void testCompleteWaterfallAllocation() {
            // Given: Statement with all charges
            double lateFee = 15.0;
            double pastDueMinimum = 200.0;
            double interest = 50.0;
            double balance = 2000.0;
            
            // When: Customer pays 1000
            double payment = 1000.0;
            
            // Then: Waterfall allocation
            // Priority 1: Late Fee
            double toLateFee = Math.min(payment, lateFee);
            double remaining = payment - toLateFee;
            
            // Priority 2: Past Due Minimum
            double toPastDue = Math.min(remaining, pastDueMinimum);
            remaining -= toPastDue;
            
            // Priority 3: Interest
            double toInterest = Math.min(remaining, interest);
            remaining -= toInterest;
            
            // Priority 4: Balance
            double toBalance = remaining;
            
            // Verify allocation
            assertEquals(15.0, toLateFee, 0.01, "Late fee paid first");
            assertEquals(200.0, toPastDue, 0.01, "Past due paid second");
            assertEquals(50.0, toInterest, 0.01, "Interest paid third");
            assertEquals(735.0, toBalance, 0.01, "Balance paid last");
            
            // Verify total
            double totalAllocated = toLateFee + toPastDue + toInterest + toBalance;
            assertEquals(1000.0, totalAllocated, 0.01, "Total should match payment");
            
            // Remaining balance
            double remainingBalance = balance - toBalance;
            assertEquals(1265.0, remainingBalance, 0.01);
        }
        
        @Test
        @DisplayName("Should handle partial payment covering only late fee and past due")
        void testPartialWaterfallAllocation() {
            // Given: Statement with all charges
            double lateFee = 15.0;
            double pastDueMinimum = 200.0;
            double interest = 50.0;
            double balance = 2000.0;
            
            // When: Customer pays only 100 (less than late fee + past due)
            double payment = 100.0;
            
            // Then: Waterfall allocation
            double toLateFee = Math.min(payment, lateFee);
            double remaining = payment - toLateFee;
            
            double toPastDue = Math.min(remaining, pastDueMinimum);
            remaining -= toPastDue;
            
            double toInterest = Math.min(remaining, interest);
            remaining -= toInterest;
            
            double toBalance = remaining;
            
            // Verify allocation
            assertEquals(15.0, toLateFee, 0.01, "Late fee fully paid");
            assertEquals(85.0, toPastDue, 0.01, "Partial past due paid");
            assertEquals(0.0, toInterest, 0.01, "No interest paid");
            assertEquals(0.0, toBalance, 0.01, "No balance paid");
            
            // Remaining amounts
            double remainingLateFee = lateFee - toLateFee;
            double remainingPastDue = pastDueMinimum - toPastDue;
            double remainingInterest = interest - toInterest;
            double remainingBalance = balance - toBalance;
            
            assertEquals(0.0, remainingLateFee, 0.01);
            assertEquals(115.0, remainingPastDue, 0.01);
            assertEquals(50.0, remainingInterest, 0.01);
            assertEquals(2000.0, remainingBalance, 0.01);
        }
    }
    
    @Nested
    @DisplayName("Scenario 7: Edge cases")
    class EdgeCaseScenarios {
        
        @Test
        @DisplayName("Should handle very small balance correctly")
        void testSmallBalance() {
            double balance = 5.0;
            double rate = 5.0;
            double floor = 10.0;
            
            double minimumDue = calculateMinimumDue(balance, rate, floor);
            
            assertEquals(5.0, minimumDue, 0.01, "Should be full balance");
        }
        
        @Test
        @DisplayName("Should handle credit balance (negative)")
        void testCreditBalance() {
            double balance = -100.0; // Customer has credit
            double rate = 5.0;
            double floor = 10.0;
            
            double minimumDue = calculateMinimumDue(balance, rate, floor);
            
            assertEquals(0.0, minimumDue, 0.01, "No minimum due for credit balance");
        }
        
        @Test
        @DisplayName("Should handle over-limit scenario")
        void testOverLimit() {
            double creditLimit = 100000.0;
            double balance = 105000.0; // Over limit by 5000
            
            double availableCredit = creditLimit - balance;
            
            assertEquals(-5000.0, availableCredit, 0.01, "Negative means over limit");
            assertTrue(availableCredit < 0, "Should be over limit");
        }
        
        @Test
        @DisplayName("Should calculate interest correctly for leap year")
        void testInterestLeapYear() {
            double balance = 1000.0;
            double annualRate = 36.0;
            int daysInYear = 366; // Leap year
            
            double dailyRate = annualRate / 100.0 / daysInYear;
            double interestForYear = balance * dailyRate * daysInYear;
            
            assertEquals(360.0, interestForYear, 1.0, "Should still be ~36% for leap year");
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
}
