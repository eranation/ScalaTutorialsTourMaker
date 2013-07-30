import au.com.bytecode.opencsv.CSVReader
import java.io.{File, InputStreamReader}
import java.util._
import org.fusesource.scalate._
import scala.collection.JavaConversions._
import scala.collection.mutable

object ScalaTutorialTourMaker extends App {

  val cl = Thread.currentThread().getContextClassLoader
  val props = new Properties
  props.load(cl.getResourceAsStream("config.properties"))

  val engine = new TemplateEngine


  val reader = new CSVReader(new InputStreamReader(cl.getResourceAsStream("tour.csv")))

  val headers = reader.readNext()
  val lines = reader.readAll()
  val totalLines = lines.length
  val files = for ((line, i) <- lines.zipWithIndex) yield {
    val map = line.zipWithIndex.map {
      case (col, j) => headers(j) -> col
    }.toMap.updated("isLast", i == totalLines - 1).updated("pageNumber", i + 1)

    val title: String = map("title").asInstanceOf[String] //ugly but quick
    val slug = title.toLowerCase.replaceAll(" ", "_").replaceAll("\\W", "")
    val fileName = s"${"%02d".format(i + 1)}_tour_of_scala_$slug"
    (fileName, map)
  }

  for (((fileName, map), index) <- files.zipWithIndex) {
    val outputFile = new File(props.getProperty("outputFolder"), fileName + ".md")
    println("writing to file " + outputFile.getAbsolutePath)
    printToFile(outputFile)(p => {
      val params = mutable.HashMap[String, Any]()
      params ++= map
      if (index < totalLines - 1) {
        params += "nextPage" -> (files(index + 1)._1 + ".html")
      }
      if (index > 0) {
        params += "prevPage" -> (files(index - 1)._1 + ".html")
      }
      val output = engine.layout("template.ssp", params.toMap)
      p.print(output)
    })
  }

  def printToFile(f: java.io.File)(op: java.io.PrintWriter => Unit) {
    val p = new java.io.PrintWriter(f)
    try {
      op(p)
    } finally {
      p.close()
    }
  }
}
