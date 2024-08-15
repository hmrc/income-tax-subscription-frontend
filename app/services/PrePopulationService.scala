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

package services

import models.PrePopData
import models.common.business._
import models.common.{OverseasPropertyModel, PropertyModel}
import play.api.Logging
import uk.gov.hmrc.http.HeaderCarrier
import utilities.UUIDProvider

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class PrePopulationService @Inject()(val subscriptionDetailsService: SubscriptionDetailsService, uuidProvider: UUIDProvider) extends Logging {

  def prePopulate(reference: String, prepop: PrePopData)
                 (implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Unit] = for {
    flag <- subscriptionDetailsService.fetchPrePopFlag(reference)
    _ <- flag match {
      case None => populateSubscription(reference, prepop)
      case Some(_) => Future.successful(())
    }
  } yield ()

  private def populateSubscription(reference: String, prePopData: PrePopData)
                                  (implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Unit] = {
    // Set up all futures so that they parallelise.

    val maybeAccountingMethod = prePopData.selfEmployments.flatMap(_.flatMap(_.businessAccountingMethod).headOption)

    val futureSaveSelfEmployments = prePopData.selfEmployments match {
      case None => Future.successful(())
      case Some(listPrepopSelfEmployment) =>
        val listSelfEmploymentData = listPrepopSelfEmployment.map(se => SelfEmploymentData(
          uuidProvider.getUUID,
          se.businessStartDate.map(date => BusinessStartDate(date)),
          se.businessName.flatMap(name => BusinessNameModel(name).toCleanOption),
          BusinessTradeNameModel(se.businessTradeName).toCleanOption,
          if (se.businessAddressPostCode.isDefined || se.businessAddressFirstLine.isDefined)
            Some(BusinessAddressModel(address = Address(se.businessAddressFirstLine.toList, se.businessAddressPostCode)))
          else
            None
        ))
        subscriptionDetailsService.saveBusinesses(reference, listSelfEmploymentData, maybeAccountingMethod)
    }

    val futureSaveUkPropertyInfo = prePopData.ukProperty match {
      case None => Future.successful(())
      case Some(up) => subscriptionDetailsService.saveProperty(reference, PropertyModel(up.ukPropertyAccountingMethod, up.ukPropertyStartDate))
    }

    val futureSaveOverseasPropertyInfo = prePopData.overseasProperty match {
      case None => Future.successful(())
      case Some(op) =>
        subscriptionDetailsService.saveOverseasProperty(reference, OverseasPropertyModel(op.overseasPropertyAccountingMethod, op.overseasPropertyStartDate))
    }

    val futureSavePrePopFlag = subscriptionDetailsService.savePrePopFlag(reference, prepop = true)

    // Wait for futures
    for {
      _ <- futureSaveSelfEmployments
      _ <- futureSaveUkPropertyInfo
      _ <- futureSaveOverseasPropertyInfo
      _ <- futureSavePrePopFlag
    } yield ()
  }
}
