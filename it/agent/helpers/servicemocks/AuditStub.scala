/*
 * Copyright 2017 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package agent.helpers.servicemocks

import uk.gov.hmrc.http.HeaderCarrier

object AuditStub extends WireMockMethods {
  val appName = "income-tax-subscription-agent-frontend"

  def stubAuditing(): Unit =
    when(method = POST, uri = "/write/audit")
      .thenReturn(status = 200, body = """{"x":2}""")

  def verifyAudit()(implicit hc: HeaderCarrier): Unit = {
    //We cannot verify content of audit body without string matching/regex
    //It is tested in more detail at unit level
    verify(method = POST, uri = "/write/audit")
  }
}