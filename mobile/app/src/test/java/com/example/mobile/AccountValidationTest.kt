package com.example.mobile

import com.example.mobile.ui.screens.AccountState
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AccountValidationTest {

    @Test
    fun register_shortUsername_fails() = runTest {
        val result = AccountState.register("ab", "test@test.com", "password123", "password123")
        assertTrue(result.isFailure)
        assertEquals(
            "Le pseudo doit faire 3 caractères minimum.",
            result.exceptionOrNull()?.message
        )
    }

    @Test
    fun register_emptyUsername_fails() = runTest {
        val result = AccountState.register("", "test@test.com", "password123", "password123")
        assertTrue(result.isFailure)
        assertEquals(
            "Le pseudo doit faire 3 caractères minimum.",
            result.exceptionOrNull()?.message
        )
    }

    @Test
    fun register_twoCharUsername_fails() = runTest {
        val result = AccountState.register("AB", "test@test.com", "password123", "password123")
        assertTrue(result.isFailure)
        assertEquals(
            "Le pseudo doit faire 3 caractères minimum.",
            result.exceptionOrNull()?.message
        )
    }

    @Test
    fun register_invalidEmail_noAt_fails() = runTest {
        val result = AccountState.register("player1", "invalidemail.com", "password123", "password123")
        assertTrue(result.isFailure)
        assertEquals("Email invalide.", result.exceptionOrNull()?.message)
    }

    @Test
    fun register_invalidEmail_empty_fails() = runTest {
        val result = AccountState.register("player1", "", "password123", "password123")
        assertTrue(result.isFailure)
        assertEquals("Email invalide.", result.exceptionOrNull()?.message)
    }

    @Test
    fun register_shortPassword_fails() = runTest {
        val result = AccountState.register("player1", "test@test.com", "12345", "12345")
        assertTrue(result.isFailure)
        assertEquals(
            "Le mot de passe doit faire 6 caractères minimum.",
            result.exceptionOrNull()?.message
        )
    }

    @Test
    fun register_emptyPassword_fails() = runTest {
        val result = AccountState.register("player1", "test@test.com", "", "")
        assertTrue(result.isFailure)
        assertEquals(
            "Le mot de passe doit faire 6 caractères minimum.",
            result.exceptionOrNull()?.message
        )
    }

    @Test
    fun register_fiveCharPassword_fails() = runTest {
        val result = AccountState.register("player1", "test@test.com", "abcde", "abcde")
        assertTrue(result.isFailure)
        assertEquals(
            "Le mot de passe doit faire 6 caractères minimum.",
            result.exceptionOrNull()?.message
        )
    }

    @Test
    fun register_passwordMismatch_fails() = runTest {
        val result = AccountState.register("player1", "test@test.com", "password123", "password456")
        assertTrue(result.isFailure)
        assertEquals(
            "Les mots de passe ne correspondent pas.",
            result.exceptionOrNull()?.message
        )
    }

    @Test
    fun register_validationOrder_usernameFirst() = runTest {
        val result = AccountState.register("ab", "invalid", "12345", "other")
        assertTrue(result.isFailure)
        assertEquals(
            "Le pseudo doit faire 3 caractères minimum.",
            result.exceptionOrNull()?.message
        )
    }

    @Test
    fun register_validationOrder_emailSecond() = runTest {
        val result = AccountState.register("player1", "invalid", "12345", "other")
        assertTrue(result.isFailure)
        assertEquals("Email invalide.", result.exceptionOrNull()?.message)
    }

    @Test
    fun register_validationOrder_passwordLengthThird() = runTest {
        val result = AccountState.register("player1", "a@b.com", "12345", "12345")
        assertTrue(result.isFailure)
        assertEquals(
            "Le mot de passe doit faire 6 caractères minimum.",
            result.exceptionOrNull()?.message
        )
    }

    @Test
    fun register_validationOrder_passwordMatchFourth() = runTest {
        val result = AccountState.register("player1", "a@b.com", "password1", "password2")
        assertTrue(result.isFailure)
        assertEquals(
            "Les mots de passe ne correspondent pas.",
            result.exceptionOrNull()?.message
        )
    }

    @Test
    fun register_exactlyThreeCharUsername_passesUsernameValidation() = runTest {
        val result = AccountState.register("abc", "noemail", "password123", "password123")
        assertTrue(result.isFailure)
        assertEquals("Email invalide.", result.exceptionOrNull()?.message)
    }

    @Test
    fun register_exactlySixCharPassword_passesPasswordLengthValidation() = runTest {
        val result = AccountState.register("player1", "a@b.com", "123456", "654321")
        assertTrue(result.isFailure)
        assertEquals(
            "Les mots de passe ne correspondent pas.",
            result.exceptionOrNull()?.message
        )
    }
}
