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

package auth

import auth.ggUser._
import connectors.mocks.MockHttp
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector
import uk.gov.hmrc.play.frontend.auth.connectors.domain.Authority
import uk.gov.hmrc.play.http.{HeaderCarrier, HttpGet}
import utils.UnitTestTrait

import scala.concurrent.Future

trait MockAuthConnector extends UnitTestTrait with MockHttp {

  object TestAuthConnector extends AuthConnector {
    override lazy val http: HttpGet = mockHttpGet
    override val serviceUrl: String = ""

    override def currentAuthority(implicit hc: HeaderCarrier): Future[Option[Authority]] = {
      hc.userId.fold[Future[Option[Authority]]](Future.successful(None))(userId => userId.value match {
        case auth.mockAuthorisedUserIdCL500 => Future.successful(Some(userCL500))
        case auth.mockAuthorisedUserIdCL200 => Future.successful(Some(userCL200))
        case auth.mockUpliftUserIdCL200NoAccounts => Future.successful(Some(userCL200NoAccounts))
        case auth.mockUpliftUserIdCL100 => Future.successful(Some(userCL100))
        case auth.mockUpliftUserIdCL50 => Future.successful(Some(userCL50))
        case auth.mockWeakUserId => Future.successful(Some(weakStrengthUser))
        case auth.mockEnrolled => Future.successful(Some(userCL200.copy(uri = auth.mockEnrolled)))
      })
    }
  }

}