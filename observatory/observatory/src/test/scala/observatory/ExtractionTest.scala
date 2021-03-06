package observatory

import org.junit.Assert._
import org.junit.Test
import java.time.LocalDate

import observatory.Extraction.{statStruct, tempStruct}
import org.apache.log4j.{Level, Logger}
import org.apache.spark.sql.SparkSession
import org.

trait ExtractionTest extends FunSuite  {
  private val milestoneTest = namedMilestoneTest("data extraction", 1) _

  // Implement tests for the methods of the `Extraction` object
  Logger.getLogger("org.apache.spark").setLevel(Level.WARN)

  test("Extraction test") {
    val spark: SparkSession = SparkSession.builder().appName("Observatory")
      .config("spark.executor.memory", "1G")
      .config("spark.master", "local").getOrCreate()

    val stationsResourcePath = getClass.getResource("/stations.csv").getPath.replace("%20", " ")
    val temperaturesResourcePath = getClass.getResource("/2015.csv").getPath.replace("%20", " ")

    import spark.implicits._

    val stations = spark.read
      .schema(stationsSchema)
      .option("header", value = false)
      .csv(stationsResourcePath)

    val temperatures = spark.read
      .schema(temperatureSchema)
      .option("header", value = false)
      .csv(temperaturesResourcePath)

    val filteredStations = stations.filter("latitude IS NOT NULL and longitude IS NOT NULL")
    //val joined = stations.join(temperatures, Seq("stnID", "wbanID"))

    val joined = stations.join(temperatures, stations("stnID") <=> temperatures("stnID") &&  stations("wbanID") <=> temperatures("wbanID"))

    //converting to rdd to avoid having to write custom encoder for LocalDate
    val formatted = joined.rdd.map(row => {
      val temperature = (row.getAs[Double]("temperature") - 32) * 5 / 9
      val location: Location = Location(row.getAs[Double]("latitude"), row.getAs[Double]("longitude"))
      val localDate: LocalDate = LocalDate.of(2017, row.getAs[Int]("month"), row.getAs[Int]("day"))
      (localDate, location, temperature)
    })

    val t2 = Extraction.locationYearlyAverageRecords(formatted.collect)
    t2.foreach(println)


}
