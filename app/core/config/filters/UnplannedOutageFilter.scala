/*
 * Copyright 2020 HM Revenue & Customs
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

package core.config.filters

import javax.inject.Inject

import akka.util.ByteString
import core.config.AppConfig
import core.config.featureswitch.{FeatureSwitching, UnplannedShutter}
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.libs.streams.Accumulator
import play.api.mvc.Results._
import play.api.mvc.{EssentialAction, EssentialFilter}
import views.html.unplanned_outage

import scala.concurrent.ExecutionContext

class UnplannedOutageFilter @Inject()(val messagesApi: MessagesApi,
                                      val appConfig: AppConfig,
                                      implicit val ec: ExecutionContext) extends EssentialFilter with FeatureSwitching with I18nSupport {

  override def apply(nextFilter: EssentialAction) = new EssentialAction {

    import play.api.mvc._

    override def apply(rh: RequestHeader): Accumulator[ByteString, Result] =
      if (isEnabled(UnplannedShutter) && rh.path.contains("/report-quarterly/income-and-expenses/sign-up"))
        nextFilter(rh).map(_ => Ok(unplanned_outage()(Request(rh, ""), request2Messages(rh), appConfig)))
      else
        nextFilter(rh)
  }

}
