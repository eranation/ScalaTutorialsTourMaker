import au.com.bytecode.opencsv.CSVReader
import java.io.{PrintWriter, File, InputStreamReader}
import java.util.Properties
import org.fusesource.scalate._
import scala.collection.JavaConversions._
import scala.collection.mutable
import scala.util.parsing.json.{JSONArray, JSONObject}
//import spray.json._
//import DefaultJsonProtocol._



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
    (fileName, title, map)
  }

  val outputFolder = props.getProperty("outputFolder")
  //ugly hack to keep links to / and the old link working, (since Jekyll doesn't have redirects per-se)
  val indexFile = new File(outputFolder, "index.md")
  val oldFile = new File(outputFolder, "tour-of-scala1.md")

  for (((fileName, _ , map), index) <- files.zipWithIndex) {
    val outputFile = new File(outputFolder, fileName + ".md")
    println("writing to file " + outputFile.getAbsolutePath)
    def op(toc:Boolean)(p:PrintWriter) {
      val params = mutable.HashMap[String, Any]()
      params ++= map
      params += "group" -> (if(toc) "tour" else "hidden")

      if (index < totalLines - 1) {
        params += "nextPage" -> (files(index + 1)._1 + ".html")
      }
      if (index > 0) {
        params += "prevPage" -> (files(index - 1)._1 + ".html")
      }
      val output = engine.layout("template.ssp", params.toMap)
      p.print(output)
    }
    printToFile(outputFile, op(toc = true))
    if(index == 0) {
      printToFile(indexFile, op(toc = false))
      printToFile(oldFile, op(toc = false))
    }
  }

  val tocJSONFile = new File(outputFolder, "toc.json")
  val filesList = files.map(i => Map("url" -> (i._1 + ".html"), "title" -> i._2)).toList

  val filesListJSON= JSONArray(filesList.map(JSONObject).toList)

  printToFile(tocJSONFile, _.print(filesListJSON.toString()))

  val tocFile = new File(outputFolder, "00_toc.html")
  val output = engine.layout("toc.ssp", Map("pages" -> filesList))
  printToFile(tocFile, _.print(output))




  def printToFile(f: java.io.File, op: java.io.PrintWriter => Unit) {
    val p = new java.io.PrintWriter(f)
    try {
      op(p)
    } finally {
      p.close()
    }
  }
}
