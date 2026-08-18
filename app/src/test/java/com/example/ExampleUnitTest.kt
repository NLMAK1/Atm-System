package com.example

import com.example.atm.core.exceptions.DailyLimitExceededError
import com.example.atm.core.exceptions.InsufficientBalanceError
import com.example.atm.core.model.CashDispenser
import com.example.atm.core.model.CurrentAccount
import com.example.atm.core.model.SavingsAccount
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class ExampleUnitTest {

  @Test
  fun savingsAccount_enforcesMinimumBalance() {
    val savings = SavingsAccount(
      accountNumber = "SAV-100",
      accountHolderName = "Test User",
      customerId = "CUST-1",
      balance = 20000.0,
      pin = "1234"
    )

    // Withdrawing 16000 + fee 50 = total 16050 leaves 3950, below min balance 5000
    try {
      savings.withdraw(16000.0)
      fail("Should have thrown InsufficientBalanceError")
    } catch (e: InsufficientBalanceError) {
      assertTrue(e.message?.contains("minimum balance") == true || e.message?.contains("Minimum balance") == true)
    }

    // Withdrawing 10000 + fee 50 = total 10050 leaves 9950 >= 5000 (Allowed)
    val newBal = savings.withdraw(10000.0)
    assertEquals(9950.0, newBal, 0.01)
  }

  @Test
  fun currentAccount_allowsOverdraftUpToLimit() {
    val current = CurrentAccount(
      accountNumber = "CUR-100",
      accountHolderName = "Test Biz",
      customerId = "CUST-2",
      balance = 5000.0,
      pin = "1234"
    )

    // Current account can go up to -50,000 (overdraft limit)
    // Withdraw 30,000 (0 fee) -> balance becomes -25,000
    val balanceAfter = current.withdraw(30000.0)
    assertEquals(-25000.0, balanceAfter, 0.01)

    // Attempting to withdraw another 35,000 (would be -60,000 > overdraft 50,000)
    try {
      current.withdraw(35000.0)
      fail("Should exceed overdraft limit")
    } catch (e: InsufficientBalanceError) {
      assertTrue(e.message?.contains("overdraft") == true || e.message?.contains("Overdraft") == true)
    }
  }

  @Test
  fun cashDispenser_optimalDenominationGreedyAlgorithm() {
    val dispenser = CashDispenser()
    dispenser.setInventory(notes5000 = 10, notes1000 = 20, notes500 = 30)

    val plan = dispenser.calculateDispense(17500.0)
    // 17500 = 3 x 5000 (15000) + 2 x 1000 (2000) + 1 x 500 (500)
    assertEquals(3, plan[5000])
    assertEquals(2, plan[1000])
    assertEquals(1, plan[500])

    dispenser.dispense(17500.0)
    assertEquals(7, dispenser.getInventory()[5000])
    assertEquals(18, dispenser.getInventory()[1000])
    assertEquals(29, dispenser.getInventory()[500])
  }

  @Test
  fun account_dailyLimitEnforcement() {
    val savings = SavingsAccount(
      accountNumber = "SAV-200",
      accountHolderName = "Daily User",
      customerId = "CUST-3",
      balance = 200000.0,
      pin = "1234"
    )

    // Daily limit for savings is 100,000, single withdrawal limit is 50,000
    savings.withdraw(50000.0)
    savings.withdraw(45000.0)
    assertEquals(95000.0, savings.getDailyWithdrawnAmount(), 0.01)

    try {
      savings.withdraw(10000.0) // 95,000 + 10,000 = 105,000 > 100,000
      fail("Should throw DailyLimitExceededError")
    } catch (e: DailyLimitExceededError) {
      assertEquals(100000.0, e.dailyLimit, 0.01)
    }
  }
}



