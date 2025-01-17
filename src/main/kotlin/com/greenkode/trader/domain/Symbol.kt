package com.greenkode.trader.domain

class Symbol(val name: String) {

    override fun toString(): String {
        return name
    }

    override fun equals(other: Any?): Boolean {

        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Symbol

        if (name != other.name) return false

        return true
    }

    override fun hashCode(): Int {
        return name.hashCode()
    }
}