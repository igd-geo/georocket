package io.georocket.commands

import com.github.tomakehurst.wiremock.client.VerificationException
import com.github.tomakehurst.wiremock.client.WireMock.aResponse
import com.github.tomakehurst.wiremock.client.WireMock.delete
import com.github.tomakehurst.wiremock.client.WireMock.deleteRequestedFor
import com.github.tomakehurst.wiremock.client.WireMock.stubFor
import com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo
import com.github.tomakehurst.wiremock.client.WireMock.verify
import io.vertx.core.Handler
import io.vertx.ext.unit.TestContext
import io.vertx.ext.unit.junit.VertxUnitRunner
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Test for [DeleteCommand]
 */
@RunWith(VertxUnitRunner::class)
class DeleteCommandTest : CommandTestBase<DeleteCommand>() {
  override val cmd = DeleteCommand()

  /**
   * Test no layer
   */
  @Test
  fun noLayer(context: TestContext) {
    val async = context.async()
    cmd.endHandler = Handler { exitCode ->
      context.assertEquals(1, exitCode)
      async.complete()
    }
    cmd.run(arrayOf(), input, out)
  }

  /**
   * Test empty layer
   */
  @Test
  fun emptyLayer(context: TestContext) {
    val async = context.async()
    cmd.endHandler = Handler { exitCode ->
      context.assertEquals(1, exitCode)
      async.complete()
    }
    cmd.run(arrayOf(""), input, out)
  }

  /**
   * Verify that a certain DELETE request has been made
   */
  private fun verifyDeleted(url: String, context: TestContext) {
    try {
      verify(deleteRequestedFor(urlEqualTo(url)))
    } catch (e: VerificationException) {
      context.fail(e)
    }
  }

  /**
   * Test a delete with a simple query
   */
  @Test
  fun simpleQueryDelete(context: TestContext) {
    val url = "/store/?search=test"
    stubFor(delete(urlEqualTo(url))
        .willReturn(aResponse()
            .withStatus(204)))

    val async = context.async()
    cmd.endHandler = Handler { exitCode ->
      context.assertEquals(0, exitCode)
      verifyDeleted(url, context)
      async.complete()
    }

    cmd.run(arrayOf("test"), input, out)
  }

  /**
   * Test a delete with query that consists of two terms
   */
  @Test
  fun twoTermsQueryDelete(context: TestContext) {
    val url = "/store/?search=test1%20test2"
    stubFor(delete(urlEqualTo(url))
        .willReturn(aResponse()
            .withStatus(204)))

    val async = context.async()
    cmd.endHandler = Handler { exitCode ->
      context.assertEquals(0, exitCode)
      verifyDeleted(url, context)
      async.complete()
    }

    cmd.run(arrayOf("test1", "test2"), input, out)
  }

  /**
   * Test to delete the root layer
   */
  @Test
  fun rootLayer(context: TestContext) {
    val url = "/store/"
    stubFor(delete(urlEqualTo(url))
        .willReturn(aResponse()
            .withStatus(204)))

    val async = context.async()
    cmd.endHandler = Handler { exitCode ->
      context.assertEquals(0, exitCode)
      verifyDeleted(url, context)
      async.complete()
    }

    cmd.run(arrayOf("-l", "/"), input, out)
  }

  /**
   * Test a delete with a layer but no query
   */
  @Test
  fun layerNoQuery(context: TestContext) {
    val url = "/store/hello/world/"
    stubFor(delete(urlEqualTo(url))
        .willReturn(aResponse()
            .withStatus(204)))

    val async = context.async()
    cmd.endHandler = Handler { exitCode ->
      context.assertEquals(0, exitCode)
      verifyDeleted(url, context)
      async.complete()
    }

    cmd.run(arrayOf("-l", "hello/world"), input, out)
  }

  /**
   * Test a delete with a layer and a query
   */
  @Test
  fun layerQuery(context: TestContext) {
    val url = "/store/hello/world/?search=test"
    stubFor(delete(urlEqualTo(url))
        .willReturn(aResponse()
            .withStatus(204)))

    val async = context.async()
    cmd.endHandler = Handler { exitCode ->
      context.assertEquals(0, exitCode)
      verifyDeleted(url, context)
      async.complete()
    }

    cmd.run(arrayOf("-l", "hello/world", "test"), input, out)
  }
}
