package services.equipment.slick

import org.joda.time.DateTime
import javax.inject._
import slick.driver.JdbcProfile
import slick.driver.MySQLDriver.api._
import com.github.tototoshi.slick.MySQLJodaSupport._
import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global
import play.api.db._
import play.api.db.slick._

import services.equipment._

class SlickBackedEquipmentService @Inject()(@NamedDatabase("equipment") dbConfigProvider: DatabaseConfigProvider) extends EquipmentService {
	val dbConfig = dbConfigProvider.get[JdbcProfile]


	class EquipmentTypeTable(tag: Tag) extends Table[(Int, String)](tag, "equipment_type") {
		def id = column[Int]("id", O.PrimaryKey) // This is the primary key column
		def name = column[String]("name")

		def * = (id, name)
	}
	val equipmentType = TableQuery[EquipmentTypeTable]


	class EquipmentTable(tag: Tag) extends Table[(Int, Int, String, Option[String], DateTime, Option[Double], Option[Double], Option[Double], Option[Double], Option[Boolean], Option[Double], Option[Double], Option[Double], Option[Double], Option[Double], Option[Boolean])](tag, "equipment") {
		def id = column[Int]("id", O.PrimaryKey) // This is the primary key column
		def equipmentTypeId = column[Int]("equipment_type_id")
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
        def normalOperatingCellTemperature = column[Option[Double]]("power_temp_coefficient")

        /* inverter columns */
		def rating = column[Option[Double]]("rating")
		def inverterEfficiency = column[Option[Double]]("inverter_efficiency")
		def inverterOutputVoltage = column[Option[Double]]("inverter_output_voltage")
        def inverterIsThreePhase = column[Option[Boolean]]("inverter_is_three_phase")

		def * = (id, equipmentTypeId, model, description, modifiedDate, panelKwStc, panelKwPtc, panelHeightMm, panelWidthMm, panelIsBipvRated, powerTempCoefficient, normalOperatingCellTemperature, rating, inverterEfficiency, inverterOutputVoltage, inverterIsThreePhase)

  		def equipmentTypeFK = foreignKey("equipment_type_id", equipmentTypeId, equipmentType)(_.id)
	}
	val equipment = TableQuery[EquipmentTable]


	def getEquipment(equipmentId: Int): Future[Option[Equipment]] = {
		val equipmentQuery = for { 
			e <- equipment if e.id === equipmentId
			et <- equipmentType if e.equipmentTypeId === et.id
		} yield (
			e.id,
			et.name,
			e.model,
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
  		
  		foundEquipment map { results => results.headOption map { 
  			case (id, "module", model, description, modifiedDate,
  				Some(panelKwStc), Some(panelKwPtc), Some(panelHeightMm), Some(panelWidthMm), panelIsBipvRated, Some(powerTempCoefficient), Some(normalOperatingCellTemperature),
  				None, None, None, isThreePhase) => 
  					Module(id, model, description, modifiedDate, panelKwStc, panelKwPtc, panelHeightMm, panelWidthMm, panelIsBipvRated, powerTempCoefficient, normalOperatingCellTemperature)
  			case (id, "inverter" , model, description, modifiedDate, 
  				None, None, None, None, None, None, None,
  				Some(rating), Some(inverterEfficiency), inverterOutputVoltage, inverterIsThreePhase) => 
  					Inverter(id, model, description, modifiedDate, rating, inverterEfficiency, inverterOutputVoltage, inverterIsThreePhase)
  			case s => throw new Exception(s"Unable to map result: $s") //TODO better error handling
  		} }
	}

}