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

package controllers.individual.tasklist

import config.AppConfig
import controllers.SignUpBaseController
import controllers.individual.actions.{IdentifierAction, SignUpJourneyRefiner}
import models.audits.SaveAndComebackAuditing
import models.audits.SaveAndComebackAuditing.SaveAndComeBackAuditModel
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import play.api.{Configuration, Environment}
import services.*
import uk.gov.hmrc.http.{HeaderCarrier, InternalServerException}
import utilities.{AccountingPeriodUtil, CacheExpiryDateProvider, CurrentDateProvider}
import views.html.individual.tasklist.ProgressSaved

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class ProgressSavedController @Inject()(progressSavedView: ProgressSaved,
                                        currentDateProvider: CurrentDateProvider,
                                        cacheExpiryDateProvider: CacheExpiryDateProvider,
                                        subscriptionDetailsService: SubscriptionDetailsService)
                                       (auditingService: AuditingService,
                                        identify: IdentifierAction,
                                        refine: SignUpJourneyRefiner,
                                        appConfig: AppConfig)
                                       (implicit ec: ExecutionContext,
                                        mcc: MessagesControllerComponents) extends SignUpBaseController {

  def show(location: Option[String] = None): Action[AnyContent] = (identify andThen refine).async { implicit request =>
    subscriptionDetailsService.fetchLastUpdatedTimestamp(request.reference) flatMap {
      case Some(timestamp) =>
        location.fold(
          Future.successful(Ok(progressSavedView(cacheExpiryDateProvider.expiryDateOf(timestamp.dateTime), signInUrl)))
        )(location => {
          for {
            saveAndComebackAuditData <- retrieveAuditData(request.request.utr, request.reference, request.nino, location)
            _ <- auditingService.audit(saveAndComebackAuditData)
          } yield {
            Ok(progressSavedView(cacheExpiryDateProvider.expiryDateOf(timestamp.dateTime), signInUrl))
          }
        })
      case None => throw new InternalServerException("[ProgressSavedController][show] - The last updated timestamp cannot be retrieved")
    }
  }

  private def retrieveAuditData(utr: Option[String], reference: String, nino: String, location: String)(implicit hc: HeaderCarrier): Future[SaveAndComeBackAuditModel] = {

    for {
      businesses <- subscriptionDetailsService.fetchAllSelfEmployments(reference)
      property <- subscriptionDetailsService.fetchProperty(reference)
      overseasProperty <- subscriptionDetailsService.fetchOverseasProperty(reference)
      selectedTaxYear <- subscriptionDetailsService.fetchSelectedTaxYear(reference)
    } yield {
      SaveAndComeBackAuditModel(
        userType = SaveAndComebackAuditing.individualUserType,
        utr = utr.getOrElse(""),
        nino = nino,
        saveAndRetrieveLocation = location,
        currentTaxYear = AccountingPeriodUtil.getTaxEndYear(currentDateProvider.getCurrentDate),
        selectedTaxYear = selectedTaxYear,
        selfEmployments = businesses,
        maybePropertyModel = property,
        maybeOverseasPropertyModel = overseasProperty
      )
    }
  }

  val signInUrl: String = appConfig.ggLoginUrl
}
