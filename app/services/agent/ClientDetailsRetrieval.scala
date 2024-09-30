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

package services.agent

import play.api.mvc.{AnyContent, Request}
import services.NinoService
import uk.gov.hmrc.http.{HeaderCarrier, InternalServerException}
import utilities.UserMatchingSessionUtil.{ClientDetails, UserMatchingSessionRequestUtil}

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class ClientDetailsRetrieval @Inject()(ninoService: NinoService)(implicit ec: ExecutionContext) {

  def getClientDetails(implicit request: Request[_], hc: HeaderCarrier): Future[ClientDetails] = {
    ninoService.getNino map { nino =>
      request.fetchClientName match {
        case Some(name) => ClientDetails(name, nino)
        case None => throw new InternalServerException("[ClientDetailsRetrieval][getClientDetails] - Unable to retrieve name from session")
      }
    }
  }

}
