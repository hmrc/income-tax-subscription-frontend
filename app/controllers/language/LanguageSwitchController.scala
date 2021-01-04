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

package controllers.language

import config.AppConfig
import javax.inject.{Inject, Singleton}
import play.api.Configuration
import play.api.i18n.Lang
import play.api.mvc.{Action, AnyContent, Call, MessagesControllerComponents}
import uk.gov.hmrc.play.language.{LanguageController, LanguageUtils}

@Singleton
class LanguageSwitchController @Inject()(mcc: MessagesControllerComponents, appConfig: AppConfig, languageUtils: LanguageUtils,
                                         configuration: Configuration) extends LanguageController(configuration, languageUtils, mcc){

  def langToCall(lang: String): String => Call = appConfig.routeToSwitchLanguage

  protected def fallbackURL: String = controllers.usermatching.routes.HomeController.home().url

  def languageMap: Map[String, Lang] = appConfig.languageMap

  override def switchToLanguage(language: String): Action[AnyContent] = Action { implicit request =>
    val lang = languageMap.getOrElse(language, languageUtils.getCurrentLang)

    val redirectURL = request.headers.get(REFERER).getOrElse(fallbackURL)

    Redirect(redirectURL).withLang(Lang.apply(lang.code))
  }
}
