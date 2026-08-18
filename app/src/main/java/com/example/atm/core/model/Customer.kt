package com.example.atm.core.model

/**
 * Class: Customer
 * Represents a bank customer holding personal details, multiple accounts, and multiple cards.
 * Demonstrates Aggregation and Composition relationships.
 */
class Customer(
    val customerId: String,
    val name: String,
    val phone: String,
    val email: String,
    private val _accounts: MutableList<Account> = mutableListOf(),
    private val _cards: MutableList<Card> = mutableListOf()
) {

    val accounts: List<Account>
        get() = _accounts.toList()

    val cards: List<Card>
        get() = _cards.toList()

    fun addAccount(account: Account) {
        if (_accounts.none { it.accountNumber == account.accountNumber }) {
            _accounts.add(account)
        }
    }

    fun addCard(card: Card) {
        if (_cards.none { it.cardNumber == card.cardNumber }) {
            _cards.add(card)
        }
    }

    fun findAccount(accountNumber: String): Account? {
        return _accounts.find { it.accountNumber == accountNumber }
    }

    fun findCard(cardNumber: String): Card? {
        return _cards.find { it.cardNumber == cardNumber }
    }
}
