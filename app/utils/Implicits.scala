/*
 * Copyright 2017 HM Revenue & Customs
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

package utils

import scala.concurrent.Future


trait Implicits {

  implicit def optionWrapperUtil[T, S <: T](data: S): Option[T] = Some(data)

  implicit def FutureUtl[T, S <: T](fData: S): Future[T] = Future.successful(fData)

  implicit def FutureUtl[T](err: Throwable): Future[T] = Future.failed(err)

  implicit def EitherUtilLeft[T, R <: T, L](left: L): Either[L, R] = Left(left)

  implicit def EitherUtilRight[T, R <: T, L](right: R): Either[L, R] = Right(right)

  implicit class StringNinoUtil(string: String) {
    @inline def stripSpaces: String = string.toUpperCase().replace(" ", "")

    def toNinoDisplayFormat: String = string.stripSpaces.split("(?<=\\G.{2})").reduce(_ + " " + _)
  }
}


object Implicits extends Implicits
