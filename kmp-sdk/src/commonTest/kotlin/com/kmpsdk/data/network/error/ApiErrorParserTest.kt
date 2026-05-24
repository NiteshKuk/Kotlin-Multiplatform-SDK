package com.kmpsdk.data.network.error

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ApiErrorParserTest {

    private val parser = ApiErrorParser()

    @Test
    fun parsesStandardCodeAndMessage() {
        val error = parser.parse(
            httpCode = 404,
            responseBody = """{"code":"USER_NOT_FOUND","message":"User not found"}""",
        )

        assertEquals("USER_NOT_FOUND", error.code)
        assertEquals("User not found", error.message)
    }

    @Test
    fun parsesOAuthStyleError() {
        val error = parser.parse(
            httpCode = 400,
            responseBody = """{"error":"invalid_grant","error_description":"Token expired"}""",
        )

        assertEquals("invalid_grant", error.code)
        assertEquals("Token expired", error.message)
    }

    @Test
    fun parsesFieldErrorsMap() {
        val error = parser.parse(
            httpCode = 422,
            responseBody = """{"message":"Validation failed","errors":{"email":"invalid","name":"required"}}""",
        )

        assertEquals(2, error.fieldErrors.size)
        assertEquals("invalid", error.fieldErrors["email"])
    }

    @Test
    fun parsesFieldErrorsArray() {
        val error = parser.parse(
            httpCode = 422,
            responseBody = """{"errors":[{"field":"email","message":"must be valid"}]}""",
        )

        assertEquals("must be valid", error.fieldErrors["email"])
    }

    @Test
    fun mapsToBusinessErrorWithApiError() {
        val kmpError = parser.toKmpSdkError(
            httpCode = 422,
            responseBody = """{"code":"VALIDATION","message":"Bad input","errors":{"email":"invalid"}}""",
            fallbackMessage = "failed",
        )

        assertTrue(kmpError is com.kmpsdk.domain.error.KmpSdkError.Business)
        val business = kmpError as com.kmpsdk.domain.error.KmpSdkError.Business
        assertEquals(422, business.httpCode)
        assertEquals("VALIDATION", business.code)
        assertEquals("invalid", business.fieldErrors["email"])
        assertEquals("VALIDATION", business.apiError?.code)
    }
}
