/*
 * Copyright 2019 HM Revenue & Customs
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

package usermatching.services

import core.config.AppConfig
import javax.inject.{Inject, Singleton}
import uk.gov.hmrc.http.{HeaderCarrier, InternalServerException}
import usermatching.connectors.CitizenDetailsConnector
import usermatching.models.CitizenDetailsSuccess

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class CitizenDetailsService @Inject()(appConfig: AppConfig,
                                      citizenDetailsConnector: CitizenDetailsConnector)(implicit ec: ExecutionContext) {

  /* N.B. this is header update is to be used in conjunction with the test only route
*  MatchingStubController
*  the True-Client-IP must match the testId in in testonly.core.connectors.Request sent
*  The hc must not be edited in production
*/
  def amendHCForTest(implicit hc: HeaderCarrier): HeaderCarrier =
    if(appConfig.hasEnabledTestOnlyRoutes) hc.copy(trueClientIp = Some("ITSA"))
    else hc

  def lookupUtr(nino: String)(implicit hc: HeaderCarrier): Future[Option[String]] =
    citizenDetailsConnector.lookupUtr(nino)(amendHCForTest) map {
      case Right(Some(CitizenDetailsSuccess(utr, _))) =>
        utr
      case _ =>
        throw new InternalServerException("unexpected error calling the citizen details service")
    }

  def lookupNino(utr: String)(implicit hc: HeaderCarrier): Future[String] =
    citizenDetailsConnector.lookupNino(utr)(amendHCForTest) map {
      case Right(Some(CitizenDetailsSuccess(_, nino))) =>
        nino
      case _ =>
        throw new InternalServerException("unexpected error calling the citizen details service")
    }

}
