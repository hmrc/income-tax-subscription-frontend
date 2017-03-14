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

package controllers

import config.AppConfig
import play.api.i18n.{I18nSupport, Messages}
import play.api.mvc.{Request, Result}
import play.twirl.api.Html
import uk.gov.hmrc.play.frontend.auth.Actions
import uk.gov.hmrc.play.frontend.bootstrap.ShowErrorPage


trait ErrorPageRenderer extends Actions with ShowErrorPage with I18nSupport {

  val applicationConfig: AppConfig

  override def standardErrorTemplate(pageTitle: String, heading: String, message: String)(implicit request: Request[_]): Html =
    views.html.templates.error_template(pageTitle, heading, message)(request, implicitly[Messages], applicationConfig)

  def showBadRequest(implicit request: Request[_]): Result = BadRequest(badRequestTemplate)

  def showNotFound(implicit request: Request[_]): Result = NotFound(notFoundTemplate)

  def showInternalServerError(implicit request: Request[_]): Result = InternalServerError(internalServerErrorTemplate)

}
