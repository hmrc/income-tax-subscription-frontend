/*
 * Copyright 2024 HM Revenue & Customs
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

package controllers

import play.api.Logging
import play.api.data.Form
import play.api.i18n.I18nSupport
import play.api.mvc.{MessagesControllerComponents, RequestHeader}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.frontend.controller.{FrontendBaseController, FrontendController}
import uk.gov.hmrc.play.http.HeaderCarrierConverter

abstract class SignUpBaseController(implicit cc: MessagesControllerComponents) extends FrontendController(cc) with I18nSupport with Logging {

  override implicit def hc(implicit rh: RequestHeader): HeaderCarrier = {
    HeaderCarrierConverter.fromRequestAndSession(rh, rh.session)
  }

  implicit class FormUtil[T](form: Form[T]) {
    def fill(data: Option[T]): Form[T] = data.fold(form)(form.fill)
  }

}
