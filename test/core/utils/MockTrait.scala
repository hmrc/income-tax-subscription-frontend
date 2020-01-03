/*
 * Copyright 2020 HM Revenue & Customs
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

package core.utils

import org.mockito.{ArgumentMatcher, ArgumentMatchers}
import org.scalatest.BeforeAndAfterEach
import org.scalatest.mockito.MockitoSugar

import scala.concurrent.Future


trait MockTrait extends UnitTestTrait with MockitoSugar with BeforeAndAfterEach {

  // used to help mock setup functions to clarify if certain results should be mocked.
  sealed trait MockConfiguration[+A] {
    final def get = this match {
      case Configure(config) => config
      case _ => throw new RuntimeException("This element is not to be configured")
    }

    final def ifConfiguredThen(action: A => Unit): Unit = this match {
      case Configure(dataToReturn) => action(dataToReturn)
      case _ =>
    }
  }

  case class Configure[A](config: A) extends MockConfiguration[A]

  case object DoNotConfigure extends MockConfiguration[Nothing]

  implicit def convertToMockConfiguration[T](value: T): MockConfiguration[T] = Configure(value)

  implicit def convertToMockConfiguration2[T](value: T): MockConfiguration[Option[T]] = Configure(value)

  implicit def convertToMockConfiguration3[T](value: T): MockConfiguration[Future[T]] = Configure(value)

  implicit def convertToMockConfiguration4[T](value: T): MockConfiguration[Future[Option[T]]] = Configure(Some(value))

  implicit def convertToMockConfiguration5[T](err: Throwable): MockConfiguration[Future[Option[T]]] = Configure(err)

  implicit class VerificationUtil(someCount: Option[Int]) {
    // util function designed for aiding verify functions
    def ifDefinedThen(action: (Int) => Unit) = someCount match {
      case Some(count) => action(count)
      case _ =>
    }
  }

  type M[T] = MockConfiguration[T]
  type MO[T] = MockConfiguration[Option[T]]
  type MFO[T] = MockConfiguration[Future[Option[T]]]
  type MF[T] = MockConfiguration[Future[T]]

  def matches[T](predicate: T => Boolean): T = ArgumentMatchers.argThat(
    new ArgumentMatcher[T] {
      override def matches(argument: T): Boolean = predicate(argument)
    }
  )
}
