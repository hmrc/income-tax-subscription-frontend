/*
 * Copyright 2022 HM Revenue & Customs
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

package services

import config.featureswitch.FeatureSwitching
import models.PrePopData
import models.common.business._
import models.common.{OverseasPropertyModel, PropertyModel}
import play.api.Logging
import uk.gov.hmrc.http.HeaderCarrier

import java.util.UUID
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class PrePopulationService @Inject()(
                                      val subscriptionDetailsService: SubscriptionDetailsService
                                    ) extends FeatureSwitching with Logging {

  def prePopulate(reference: String, prepop: PrePopData)
                 (implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Unit] = for {
    flag <- subscriptionDetailsService.fetchPrePopFlag(reference)
    _ <- flag match {
      case None => populateSubscription(reference, prepop)
      case Some(_) => Future.successful(Unit)
    }
  } yield Unit

  private def populateSubscription(reference: String, prePopData: PrePopData)
                                  (implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Unit] = {
    // Set up all futures so that they parallelise.
    val futureSaveSelfEmployments = prePopData.selfEmployments match {
      case None => Future.successful(Unit)
      case Some(listPrepopSelfEmployment) =>
        val listSelfEmploymentData = listPrepopSelfEmployment.map(se => SelfEmploymentData(
          UUID.randomUUID().toString,
          se.businessStartDate.map(date => BusinessStartDate(date)),
          se.businessName.map(name => BusinessNameModel(name)),
          Some(BusinessTradeNameModel(se.businessTradeName)),
          if (se.businessAddressPostCode.isDefined || se.businessAddressFirstLine.isDefined)
            Some(BusinessAddressModel(UUID.randomUUID().toString, address = Address(se.businessAddressFirstLine.toList.seq, se.businessAddressPostCode)))
          else
            None
        ))
        subscriptionDetailsService.saveBusinesses(reference, listSelfEmploymentData)
    }

    val futureSaveUkPropertyInfo = prePopData.ukProperty match {
      case None => Future.successful(Unit)
      case Some(up) => subscriptionDetailsService.saveProperty(reference, PropertyModel(up.ukPropertyAccountingMethod, up.ukPropertyStartDate))
    }

    val futureSaveOverseasPropertyInfo = prePopData.overseasProperty match {
      case None => Future.successful(Unit)
      case Some(op) =>
        subscriptionDetailsService.saveOverseasProperty(reference, OverseasPropertyModel(op.overseasPropertyAccountingMethod, op.overseasPropertyStartDate))
    }

    val maybeAccountingMethod = prePopData.selfEmployments.flatMap(_.flatMap(_.businessAccountingMethod).headOption)
    val futureSaveSelfEmploymentsAccountingMethod = maybeAccountingMethod match {
      case None => Future.successful(Unit)
      case Some(accountingMethod) => subscriptionDetailsService.saveSelfEmploymentsAccountingMethod(reference, AccountingMethodModel(accountingMethod))
    }

    val futureSavePrePopFlag = subscriptionDetailsService.savePrePopFlag(reference, prepop = true)

    // Wait for futures
    for {
      _ <- futureSaveSelfEmployments
      _ <- futureSaveUkPropertyInfo
      _ <- futureSaveOverseasPropertyInfo
      _ <- futureSaveSelfEmploymentsAccountingMethod
      _ <- futureSavePrePopFlag
    } yield Unit
  }

}
