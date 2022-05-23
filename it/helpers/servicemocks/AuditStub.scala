
package helpers.servicemocks

import helpers.WiremockHelper.verifyPost

object AuditStub extends WireMockMethods {

  def stubAuditing(): Unit = {
    when(method = POST, uri = "/write/audit/merged")
      .thenReturn(status = 200, body = """{"x":2}""")

    when(method = POST, uri = "/write/audit")
      .thenReturn(status = 200, body = """{"x":2}""")
  }

  def verifyAudit(count: Option[Int] = None): Unit = {
    //We cannot verify content of audit body without string matching/regex
    //It is tested in more detail at unit level
    verifyPost(uri = "/write/audit", optBody = None, count = count)
  }
}
