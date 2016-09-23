package com.ibm.couchdb.examples

import com.ibm.couchdb._
import org.slf4j.LoggerFactory

import scalaz._
import scalaz.concurrent.Task

object CouchDBExample extends App {
  private val logger = LoggerFactory.getLogger(Basic.getClass)

  // Define a simple case class to represent our data model
  case class Employee(name:String, band: String )

  // Define a type mapping used to transform class names into the doc kind
  val typeMapping = TypeMapping(classOf[Employee] -> "Employee")

  // Define some sample data
  val alice = Employee("Amit Karir", "7A")
  val bob   = Employee("Vandana", "7B")
  val carl  = Employee("Priya", "8A")

  // Create a CouchDB client instance
  logger.info("Connectiong to local CouchDB instance...")
  val couch  = CouchDb("127.0.0.1", 5984, https=false, "amit", "amit123$")
  // Define a database name
  val dbName = "employeedb"
  // Get an instance of the DB API by name and type mapping
  logger.info("Getting DB API")
  val db     = couch.db(dbName, typeMapping)


  typeMapping.get(classOf[Employee]).foreach { mType =>
    val actions: Task[Seq[Employee]] = for {
    // Delete the database or ignore the error if it doesn't exist
      _ <- couch.dbs.delete(dbName).ignoreError
      // Create a new database
      _ <- couch.dbs.create(dbName)
      // Insert documents into the database
      _ <- db.docs.createMany(Seq(alice, bob, carl))
      // Retrieve all documents from the database and unserialize to Employee
      docs <- db.docs.getMany.includeDocs[Employee].byTypeUsingTemporaryView(mType).build.query
    } yield docs.getDocsData





    // Execute the actions and process the result
    actions.attemptRun  match {
      // In case of an error (left side of Either), print it
      case -\/(e) => logger.error(e.getMessage, e)
      // In case of a success (right side of Either), print each object
      case \/-(a) => a.foreach(x => logger.info(x.toString))
    }
    couch.client.client.shutdownNow()
  }
}