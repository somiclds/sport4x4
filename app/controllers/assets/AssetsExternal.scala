package controllers.assets

import java.io.File

import play.Play
import play.api.mvc.{Action, AnyContent, Controller}

class AssetsExternal extends Controller {

  val AbsolutePath = """^(/|[a-zA-Z]:\\).*""".r

  /**
    * Generates an `Action` that serves a static resource from an external folder
    *
    * @param rootPath the root folder for searching the static resource files.
    */
  def at(rootPath: String, years: Int, month: Int, day: Int, name: String): Action[AnyContent] = Action { request =>
    val file = years+"/"+month+"/"+day+"/"+name

    Console.println(file)

    val fileToServe = rootPath match {
      case AbsolutePath(_) => new File(rootPath, file)
      case _ => new File(Play.application.getFile(rootPath), file)
    }

    if (fileToServe.exists && !fileToServe.isDirectory) {
      Ok.sendFile(fileToServe, inline = true)
    } else {
      NotFound
    }
  }

}

object AssetsExternal extends AssetsExternal
