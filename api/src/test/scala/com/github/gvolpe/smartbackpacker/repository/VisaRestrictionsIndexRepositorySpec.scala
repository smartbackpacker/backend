package com.github.gvolpe.smartbackpacker.repository

import cats.effect.IO
import com.github.gvolpe.smartbackpacker.common.IOAssertion
import com.github.gvolpe.smartbackpacker.model.CountryCode
import doobie.h2.H2Transactor
import doobie.implicits._
import doobie.util.transactor.Transactor
import doobie.util.update.Update0
import org.scalatest.{BeforeAndAfterAll, FlatSpecLike, Matchers}

class VisaRestrictionsIndexRepositorySpec extends VisaRestrictionsIndexSQLSetup with FlatSpecLike with Matchers with BeforeAndAfterAll {

  override val h2Transactor: IO[H2Transactor[IO]] =
    H2Transactor.newH2Transactor[IO]("jdbc:h2:mem:sb;MODE=PostgreSQL;DB_CLOSE_DELAY=-1", "sa", "")

  override def beforeAll(): Unit = {
    super.beforeAll()
    setup.unsafeRunSync()
  }

  it should "NOT find the visa restriction index" in IOAssertion {
    for {
      xa  <- h2Transactor
      idx <- new PostgresVisaRestrictionsIndexRepository[IO](xa).findRestrictionsIndex(new CountryCode("AR"))
    } yield {
      idx should be (None)
    }
  }

}

trait VisaRestrictionsIndexSQLSetup {

  def h2Transactor: IO[H2Transactor[IO]]

  lazy val setup: IO[Unit] =
    for {
      xa  <- h2Transactor
      _   <- createVisaIndexTable(xa)
    } yield ()

  private def createVisaIndexTableStatement: Update0 =
    sql"""
         CREATE TABLE visa_restrictions_index (
           country_code VARCHAR (2) PRIMARY KEY,
           rank SMALLINT NOT NULL,
           acc SMALLINT NOT NULL,
           sharing SMALLINT DEFAULT 0
         )
       """.update

  def createVisaIndexTable(xa: Transactor[IO]): IO[Int] =
    createVisaIndexTableStatement.run.transact(xa)

}
