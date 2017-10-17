package helpers.servicemocks

object AuditStub extends WireMockMethods {

  def stubAuditing(): Unit =
    when(method = POST, uri = "/write/core.audit")
      .thenReturn(status = 200, body = """{"x":2}""")
}