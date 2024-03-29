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

package connectors.individual.eligibility

import config.AppConfig
import connectors.individual.eligibility.httpparsers.GetEligibilityStatusHttpParser._
import models.EligibilityStatus
import uk.gov.hmrc.http.{HeaderCarrier, HttpClient}
import utilities.HttpResult.HttpResult

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class GetEligibilityStatusConnector @Inject()(appConfig: AppConfig, http: HttpClient)
                                             (implicit ec: ExecutionContext) {

  def eligibilityUrl(sautr: String): String = s"${appConfig.incomeTaxEligibilityUrl}/eligibility/$sautr"

  def getEligibilityStatus(sautr: String)(implicit hc: HeaderCarrier): Future[HttpResult[EligibilityStatus]] = {
    http.GET(eligibilityUrl(sautr))
  }

}
