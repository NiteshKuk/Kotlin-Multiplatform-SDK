package com.kmpsdk.domain.error

import com.kmpsdk.data.network.KmpHttpException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class KmpSdkErrorHttpMappingTest {

    @Test
    fun authErrorExposesHttpCodeAndBody() {
        val error = KmpSdkError.Auth(
            message = "Unauthorized",
            httpCode = 401,
            responseBody = """{"error":"invalid_token"}""",
        )

        assertEquals(401, error.httpStatusCode)
        assertEquals("""{"error":"invalid_token"}""", error.responseBodyOrNull)
    }

    @Test
    fun businessErrorExposes404() {
        val error = KmpSdkError.Business(
            message = "Not found",
            httpCode = 404,
            responseBody = "Not Found",
        )

        assertEquals(404, error.httpStatusCode)
        assertEquals(true, error.isClientError())
    }

    @Test
    fun serverErrorExposes500() {
        val error = KmpSdkError.Server(
            message = "Internal error",
            httpCode = 500,
        )

        assertEquals(500, error.httpStatusCode)
        assertEquals(true, error.isServerError())
    }

    @Test
    fun networkErrorHasNoHttpCode() {
        val error = KmpSdkError.Network()

        assertNull(error.httpStatusCode)
        assertNull(error.responseBodyOrNull)
    }

    @Test
    fun resultFailureExposesHttpFields() {
        val result = KmpSdkResult.Failure(
            KmpSdkError.Business(
                message = "Validation failed",
                httpCode = 422,
                responseBody = """{"field":"email"}""",
            ),
        )

        assertEquals(422, result.httpStatusCode)
        assertEquals("""{"field":"email"}""", result.responseBody)
    }

    @Test
    fun httpExceptionMappingPreservesStatusAndBody() {
        val exception = KmpHttpException(
            httpCode = 418,
            responseBody = "I'm a teapot",
            message = "I'm a teapot",
        )

        val error = exception.toKmpSdkError() as KmpSdkError.Unknown

        assertEquals(418, error.httpStatusCode)
        assertEquals("I'm a teapot", error.responseBodyOrNull)
    }
}
