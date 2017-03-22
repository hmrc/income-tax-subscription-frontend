package views

import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import play.twirl.api.Html
import utils.UnitTestTrait


trait ViewSpecTrait extends UnitTestTrait {

  trait ElementTest {
    val name: String
    val element: Element

    def mustHaveTheFollowingParagaphs(paragraphs: String*) =
      for (p <- paragraphs) {
        s"$name must have the paragraph (P) '$p'" in {
          element.getElementsByTag("p").text() must include(p)
        }
      }

//    def select(name:String, cssQuery:String):Option[ElementTest] = {
//      val n = name
//      val eles = element.select(cssQuery)
//      eles.size must  1
//
//      ele match {
//        case null => None
//        case _ =>new ElementTest {
//          val name =n
//          val element: Element = ele
//        }
//      }
//
//    }
  }

  class TestView(override val name: String, val page: Html) extends ElementTest {
    lazy val document = Jsoup.parse(page.body)
    override lazy val element = document

    def mustHaveTheTitle(title: String) =
      s"$name must have the title '$title'" in {
        document.title() mustBe title
      }

    def mustHaveTheHeading(heading: String) =
      s"$name must have the heading (H1) '$heading'" in {
        val h1 = document.getElementsByTag("H1")
        h1.size() mustBe 1
        h1.text() mustBe heading
      }
  }

  def testPage(name: String, page: Html): TestView = new TestView(name, page)



}
