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

import auth.individual.{IncomeTaxSAUser, SignUpController}
import config.AppConfig
import controllers.utils.ReferenceRetrieval
import models.audits.SaveAndComebackAuditing
import models.audits.SaveAndComebackAuditing.SaveAndComeBackAuditModel
import models.common.business.AccountingMethodModel
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Request}
import play.api.{Configuration, Environment}
import services.{AuditingService, AuthService, NinoService, SubscriptionDetailsService}
import uk.gov.hmrc.http.{HeaderCarrier, InternalServerException}
import uk.gov.hmrc.play.bootstrap.config.AuthRedirects
import utilities.{AccountingPeriodUtil, CacheExpiryDateProvider, CurrentDateProvider}
import views.html.individual.tasklist.ProgressSaved

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class ProgressSavedController @Inject()(progressSavedView: ProgressSaved,
                                        currentDateProvider: CurrentDateProvider,
                                        cacheExpiryDateProvider: CacheExpiryDateProvider,
                                        ninoService: NinoService,
                                        subscriptionDetailsService: SubscriptionDetailsService,
                                        referenceRetrieval: ReferenceRetrieval)
                                       (val auditingService: AuditingService,
                                        val authService: AuthService,
                                        val appConfig: AppConfig)
                                       (implicit val ec: ExecutionContext,
                                        val config: Configuration,
                                        val env: Environment,
                                        mcc: MessagesControllerComponents) extends SignUpController with AuthRedirects {

  def show(location: Option[String] = None): Action[AnyContent] = Authenticated.async { implicit request =>
    implicit user =>
      referenceRetrieval.getIndividualReference flatMap { reference =>
        subscriptionDetailsService.fetchLastUpdatedTimestamp(reference) flatMap {
          case Some(timestamp) =>
            location.fold(
              Future.successful(Ok(progressSavedView(cacheExpiryDateProvider.expiryDateOf(timestamp.dateTime), signInUrl)))
            )(location => {
              for {
                saveAndComebackAuditData <- retrieveAuditData(reference, location)
                _ <- auditingService.audit(saveAndComebackAuditData)
              } yield {
                Ok(progressSavedView(cacheExpiryDateProvider.expiryDateOf(timestamp.dateTime), signInUrl))
              }
            })
          case None => throw new InternalServerException("[ProgressSavedController][show] - The last updated timestamp cannot be retrieved")
        }
      }
  }

  private def retrieveAuditData(
                                 reference: String,
                                 location: String
                               )(implicit request: Request[AnyContent], user: IncomeTaxSAUser, hc: HeaderCarrier): Future[SaveAndComeBackAuditModel] = {

    for {
      nino <- ninoService.getNino
      (businesses, accountingMethod) <- subscriptionDetailsService.fetchAllSelfEmployments(reference)
      property <- subscriptionDetailsService.fetchProperty(reference)
      overseasProperty <- subscriptionDetailsService.fetchOverseasProperty(reference)
      selectedTaxYear <- subscriptionDetailsService.fetchSelectedTaxYear(reference, user.getUtr)
    } yield {
      SaveAndComeBackAuditModel(
        userType = SaveAndComebackAuditing.individualUserType,
        utr = user.getUtr,
        nino = nino,
        saveAndRetrieveLocation = location,
        currentTaxYear = AccountingPeriodUtil.getTaxEndYear(currentDateProvider.getCurrentDate),
        selectedTaxYear = selectedTaxYear,
        selfEmployments = businesses,
        maybeSelfEmploymentAccountingMethod = accountingMethod.map(AccountingMethodModel.apply),
        maybePropertyModel = property,
        maybeOverseasPropertyModel = overseasProperty
      )
    }
  }

  val signInUrl: String = ggLoginUrl
}
