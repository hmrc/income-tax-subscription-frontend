/*
 * Copyright 2019 HM Revenue & Customs
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

package incometax.unauthorisedagent.services

import javax.inject.{Inject, Singleton}

import core.config.AppConfig
import core.models.{No, Yes}
import core.services.KeystoreService
import incometax.business.models.{AccountingMethodModel, AccountingPeriodModel, BusinessNameModel}
import incometax.unauthorisedagent.connectors.SubscriptionStoreConnector
import incometax.unauthorisedagent.models.{DeleteSubscriptionFailure, DeleteSubscriptionSuccess, StoredSubscription}
import uk.gov.hmrc.http.{HeaderCarrier, InternalServerException}

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class SubscriptionStoreRetrievalService @Inject()(subscriptionStoreConnector: SubscriptionStoreConnector,
                                                  keystoreService: KeystoreService,
                                                  appConfig: AppConfig)(implicit ec: ExecutionContext) {
  def retrieveSubscriptionData(nino: String)(implicit hc: HeaderCarrier): Future[Option[StoredSubscription]] = {
    if(appConfig.unauthorisedAgentEnabled) {
      subscriptionStoreConnector.retrieveSubscriptionData(nino) flatMap {
        case Right(Some(storedSubscription)) => storeSubscriptionData(storedSubscription)
        case Right(None) => Future.successful(None)
        case Left(error) => Future.failed(new InternalServerException(s"Call to subscription store failed: $error"))
      }
    }
    else Future.successful(None)
  }

  private def storeSubscriptionData(storedSubscription: StoredSubscription)(implicit hc: HeaderCarrier): Future[Option[StoredSubscription]] = {
    val incomeSource = storedSubscription.incomeSource

    val otherIncome = storedSubscription.otherIncome match {
      case true => Yes
      case false => No
    }

    val accountingPeriod = for {
      accountingPeriodStart <- storedSubscription.accountingPeriodStart
      accountingPeriodEnd <- storedSubscription.accountingPeriodEnd
    } yield AccountingPeriodModel(accountingPeriodStart, accountingPeriodEnd)

    val businessName = storedSubscription.tradingName map BusinessNameModel.apply

    val accountingMethod = storedSubscription.cashOrAccruals map AccountingMethodModel.apply

    for {
      _ <- keystoreService.saveIncomeSource(incomeSource)
      _ <- keystoreService.saveOtherIncome(otherIncome)
      _ <- accountingPeriod.fold(Future.successful(Unit))(keystoreService.saveAccountingPeriodDate(_) map (_ => Unit))
      _ <- businessName.fold(Future.successful(Unit))(keystoreService.saveBusinessName(_) map (_ => Unit))
      _ <- accountingMethod.fold(Future.successful(Unit))(keystoreService.saveAccountingMethod(_) map (_ => Unit))
    } yield Some(storedSubscription)
  }

  def deleteSubscriptionData(nino: String)(implicit hc: HeaderCarrier): Future[DeleteSubscriptionSuccess.type] = {
    subscriptionStoreConnector.deleteSubscriptionData(nino) map {
      case Right(success) =>
        success
      case Left(DeleteSubscriptionFailure(reason)) =>
        throw new InternalServerException(s"Delete subscription failed: $reason")
    }
  }
}
