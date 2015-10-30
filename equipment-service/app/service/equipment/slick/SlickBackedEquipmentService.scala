package service.equipment.slick

import org.joda.time.DateTime
import javax.inject._
import slick.driver.JdbcProfile
import slick.driver.MySQLDriver.api._
import com.github.tototoshi.slick.MySQLJodaSupport._
import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global
import play.api.db._
import play.api.db.slick._
import play.api.Logger

import service.equipment._

/**
  * Slick implementation of the Equipment Service that reads equipment from a MySQL db.
  *
  * @constructor create a new controller with an injected db config provider
  * @param dbConfigProvider the config for the equipment database 
  */
class SlickBackedEquipmentService @Inject()(@NamedDatabase("equipment") dbConfigProvider: DatabaseConfigProvider) extends EquipmentService {
  
  protected[slick] type EquipmentResult = (Int, String, String, String, Option[String], DateTime, 
    Option[Double], Option[Double], Option[Double], Option[Double], Option[Boolean], Option[Double], 
    Option[Double], Option[Double], Option[Double], Option[Double], Option[Boolean])

  val dbConfig = dbConfigProvider.get[JdbcProfile]


  class ManufacturerTable(tag: Tag) extends Table[(Int, String)](tag, "manufacturer") {
    def id = column[Int]("id", O.PrimaryKey) // This is the primary key column
    def name = column[String]("name")

    def * = (id, name)
  }
  val manufacturer = TableQuery[ManufacturerTable]

  class EquipmentTypeTable(tag: Tag) extends Table[(Int, String)](tag, "equipment_type") {
    def id = column[Int]("id", O.PrimaryKey) // This is the primary key column
    def name = column[String]("name")

    def * = (id, name)
  }
  val equipmentType = TableQuery[EquipmentTypeTable]


  class EquipmentTable(tag: Tag) extends Table[(Int, Int, Int, String, Option[String], DateTime, Option[Double], Option[Double], Option[Double], Option[Double],
    Option[Boolean], Option[Double], Option[Double], Option[Double], Option[Double], Option[Double], Option[Boolean])](tag, "equipment") {

    def id = column[Int]("id", O.PrimaryKey) // This is the primary key column
    def equipmentTypeId = column[Int]("equipment_type_id")
    def manufacturerId = column[Int]("manufacturer_id")
    def model = column[String]("model")
    def description = column[Option[String]]("description")
    def modifiedDate = column[DateTime]("modified_date")

    /* module columns */
    def panelKwStc = column[Option[Double]]("panel_kw_stc")
    def panelKwPtc = column[Option[Double]]("panel_kw_ptc")
    def panelHeightMm = column[Option[Double]]("panel_height_mm")
    def panelWidthMm = column[Option[Double]]("panel_width_mm")
    def panelIsBipvRated = column[Option[Boolean]]("panel_is_bipv_rated")
    def powerTempCoefficient = column[Option[Double]]("power_temp_coefficient")
    def normalOperatingCellTemperature = column[Option[Double]]("normal_operating_cell_temperature")

    /* inverter columns */
    def rating = column[Option[Double]]("rating")
    def inverterEfficiency = column[Option[Double]]("inverter_efficiency")
    def inverterOutputVoltage = column[Option[Double]]("inverter_output_voltage")
    def inverterIsThreePhase = column[Option[Boolean]]("inverter_is_three_phase")

    def * = (id, equipmentTypeId, manufacturerId, model, description, modifiedDate, panelKwStc, panelKwPtc,
      panelHeightMm, panelWidthMm, panelIsBipvRated, powerTempCoefficient, normalOperatingCellTemperature, rating,
      inverterEfficiency, inverterOutputVoltage, inverterIsThreePhase)
  }
  val equipment = TableQuery[EquipmentTable]


  def getEquipment(equipmentId: EquipmentIdentity): Future[Option[Equipment]] = {
    Logger.info(s"Attempting to obtain equipment for $equipmentId")

    val equipmentQuery = for {
      e <- equipment if e.id === equipmentId.value
      et <- equipmentType if e.equipmentTypeId === et.id
      m <- manufacturer if e.manufacturerId === m.id
    } yield (
      e.id,
      et.name,
      e.model,
      m.name,
      e.description,
      e.modifiedDate,
      e.panelKwStc,
      e.panelKwPtc,
      e.panelHeightMm,
      e.panelWidthMm,
      e.panelIsBipvRated,
      e.powerTempCoefficient,
      e.normalOperatingCellTemperature,
      e.rating,
      e.inverterEfficiency,
      e.inverterOutputVoltage,
      e.inverterIsThreePhase)

    val foundEquipment = dbConfig.db.run(equipmentQuery.result)

    foundEquipment map { results => results.headOption map toEquipment } 
  }

  protected[slick] def toEquipment(result: EquipmentResult): Equipment = result match {
    
    case (
          id,
          "module",
          modelName, manufacturerName, description, modifiedDate,
          Some(panelKwStc), Some(panelKwPtc), Some(panelHeightMm), Some(panelWidthMm),
          panelIsBipvRated, Some(powerTempCoefficient), Some(normalOperatingCellTemperature),
          None, None, None, _) =>
            Module(
              new EquipmentIdentity(id), modelName, manufacturerName, description, modifiedDate, panelKwStc, panelKwPtc,
              panelHeightMm, panelWidthMm, panelIsBipvRated, powerTempCoefficient, normalOperatingCellTemperature)
    
    case (
          id,
          "inverter" , modelName, manufacturerName, description, modifiedDate,
          None, None, None, None, 
          _, _, _, /* _ fields are sometimes stuffed with marker values of false, 0,0 instead of null */
          rating, Some(inverterEfficiency), inverterOutputVoltage, inverterIsThreePhase) =>
            Inverter(new EquipmentIdentity(id), modelName, manufacturerName, description, modifiedDate, rating, inverterEfficiency, inverterOutputVoltage, inverterIsThreePhase)
    
    case s => throw new IllegalStateException(s"Unable to map result: $s")
  }

}
