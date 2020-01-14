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

package controllers.individual.testonly

import core.config.AppConfig
import core.models.{Cash, DateModel}
import core.services.AuthService
import core.utils.Implicits._
import forms.testonly.UnauthorisedAgentSubmissionForm
import incometax.subscription.models._
import incometax.unauthorisedagent.connectors.SubscriptionStoreConnector
import incometax.unauthorisedagent.models.{StoreSubscriptionSuccess, StoredSubscription}
import javax.inject.{Inject, Singleton}
import play.api.data.Form
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, Request}
import play.twirl.api.Html
import testonly.models.UnauthorisedAgentSubmissionModel
import uk.gov.hmrc.play.bootstrap.controller.FrontendController

//$COVERAGE-OFF$Disabling scoverage on this class as it is only intended to be used by the test only controller

@Singleton
class UnauthorisedAgentSubmissionController @Inject()(implicit val applicationConfig: AppConfig,
                                                      val messagesApi: MessagesApi,
                                                      val subscriptionStoreConnector: SubscriptionStoreConnector,
                                                      val authService: AuthService
                                                     ) extends FrontendController with I18nSupport {

  def view(unauthorisedAgentSubmissionForm: Form[UnauthorisedAgentSubmissionModel])(implicit request: Request[_]): Html =
    testonly.views.html.unauthorised_agent_submission(
      unauthorisedAgentSubmissionForm,
      routes.UnauthorisedAgentSubmissionController.submit()
    )

  def show = Action.async { implicit request =>
    authService.authorised() {
      Ok(view(UnauthorisedAgentSubmissionForm.unauthorisedAgentSubmissionForm.form))
    }
  }

  def submit = Action.async { implicit request =>
    authService.authorised() {
      UnauthorisedAgentSubmissionForm.unauthorisedAgentSubmissionForm.bindFromRequest.fold(
        formWithErrors => BadRequest(view(formWithErrors)),
        details => {
          val arn = details.arn.stripSpaces
          val nino = details.nino.stripSpaces
          val storedSubscription =
            details.incomeSourceType match {
              case IncomeSourceType.business => business(arn)
              case IncomeSourceType.property => property(arn)
              case IncomeSourceType.both => both(arn)
            }
          subscriptionStoreConnector.storeSubscriptionData(nino, storedSubscription).map {
            case Right(StoreSubscriptionSuccess) => Ok("success")
            case Left(reason) => InternalServerError("failed " + reason)
          }.recover {
            case reason => InternalServerError("failed " + reason)
          }
        }
      )
    }
  }

  private def business(arn: String) =
    StoredSubscription(
      arn = arn,
      incomeSource = Business,
      otherIncome = false,
      accountingPeriodStart = Some(DateModel("06", "04", "2017")),
      accountingPeriodEnd = Some(DateModel("05", "04", "2018")),
      tradingName = Some(RandomStringUtil.randomAlpha(10)),
      cashOrAccruals = Some(Cash)
    )

  private def property(arn: String) =
    StoredSubscription(
      arn = arn,
      incomeSource = Property,
      otherIncome = false
    )

  private def both(arn: String) =
    StoredSubscription(
      arn = arn,
      incomeSource = Both,
      otherIncome = false,
      accountingPeriodStart = Some(DateModel("06", "04", "2018")),
      accountingPeriodEnd = Some(DateModel("05", "04", "2019")),
      tradingName = Some(RandomStringUtil.randomAlpha(10)),
      cashOrAccruals = Some(Cash)
    )
}


object RandomStringUtil {

  def randomAlpha(length: Int): String = {
    val chars = ('a' to 'z') ++ ('A' to 'Z')
    randomStringFromCharList(length, chars)
  }

  private def randomStringFromCharList(length: Int, chars: Seq[Char]): String = {
    val sb = new StringBuilder
    for (i <- 1 to length) {
      val randomNum = util.Random.nextInt(chars.length)
      sb.append(chars(randomNum))
    }
    sb.toString
  }

}

// $COVERAGE-ON$
