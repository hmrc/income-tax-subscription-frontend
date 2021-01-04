/*
 * Copyright 2021 HM Revenue & Customs
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

package testonly.connectors

import config.AppConfig
import javax.inject.Inject
import testonly.models.FeatureSwitchSetting
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse}
import uk.gov.hmrc.play.bootstrap.http.HttpClient

import scala.concurrent.{ExecutionContext, Future}

class EligibilityFeatureSwitchConnector @Inject()(http: HttpClient, appConfig: AppConfig)
                                                 (implicit ec: ExecutionContext) {

  def getEligibilityFeatureSwitches(implicit hc: HeaderCarrier): Future[Map[String, Boolean]] = for {
    featureSwitches <- http.GET[Set[FeatureSwitchSetting]](appConfig.eligibilityFeatureSwitchUrl)
  } yield {
    featureSwitches map { case FeatureSwitchSetting(name, isEnabled) => name -> isEnabled }
    }.toMap

  def submitEligibilityFeatureSwitches(featureSwitches: Set[FeatureSwitchSetting])
                                      (implicit hc: HeaderCarrier): Future[HttpResponse] =
    http.POST(appConfig.eligibilityFeatureSwitchUrl, featureSwitches)

}
