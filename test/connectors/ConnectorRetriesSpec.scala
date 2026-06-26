/*
 * Copyright 2026 HM Revenue & Customs
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

package connectors

import com.typesafe.config.{Config, ConfigFactory, ConfigValueFactory}
import org.apache.pekko.actor.ActorSystem
import org.scalatest.BeforeAndAfterAll
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

import scala.concurrent.duration.*
import scala.concurrent.{Await, ExecutionContext, Future}
import scala.jdk.CollectionConverters.*

class ConnectorRetriesSpec extends AnyWordSpec with Matchers with BeforeAndAfterAll {

  private implicit val actorSystem: ActorSystem = ActorSystem("connector-retries-spec")
  private implicit val ec: ExecutionContext = ExecutionContext.global

  private class TestConnectorRetries(config: Config) extends ConnectorRetries {
    override protected val actorSystem: ActorSystem = ConnectorRetriesSpec.this.actorSystem
    override protected val configuration: Config = config
  }

  private val intervalsConfig: Config =
    ConfigFactory.empty().withValue(
      "retries.intervals",
      ConfigValueFactory.fromIterable(List("1 millisecond", "1 millisecond").asJava)
    )

  private trait Setup {
    val config: Config = intervalsConfig
    val label: String = "test"
    val initialKey: String = "key-1"
    lazy val underTest: TestConnectorRetries = new TestConnectorRetries(config)

    def runRetry[A](nextKey: PartialFunction[(A, String), String])(block: String => Future[A]): A =
      Await.result(
        underTest.retryWithIdempotency[A](label, initialKey)(nextKey)(block),
        2.seconds
      )
  }

  override protected def afterAll(): Unit = {
    Await.result(actorSystem.terminate(), 5.seconds)
    super.afterAll()
  }

  "retryWithIdempotency" should {
    "retry while the partial function matches" in new Setup {
      var attempts = 0

      val result = runRetry[String] {
        case ("retry", currentKey) => currentKey
      } { _ =>
        attempts += 1
        Future.successful(if (attempts < 3) "retry" else "done")
      }

      result shouldBe "done"
      attempts shouldBe 3
    }

    "return immediately when the partial function does not match" in new Setup {
      var attempts = 0

      val result = runRetry[String] {
        case ("retry", currentKey) => currentKey
      } { _ =>
        attempts += 1
        Future.successful("done")
      }

      result shouldBe "done"
      attempts shouldBe 1
    }

    "stop retrying once configured intervals are exhausted" in new Setup {
      var attempts = 0

      val result = runRetry[String] {
        case ("retry", currentKey) => currentKey
      } { _ =>
        attempts += 1
        Future.successful("retry")
      }

      result shouldBe "retry"
      attempts shouldBe 3
    }

    "use global intervals even when the label is different" in new Setup {
      override val label: String = "missing-label"
      var attempts = 0

      val result = runRetry[String] {
        case ("retry", currentKey) => currentKey
      } { _ =>
        attempts += 1
        Future.successful(if (attempts < 3) "retry" else "done")
      }

      result shouldBe "done"
      attempts shouldBe 3
    }

    "reuse the same idempotency key when nextKey returns the current key" in new Setup {
      var attempts = 0
      val keysUsed = scala.collection.mutable.ListBuffer.empty[String]

      val result = runRetry[Either[String, String]] {
        case (Left("temporary"), currentKey) => currentKey
      } { key =>
        attempts += 1
        keysUsed += key
        Future.successful(if (attempts < 3) Left("temporary") else Right("ok"))
      }

      result shouldBe Right("ok")
      keysUsed.toList shouldBe List("key-1", "key-1", "key-1")
    }

    "switch to a new idempotency key when nextKey returns a new one" in new Setup {
      var attempts = 0
      val keysUsed = scala.collection.mutable.ListBuffer.empty[String]

      val result = runRetry[Either[String, String]] {
        case (Left("duplicate"), _) => "key-2"
        case (Left("temporary"), currentKey) => currentKey
      } { key =>
        attempts += 1
        keysUsed += key
        val response = attempts match {
          case 1 => Left("duplicate")
          case 2 => Left("temporary")
          case _ => Right("ok")
        }
        Future.successful(response)
      }

      result shouldBe Right("ok")
      keysUsed.toList shouldBe List("key-1", "key-2", "key-2")
    }

    "not retry when no intervals are configured" in new Setup {
      override val config: Config = ConfigFactory.empty()
      var attempts = 0
      val keysUsed = scala.collection.mutable.ListBuffer.empty[String]

      val result = runRetry[Either[String, String]] {
        case (Left("temporary"), currentKey) => currentKey
      } { key =>
        attempts += 1
        keysUsed += key
        Future.successful(Left("temporary"))
      }

      result shouldBe Left("temporary")
      attempts shouldBe 1
      keysUsed.toList shouldBe List("key-1")
    }
  }
}
