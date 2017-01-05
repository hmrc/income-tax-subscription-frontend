package models

import java.time.LocalDate
import java.time.format.DateTimeFormatter

import org.scalatestplus.play.{OneAppPerTest, PlaySpec}
import org.scalatest.Matchers._


class DateModelSpec extends PlaySpec with OneAppPerTest {

  "the DateModel" should {
    "convert correctly to java.time.LocalDate" in {
      val date = DateModel("01", "02", "2017")
      date.toLocalDate shouldBe LocalDate.parse("01/02/2017", DateTimeFormatter.ofPattern("dd/MM/yyyy"))
      date.toLocalDate.isEqual(date) shouldBe true
    }
  }
}
