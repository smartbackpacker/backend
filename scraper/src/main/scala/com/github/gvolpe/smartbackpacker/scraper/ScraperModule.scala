package com.github.gvolpe.smartbackpacker.scraper

import cats.effect.Async
import com.github.gvolpe.smartbackpacker.common.instances.log._
import com.github.gvolpe.smartbackpacker.scraper.config.ScraperConfiguration
import com.github.gvolpe.smartbackpacker.scraper.parser.{HealthInfoParser, VisaRequirementsParser, VisaRestrictionsIndexParser}
import com.github.gvolpe.smartbackpacker.scraper.sql.{CountryInsertData, HealthInfoInsertData, VisaCategoryInsertData, VisaRequirementsInsertData, VisaRestrictionsIndexInsertData}
import doobie.util.transactor.Transactor

class ScraperModule[F[_] : Async] {

  lazy val scraperConfig     = new ScraperConfiguration[F]

  lazy val devDbUrl: String  = sys.env.getOrElse("JDBC_DATABASE_URL", "")
  lazy val dbUrl: String     = sys.env.getOrElse("SB_DB_URL", "jdbc:postgresql:sb")

  private lazy val dbDriver  = sys.env.getOrElse("SB_DB_DRIVER", "org.postgresql.Driver")
  private lazy val dbUser    = sys.env.getOrElse("SB_DB_USER", "postgres")
  private lazy val dbPass    = sys.env.getOrElse("SB_DB_PASSWORD", "")

  private val xa = {
    if (devDbUrl.nonEmpty) Transactor.fromDriverManager[F](dbDriver, devDbUrl)
    else Transactor.fromDriverManager[F](dbDriver, dbUrl, dbUser, dbPass)
  }

  private val visaRequirementsParser  = new VisaRequirementsParser[F](scraperConfig)
  private val healthInfoParser        = new HealthInfoParser[F](scraperConfig)

  val visaRequirementsInsertData      = new VisaRequirementsInsertData[F](xa, visaRequirementsParser)

  val visaRestrictionsIndexParser     = new VisaRestrictionsIndexParser[F](scraperConfig)
  val visaRestrictionsInsertData      = new VisaRestrictionsIndexInsertData[F](xa)

  val countryInsertData               = new CountryInsertData[F](scraperConfig, xa)
  val visaCategoryInsertData          = new VisaCategoryInsertData[F](xa)

  val healthInfoInsertData            = new HealthInfoInsertData[F](xa, healthInfoParser)

}
