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

package core.connectors.mocks

import core.utils.MockTrait
import org.mockito.ArgumentMatchers
import uk.gov.hmrc.auth.core.AuthConnector
import org.mockito.Mockito._
import uk.gov.hmrc.auth.core.authorise.Predicate
import uk.gov.hmrc.auth.core.retrieve.Retrieval
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.{ExecutionContext, Future}

trait MockAuth extends MockTrait {
  val mockAuth = mock[AuthConnector]

  def mockAuthorise[T](predicate: Predicate, retrieval: Retrieval[T])(result: T): Unit =
    when(mockAuth.authorise[T](
      ArgumentMatchers.eq(predicate),
      ArgumentMatchers.any[Retrieval[T]]
    )(ArgumentMatchers.any[HeaderCarrier], ArgumentMatchers.any[ExecutionContext]))
      .thenReturn(Future.successful(result))
}
