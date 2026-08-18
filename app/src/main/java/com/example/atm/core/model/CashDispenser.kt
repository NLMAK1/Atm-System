package com.example.atm.core.model

import com.example.atm.core.exceptions.DenominationUnavailableError
import com.example.atm.core.exceptions.InsufficientATMFundsError
import com.example.atm.core.exceptions.InvalidAmountError

/**
 * Class: CashDispenser
 * Handles hardware ATM note cassette management, greedy denomination dispensing, and refills.
 */
class CashDispenser(
    initialNotes5000: Int = 10,
    initialNotes1000: Int = 20,
    initialNotes500: Int = 10
) {
    // Encapsulated note inventories: Key = Denomination (5000, 1000, 500), Value = Count
    private val inventory = mutableMapOf<Int, Int>(
        5000 to initialNotes5000,
        1000 to initialNotes1000,
        500 to initialNotes500
    )

    fun getInventory(): Map<Int, Int> = inventory.toMap()

    fun getTotalCash(): Double {
        return inventory.entries.sumOf { it.key.toDouble() * it.value }
    }

    /**
     * Calculates note combination required for dispensing an amount.
     * Uses greedy denomination allocation with inventory constraint checks.
     */
    fun calculateDispense(amount: Double): Map<Int, Int> {
        if (amount <= 0 || amount % 500 != 0.0) {
            throw InvalidAmountError("Withdrawal amount must be in multiples of Rs. 500. Requested: Rs. $amount")
        }

        val totalCash = getTotalCash()
        if (amount > totalCash) {
            throw InsufficientATMFundsError(totalCash, amount)
        }

        var remaining = amount.toLong()
        val result = mutableMapOf<Int, Int>()

        // Denominations in descending order: 5000, 1000, 500
        val denominations = listOf(5000, 1000, 500)
        for (denom in denominations) {
            val available = inventory[denom] ?: 0
            if (available > 0 && remaining >= denom) {
                val needed = remaining / denom
                val take = minOf(needed, available.toLong()).toInt()
                if (take > 0) {
                    result[denom] = take
                    remaining -= (take.toLong() * denom)
                }
            }
        }

        if (remaining != 0L) {
            throw DenominationUnavailableError(
                amount,
                "ATM cannot dispense Rs. ${"%,.2f".format(amount)} with available note inventory: " +
                        "5000s (${inventory[5000]}), 1000s (${inventory[1000]}), 500s (${inventory[500]})."
            )
        }

        return result
    }

    /**
     * Deducts dispensed notes from ATM inventory and returns dispensed notes map.
     */
    fun dispense(amount: Double): Map<Int, Int> {
        val notesToDispense = calculateDispense(amount)
        for ((denom, count) in notesToDispense) {
            inventory[denom] = (inventory[denom] ?: 0) - count
        }
        return notesToDispense
    }

    /**
     * Refills the ATM note cassettes.
     */
    fun refill(notes5000: Int, notes1000: Int, notes500: Int) {
        inventory[5000] = (inventory[5000] ?: 0) + maxOf(0, notes5000)
        inventory[1000] = (inventory[1000] ?: 0) + maxOf(0, notes1000)
        inventory[500] = (inventory[500] ?: 0) + maxOf(0, notes500)
    }

    /**
     * Sets exact inventory count (for hydration from DB).
     */
    fun setInventory(notes5000: Int, notes1000: Int, notes500: Int) {
        inventory[5000] = maxOf(0, notes5000)
        inventory[1000] = maxOf(0, notes1000)
        inventory[500] = maxOf(0, notes500)
    }
}
