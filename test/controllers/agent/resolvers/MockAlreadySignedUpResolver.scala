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

package controllers.agent.resolvers

import controllers.BaseMockResolver
import models.Channel
import org.mockito.ArgumentMatchers
import org.mockito.Mockito.when
import org.scalatestplus.mockito.MockitoSugar.mock
import play.api.mvc.Results.Redirect

import scala.concurrent.Future

trait MockAlreadySignedUpResolver extends BaseMockResolver {

  val mockResolver: AlreadySignedUpResolver = mock[AlreadySignedUpResolver]

  def mockResolverNoChannel(): Unit = {
    when(mockResolver.resolve(
      ArgumentMatchers.any(),
      ArgumentMatchers.eq(None)
    )(ArgumentMatchers.any(),
      ArgumentMatchers.any()
    )).thenReturn(
      Future.successful(Redirect(call))
    )
  }

  def mockResolverWithChannel(channel: Channel): Unit = {
    when(mockResolver.resolve(
      ArgumentMatchers.any(),
      ArgumentMatchers.eq(Some(channel))
    )(ArgumentMatchers.any(),
      ArgumentMatchers.any()
    )).thenReturn(
      Future.successful(Redirect(call))
    )
  }
}