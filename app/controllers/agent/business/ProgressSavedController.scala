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

package controllers.agent.business

import auth.agent.AuthenticatedController
import config.AppConfig
import connectors.IncomeTaxSubscriptionConnector
import controllers.utils.ReferenceRetrieval
import models.audits.SaveAndComebackAuditing
import models.audits.SaveAndComebackAuditing.SaveAndComeBackAuditModel
import models.common.business.{AccountingMethodModel, SelfEmploymentData}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Request}
import play.api.{Configuration, Environment}
import services.{AuditingService, AuthService, SubscriptionDetailsService}
import uk.gov.hmrc.http.{HeaderCarrier, InternalServerException}
import uk.gov.hmrc.play.bootstrap.config.AuthRedirects
import utilities.SubscriptionDataKeys.{BusinessAccountingMethod, BusinessesKey}
import utilities.{AccountingPeriodUtil, CacheExpiryDateProvider, CurrentDateProvider}
import views.html.agent.business.ProgressSaved

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class ProgressSavedController @Inject()(val progressSavedView: ProgressSaved,
                                        val auditingService: AuditingService,
                                        val authService: AuthService,
                                        val subscriptionDetailsService: SubscriptionDetailsService,
                                        val incomeTaxSubscriptionConnector: IncomeTaxSubscriptionConnector,
                                        val currentDateProvider: CurrentDateProvider,
                                        val cacheExpiryDateProvider: CacheExpiryDateProvider)
                                      (implicit val ec: ExecutionContext,
                                        val appConfig: AppConfig,
                                        val config: Configuration,
                                        val env: Environment,
                                        mcc: MessagesControllerComponents) extends AuthenticatedController with AuthRedirects with ReferenceRetrieval {
  def show(location: Option[String] = None): Action[AnyContent] = Authenticated.async {
    implicit request =>
      implicit user =>
        withAgentReference { reference =>
          subscriptionDetailsService.fetchLastUpdatedTimestamp(reference) flatMap {
            case Some(timestamp) =>
              location.fold(
                Future.successful(Ok(progressSavedView(cacheExpiryDateProvider.expiryDateOf(timestamp.dateTime), signInUrl)))
              )(location => {
                for {
                  saveAndComebackAuditData <- retrieveAuditData(reference, user.arn, user.clientUtr, user.clientNino, location)
                  _ = auditingService.audit(saveAndComebackAuditData)
                } yield
                  Ok(progressSavedView(cacheExpiryDateProvider.expiryDateOf(timestamp.dateTime), signInUrl))
              })
            case None => throw new InternalServerException("[ProgressSavedController][show] - The last updated timestamp cannot be retrieved")
          }
        }
  }

  private def retrieveAuditData(
                         reference: String,
                         maybeArn: Option[String],
                         maybeUtr: Option[String],
                         maybeNino: Option[String],
                         location: String
                       )(implicit request: Request[AnyContent], hc: HeaderCarrier): Future[SaveAndComeBackAuditModel] = {
    for {
      businesses <- incomeTaxSubscriptionConnector.getSubscriptionDetailsSeq[SelfEmploymentData](reference, BusinessesKey)
      businessAccountingMethod <- incomeTaxSubscriptionConnector.getSubscriptionDetails[AccountingMethodModel](reference, BusinessAccountingMethod)
      property <- subscriptionDetailsService.fetchProperty(reference)
      overseasProperty <- subscriptionDetailsService.fetchOverseasProperty(reference)
      selectedTaxYear <- subscriptionDetailsService.fetchSelectedTaxYear(reference)
    } yield {
      SaveAndComeBackAuditModel(
        userType = SaveAndComebackAuditing.agentUserType,
        maybeAgentReferenceNumber = Some(maybeArn.getOrElse(throw new Exception("[ProgressSavedController][show] - could not retrieve arn from session"))),
        utr = maybeUtr.getOrElse(throw new Exception("[ProgressSavedController][show] - could not retrieve utr from session")),
        saveAndRetrieveLocation = location,
        nino = maybeNino.getOrElse(throw new Exception("[ProgressSavedController][show] - could not retrieve nino from session")),
        currentTaxYear = AccountingPeriodUtil.getTaxEndYear(currentDateProvider.getCurrentDate),
        selectedTaxYear = selectedTaxYear,
        selfEmployments = businesses,
        maybeSelfEmploymentAccountingMethod = businessAccountingMethod,
        maybePropertyModel = property,
        maybeOverseasPropertyModel = overseasProperty
      )
    }
  }

  private val signInUrl: String = ggLoginUrl
}
