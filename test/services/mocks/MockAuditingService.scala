/*
 * Copyright 2023 HM Revenue & Customs
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

package services.mocks

import org.mockito.ArgumentMatchers
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{reset, verify, when}
import org.scalatest.{BeforeAndAfterEach, Suite}
import org.scalatestplus.mockito.MockitoSugar
import play.api.mvc.Request
import services.{AuditModel, AuditingService, JsonAuditModel}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.audit.http.connector.AuditResult

import scala.concurrent.Future

trait MockAuditingService extends MockitoSugar with BeforeAndAfterEach {
  suite: Suite =>

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(mockAuditingService)
    when(mockAuditingService.audit(any[AuditModel]())(any(), any())).thenReturn(Future.successful(AuditResult.Success))
    when(mockAuditingService.audit(any[JsonAuditModel]())(any(), any())).thenReturn(Future.successful(AuditResult.Success))
  }

  val mockAuditingService: AuditingService = mock[AuditingService]

  def verifyAudit(model: AuditModel): Unit =
    verify(mockAuditingService).audit(
      ArgumentMatchers.eq(model)
    )(
      ArgumentMatchers.any[HeaderCarrier],
      ArgumentMatchers.any[Request[_]]
    )

  def verifyAudit(model: JsonAuditModel): Unit =
    verify(mockAuditingService).audit(
      ArgumentMatchers.eq(model)
    )(
      ArgumentMatchers.any[HeaderCarrier],
      ArgumentMatchers.any[Request[_]]
    )
}
