package forms.validation.utils

import util.UnitTestTrait
import org.scalatest.Matchers._

class PatternsSpec extends UnitTestTrait {

  "ISO 8859-1" should {
    "valid for" in {
      val testData = List[String](
        32.toChar.toString,
        126.toChar.toString,
        160.toChar.toString,
        255.toChar.toString
      )
      testData.foreach(data => Patterns.validText(data) shouldBe true)
    }

    "invalid for" in {
      val testData = List[String](
        31.toChar.toString,
        127.toChar.toString,
        159.toChar.toString,
        256.toChar.toString
      )
      testData.foreach(data => Patterns.validText(data) shouldBe false)
    }
  }
}
