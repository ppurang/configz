package net.rosien.configz

import com.typesafe.config.ConfigException.{WrongType, Missing}
import org.specs2.scalaz.Spec
import org.specs2.Specification
import org.specs2.scalaz.ScalazMatchers
import org.specs2.scalaz.ValidationMatchers
import scalaz.scalacheck.ScalazProperties.monoid

class ConfigzSpec extends Specification with Spec with ValidationMatchers with ScalazMatchers {
  import collection.JavaConversions._
  import com.typesafe.config._, Config._
  import org.scalacheck._
  import scalaz._, Scalaz._

  override def is = "Configz should" ^
    "read settings"          ! configz().settings ^
    "accumulate errors"      ! configz().errors ^
   // "accumulate only expected errors"      ! configz().onlyExpectedErrors ^
    "validate via kleisli"   ! configz().validateKleisli ^
    "provide Monoid[Config]" ! prop{ c: Config =>
      implicit val e = Equal.equalA[Config]
      monoid.laws[Config]
    } ^
    end

  implicit val ArbConfig: Arbitrary[Config] = Arbitrary {
    Gen.oneOf(
      ConfigFactory.empty,
      ConfigFactory.load,
      ConfigFactory.parseMap(Map("foo" -> 12, "bar" -> "baz")))
  }

  case class configz()  {

    val config = ConfigFactory.load
    val boolProp = "configz.bool".path[Boolean]
    val intProp = "configz.int".path[Int]

    def errors = {
      val missing = "configz.asdf".path[String]
      val wrongType = "configz.bool".path[Int]

      (missing tuple wrongType).settings(config) must beFailing.like {
        case fails => fails.list must beLike {
          case (e1: ConfigException.Missing) :: (e2: ConfigException.WrongType) :: Nil => ok
        }
      }
    }

    def onlyExpectedErrors = {
      val missing = "configz.asdf".path[String]
      val wrongType = "configz.bool".path[Int]

      (missing tuple wrongType).settings(config) match {
        case Failure(errors) =>
          errors.list match {
            case (e1: Missing) :: (e2: WrongType) :: Nil => ok
            case other => failure(other.toString)
          }
        case other => failure(other.toString)
      }
    }

    def settings = config.get(boolProp tuple intProp) must beSuccessful(true -> 1234)

    def validateKleisli = {
      import SettingBind._
      val validIntProp = validate((_: Int) < 1000, "configz.int must be < 1000")

      (config.get(intProp >==> validIntProp) must beFailing)
    }
  }
}
