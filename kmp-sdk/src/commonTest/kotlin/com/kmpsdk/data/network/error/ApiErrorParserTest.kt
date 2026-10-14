package com.kmpsdk.data.network.error

import com.kmpsdk.domain.error.KmpSdkError
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class ApiErrorParserTest {
    private val parser = ApiErrorParser()

    @Test
    fun jamBusiness403MapsToBusinessNotAuth() {
        val body =
            """
            {
              "success": false,
              "error": {
                "code": "JAM_SHORT_INVALID_SOURCE",
                "message": "You may only create a Short from your own contribution."
              }
            }
            """.trimIndent()

        val error =
            parser.toKmpSdkError(
                httpCode = 403,
                responseBody = body,
                fallbackMessage = "Forbidden",
            )

        assertIs<KmpSdkError.Business>(error)
        assertEquals("JAM_SHORT_INVALID_SOURCE", error.code)
        assertEquals(403, error.httpCode)
    }

    @Test
    fun nonEnvelope403MapsToAuth() {
        val error =
            parser.toKmpSdkError(
                httpCode = 403,
                responseBody = """{"message":"Forbidden"}""",
                fallbackMessage = "Forbidden",
            )

        assertIs<KmpSdkError.Auth>(error)
    }
}
