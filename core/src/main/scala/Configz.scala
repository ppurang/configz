package net.rosien.configz

import com.typesafe.config._
import scalaz._
import syntax.validation._
import syntax.applicative._

/** Reads settings from a [[com.typesafe.config.Config]]. */
abstract sealed class Configz[A] {
  /** Read the settings from a config. */
  def settings(config: Config): Settings[A]
}

object Configz {

  def apply[A](f: Config => Settings[A]): Configz[A] = new Configz[A]{
    def settings(config: Config) = f(config)
  }

  implicit def configzToKleisli[A](configz: Configz[A]): Kleisli[Settings, Config, A] =
    Kleisli.kleisli(configz.settings)

  implicit val ConfigzInstance: Applicative[Configz] = new Applicative[Configz] {
    override def point[A](a: => A) = new Configz[A] {
      def settings(config: Config) =
        try a.success catch {
          case e: ConfigException => e.failNel
        }
    }

    override def ap[A, B](a: => Configz[A])(f: => Configz[A => B]) = new Configz[B] {
      def settings(config: Config) =
        try a.settings(config) <*> f.settings(config) catch {
          case e: ConfigException => e.failNel
        }
    }

    override def map[A, B](r: Configz[A])(f: A => B) = Configz{
      config => r.settings(config).map(f)
    }
  }

  class ValidatedConfigz[A](configz: Configz[A], reference: Config, paths: String*) extends Configz[A] {
    def settings(config: Config) =
      try {
        config.checkValid(reference, paths: _*)
        configz.settings(config)
      } catch {
        case e: ConfigException => e.failNel
      }
  }

  class ResolvedConfigz[A](configz: Configz[A]) extends Configz[A] {
    def settings(config: Config) =
      try config.resolve.get(configz) catch {
        case e: ConfigException => e.failNel
      }
  }

  /** Get a value at a path from a [[com.typesafe.config.Config]]. */
  def atPath[A](f: Config => String => A): Configz[String => A] = new Configz[String => A] {
    def settings(config: Config): Settings[String => A] = f(config).point[Configz].settings(config)

    override def toString = "Configz(atPath)[%s]".format(f)
  }

  import collection.JavaConversions._

  implicit val BooleanAtPath:     Configz[String => Boolean]       = atPath(config => path => config.getBoolean(path))
  implicit val BooleanListAtPath: Configz[String => List[Boolean]] = atPath(config => path => config.getBooleanList(path).toList.map(Boolean.unbox))
  implicit val ConfigAtPath:      Configz[String => Config]        = atPath(config => path => config.getConfig(path))
  implicit val ConfigListAtPath:  Configz[String => List[Config]]  = atPath(config => path => config.getConfigList(path).toList)
  implicit val DoubleAtPath:      Configz[String => Double]        = atPath(config => path => config.getDouble(path))
  implicit val DoubleListAtPath:  Configz[String => List[Double]]  = atPath(config => path => config.getDoubleList(path).toList.map(Double.unbox))
  implicit val IntAtPath:         Configz[String => Int]           = atPath(config => path => config.getInt(path))
  implicit val IntListAtPath:     Configz[String => List[Int]]     = atPath(config => path => config.getIntList(path).toList.map(Int.unbox))
  implicit val LongAtPath:        Configz[String => Long]          = atPath(config => path => config.getLong(path))
  implicit val LongListAtPath:    Configz[String => List[Long]]    = atPath(config => path => config.getLongList(path).toList.map(Long.unbox))
  implicit val NumberAtPath:      Configz[String => Number]        = atPath(config => path => config.getNumber(path))
  implicit val NumberListAtPath:  Configz[String => List[Number]]  = atPath(config => path => config.getNumberList(path).toList)
  implicit val StringAtPath:      Configz[String => String]        = atPath(config => path => config.getString(path))
  implicit val StringListAtPath:  Configz[String => List[String]]  = atPath(config => path => config.getStringList(path).toList)
}
