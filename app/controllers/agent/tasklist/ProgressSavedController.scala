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

package controllers.agent.tasklist

import controllers.SignUpBaseController
import controllers.agent.actions.{ConfirmedClientJourneyRefiner, IdentifierAction}
import models.audits.SaveAndComebackAuditing
import models.audits.SaveAndComebackAuditing.SaveAndComeBackAuditModel
import models.requests.agent.ConfirmedClientRequest
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import play.api.{Configuration, Environment}
import services.{AuditingService, SubscriptionDetailsService}
import uk.gov.hmrc.http.{HeaderCarrier, InternalServerException}
import uk.gov.hmrc.play.bootstrap.config.AuthRedirects
import utilities.{AccountingPeriodUtil, CacheExpiryDateProvider, CurrentDateProvider}
import views.html.agent.tasklist.ProgressSaved

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class ProgressSavedController @Inject()(identify: IdentifierAction,
                                        journeyRefiner: ConfirmedClientJourneyRefiner,
                                        auditingService: AuditingService,
                                        cacheExpiryDateProvider: CacheExpiryDateProvider,
                                        currentDateProvider: CurrentDateProvider,
                                        subscriptionDetailsService: SubscriptionDetailsService,
                                        view: ProgressSaved)
                                       (val config: Configuration, val env: Environment)
                                       (implicit mcc: MessagesControllerComponents, ec: ExecutionContext) extends SignUpBaseController with AuthRedirects {

  def show(location: Option[String] = None): Action[AnyContent] = (identify andThen journeyRefiner).async { implicit request =>
    subscriptionDetailsService.fetchLastUpdatedTimestamp(request.reference) flatMap {
      case Some(timestamp) =>
        location match {
          case Some(id) => for {
            saveAndComebackAuditData <- retrieveAuditData(id)
            _ <- auditingService.audit(saveAndComebackAuditData)
          } yield {
            Ok(view(
              expirationDate = cacheExpiryDateProvider.expiryDateOf(timestamp.dateTime),
              signInUrl = ggLoginUrl,
              clientDetails = request.clientDetails
            ))
          }
          case None =>
            Future.successful(Ok(view(
              expirationDate = cacheExpiryDateProvider.expiryDateOf(timestamp.dateTime),
              signInUrl = ggLoginUrl,
              clientDetails = request.clientDetails
            )))
        }
      case None =>
        throw new InternalServerException("[ProgressSavedController][show] - The last updated timestamp cannot be retrieved")
    }
  }

  private def retrieveAuditData(location: String)
                               (implicit request: ConfirmedClientRequest[AnyContent],
                                hc: HeaderCarrier): Future[SaveAndComeBackAuditModel] = {
    for {
      businesses <- subscriptionDetailsService.fetchAllSelfEmployments(request.reference)
      property <- subscriptionDetailsService.fetchProperty(request.reference)
      overseasProperty <- subscriptionDetailsService.fetchOverseasProperty(request.reference)
      selectedTaxYear <- subscriptionDetailsService.fetchSelectedTaxYear(request.reference)
    } yield {
      SaveAndComeBackAuditModel(
        userType = SaveAndComebackAuditing.agentUserType,
        maybeAgentReferenceNumber = Some(request.arn),
        utr = request.utr,
        saveAndRetrieveLocation = location,
        nino = request.clientDetails.nino,
        currentTaxYear = AccountingPeriodUtil.getTaxEndYear(currentDateProvider.getCurrentDate),
        selectedTaxYear = selectedTaxYear,
        selfEmployments = businesses,
        maybePropertyModel = property,
        maybeOverseasPropertyModel = overseasProperty
      )
    }
  }
}