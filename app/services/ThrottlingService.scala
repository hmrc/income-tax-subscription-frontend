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

package services

import javax.inject.{Inject, Singleton}

import audit.Logging
import auth.IncomeTaxSAUser
import connectors.models.throttling.UserAccess
import connectors.throttling.ThrottlingControlConnector
import uk.gov.hmrc.play.http.{HeaderCarrier, InternalServerException}
import utils.Implicits._

import scala.concurrent.Future

@Singleton
class ThrottlingService @Inject()(throttlingControlConnector: ThrottlingControlConnector,
                                  logging: Logging
                                 ) {

  def checkAccess(implicit user: IncomeTaxSAUser, hc: HeaderCarrier): Future[Option[UserAccess]] = {
    user.nino match {
      case Some(nino) => throttlingControlConnector.checkAccess(nino)
      case None =>
        logging.warn("ThrottlingService.checkAccess: unexpected error, no nino found")
        new InternalServerException("ThrottlingService.checkAccess: unexpected error, no nino found")
    }
  }

}
