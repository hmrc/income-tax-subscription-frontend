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

import auth.agent.AuthenticatedController
import config.AppConfig
import controllers.utils.ReferenceRetrieval
import models.audits.SaveAndComebackAuditing
import models.audits.SaveAndComebackAuditing.SaveAndComeBackAuditModel
import models.common.business.AccountingMethodModel
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Request}
import play.api.{Configuration, Environment}
import services.agent.ClientDetailsRetrieval
import services.{AuditingService, AuthService, SubscriptionDetailsService, UTRService}
import uk.gov.hmrc.http.{HeaderCarrier, InternalServerException}
import uk.gov.hmrc.play.bootstrap.config.AuthRedirects
import utilities.{AccountingPeriodUtil, CacheExpiryDateProvider, CurrentDateProvider}
import views.html.agent.tasklist.ProgressSaved

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class ProgressSavedController @Inject()(progressSavedView: ProgressSaved,
                                        currentDateProvider: CurrentDateProvider,
                                        subscriptionDetailsService: SubscriptionDetailsService,
                                        referenceRetrieval: ReferenceRetrieval,
                                        utrService: UTRService,
                                        clientDetailsRetrieval: ClientDetailsRetrieval,
                                        cacheExpiryDateProvider: CacheExpiryDateProvider)
                                       (val auditingService: AuditingService,
                                        val authService: AuthService,
                                        val appConfig: AppConfig,
                                        val config: Configuration,
                                        val env: Environment)
                                       (implicit val ec: ExecutionContext,
                                        mcc: MessagesControllerComponents) extends AuthenticatedController with AuthRedirects {

  def show(location: Option[String] = None): Action[AnyContent] = Authenticated.async {
    implicit request =>
      implicit user =>
        referenceRetrieval.getAgentReference flatMap { reference =>
          clientDetailsRetrieval.getClientDetails flatMap { clientDetails =>
            subscriptionDetailsService.fetchLastUpdatedTimestamp(reference) flatMap {
              case Some(timestamp) =>
                location.fold(
                  Future.successful(Ok(progressSavedView(cacheExpiryDateProvider.expiryDateOf(timestamp.dateTime), signInUrl, clientDetails = clientDetails)))
                )(location => {
                  for {
                    saveAndComebackAuditData <- retrieveAuditData(reference, user.arn, location)
                    _ <- auditingService.audit(saveAndComebackAuditData)
                  } yield {
                    Ok(progressSavedView(cacheExpiryDateProvider.expiryDateOf(timestamp.dateTime), signInUrl, clientDetails = clientDetails))
                  }
                })
              case None => throw new InternalServerException("[ProgressSavedController][show] - The last updated timestamp cannot be retrieved")
            }
          }
        }
  }

  private def retrieveAuditData(
                                 reference: String,
                                 arn: String,
                                 location: String
                               )(implicit request: Request[AnyContent], hc: HeaderCarrier): Future[SaveAndComeBackAuditModel] = {

    for {
      clientDetails <- clientDetailsRetrieval.getClientDetails
      utr <- utrService.getUTR
      (businesses, accountingMethod) <- subscriptionDetailsService.fetchAllSelfEmployments(reference)
      property <- subscriptionDetailsService.fetchProperty(reference)
      overseasProperty <- subscriptionDetailsService.fetchOverseasProperty(reference)
      selectedTaxYear <- subscriptionDetailsService.fetchSelectedTaxYear(reference)
    } yield {
      SaveAndComeBackAuditModel(
        userType = SaveAndComebackAuditing.agentUserType,
        maybeAgentReferenceNumber = Some(arn),
        utr = utr,
        saveAndRetrieveLocation = location,
        nino = clientDetails.nino,
        currentTaxYear = AccountingPeriodUtil.getTaxEndYear(currentDateProvider.getCurrentDate),
        selectedTaxYear = selectedTaxYear,
        selfEmployments = businesses,
        maybeSelfEmploymentAccountingMethod = accountingMethod.map(AccountingMethodModel.apply),
        maybePropertyModel = property,
        maybeOverseasPropertyModel = overseasProperty
      )
    }
  }

  private val signInUrl: String = ggLoginUrl
}
