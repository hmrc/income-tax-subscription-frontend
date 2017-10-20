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

package agent.services

import javax.inject.{Inject, Singleton}

import common.Constants._
import connectors.GGAdminConnector
import connectors.models.gg._

import scala.concurrent.Future
import uk.gov.hmrc.http.HeaderCarrier

@Singleton
class KnownFactsService @Inject()(ggAdminConnector: GGAdminConnector) {

  def addKnownFacts(mtditId: String, nino: String)(implicit hc: HeaderCarrier): Future[Either[KnownFactsFailure, KnownFactsSuccess.type]] = {
    val mtditIdKnownFact = TypeValuePair(mtdItsaEnrolmentIdentifierKey, mtditId)
    val ninoKnownFact = TypeValuePair(ninoIdentifierKey, nino)

    val request = KnownFactsRequest(
      List(
        mtditIdKnownFact,
        ninoKnownFact
      )
    )

    ggAdminConnector.addKnownFacts(request)
  }

}
