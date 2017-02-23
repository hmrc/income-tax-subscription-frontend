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

package connectors.mocks

import connectors.EnrolmentConnector
import connectors.models.Enrolment
import uk.gov.hmrc.play.http.HeaderCarrier
import utils.UnitTestTrait

import scala.concurrent.Future

trait MockEnrolmentConnector extends UnitTestTrait
  with MockHttp {

  object TestEnrolmentConnector extends EnrolmentConnector(appConfig, http = mockHttpGet) {
    override def getIncomeTaxSAEnrolment(uri: String)(implicit hc: HeaderCarrier): Future[Option[Enrolment]] =
      hc.userId.fold(Future.successful(None: Option[Enrolment]))(userId => userId.value match {
        case auth.mockEnrolled => Future.successful(Some(Enrolment("", Seq(), "Activated")))
        case _ => Future.successful(None)
      })
  }

}
