package controllers.master;

import java.io.File

import akka.event.Logging.Error
import controllers.main.Application._
import controllers.stack.Pjax
import jp.t2v.lab.play2.auth.AuthElement
import models.{Account, App, Post, Role}
import models.Role.Master
import models.Role._
import org.joda.time.DateTime
import play.api.data.Form
import play.api.data.Forms._
import play.api.data.validation.{Constraint, Invalid, Valid}
import play.api.libs.json.{JsObject, JsString, Json}
import play.api.mvc.{Action, Controller}
import views.html

import scala.util.parsing.json.JSONObject

class Mastercms extends Controller with Pjax with AuthElement with AuthConfigImpl {

  private val postsPerPage = 5;

  private val postForm = Form {
    mapping(
      "title" -> text.verifying("Antraštė yra privaloma", title => !title.isEmpty()),
      "body" -> text.verifying("Turinys yra privalomas", body => !body.isEmpty()),
      "postCategory" -> number,
      "postType" -> number
    )(Post.create)(_.map(u => (u.title, u.body, u.post_category, u.post_type)))
  }

  private def getTypeNum(t: String) = t.toLowerCase match {
    case "" => 0
    case "rallyraid" => 1
    case "trophyraid" => 2
    case _ => -1
  }
  private def getCategoryNum(cat: String) = cat.toLowerCase match {
    case "naujienos" => 1
    case "dokumentai" => 2
    case "asmenybes" => 3
    case "remejai" => 4
    case "istorija" => 5
    case _ => -1
  }

  def main(t: String, cat: String, page: Int) = StackAction(AuthorityKey -> Administrator) { implicit request =>

    val typeNum = getTypeNum(t)
    val categoryNum = getCategoryNum(cat)

    if(typeNum == -1 || categoryNum == -1){
      Redirect(routes.Mastercms.main("", "Naujienos", 1))
    }
    else if(typeNum == 0 && categoryNum == 1){
      Ok(views.html.master.main("4x4sport/Naujienos", typeNum, categoryNum, page, postsPerPage, postForm))
    }
    else{
      Ok(views.html.master.main(t.capitalize+"/"+cat.capitalize, typeNum, categoryNum, page, postsPerPage, postForm))
    }

  }

  def createPost(t: String, cat: String, page: Int) = StackAction(AuthorityKey -> Administrator) { implicit request =>
    postForm.bindFromRequest.fold(
      formWithErrors => BadRequest(html.master.main("", getTypeNum(t), getCategoryNum(cat), page, postsPerPage, formWithErrors)),
      post           => {
        Post.insert(post.get, loggedIn.id)
        Redirect(routes.Mastercms.main(t, cat, page))
      }
    )
  }

  def updatePost(t: String, cat: String, page: Int, id: Int) = StackAction(AuthorityKey -> Administrator) { implicit request =>
    postForm.bindFromRequest.fold(
      formWithErrors => BadRequest(html.master.main("", getTypeNum(t), getCategoryNum(cat), page, postsPerPage, formWithErrors)),
      post           => {
        Post.update(id, post.get, loggedIn.id)
        Redirect(routes.Mastercms.main(t, cat, page))
      }
    )
  }

  def deletePost(t: String, cat: String, page: Int, id: Int) = StackAction(AuthorityKey -> Administrator) { implicit request =>
    Post.delete(id)
    Redirect(routes.Mastercms.main(t, cat, page))
  }

  private val registrationForm = Form{
    mapping(
      "email" -> email
        .verifying("El. paštas yra privalomas", email => !email.isEmpty)
        .verifying(
        "Toks el. paštas jau naudojamas", email => Account.findByEmail(email.toString).isEmpty
      ),
      "password" -> tuple(
        "main" -> text.verifying("Slaptažodis yra privalomas", main => !main.isEmpty),
        "confirm" -> text.verifying("Patvirtinkite slaptažodį", confirm => !confirm.isEmpty)
      ).verifying(
        "Slaptažodžiai nesutampa", password => password._1 == password._2
      ).transform[String](
        password => password._1,
        password => ("", "")
      ),
      "name" -> text.verifying("Vardas yra privalomas", name => !name.isEmpty),
      "role" -> text.verifying(
        "Tipas yra privalomas", role => Role.valueOf(role) != null && role == Role.valueOf(role).toString
      )
    )(Account.create)(_.map(u => (u.email, "", u.name, u.role.toString)))
  }

  def admins = StackAction(AuthorityKey -> Master) { implicit request =>
    Ok(html.master.admins(loggedIn, registrationForm))
  }

  def applications = StackAction(AuthorityKey -> Administrator) { implicit request =>
    Ok(html.master.applications(loggedIn))
  }

  def deleteApplication(id: Int) = StackAction(AuthorityKey -> Master) { implicit request =>
    App.delete(id)
    Redirect(routes.Mastercms.applications)
  }

  def addUser = StackAction(AuthorityKey -> Master) { implicit request =>
    registrationForm.bindFromRequest.fold(
      formWithErrors => BadRequest(html.master.admins(loggedIn, formWithErrors)),
      user           => Redirect(routes.Mastercms.admins)
    )
  }

  def deleteUser(id: Int) = StackAction(AuthorityKey -> Master) { implicit request =>
      Account.delete(id)
      Redirect(routes.Mastercms.admins)
  }

  def fileBrowser = StackAction(AuthorityKey -> Administrator) { implicit request =>
    Ok(html.master.dropzone("Dropzone"))
  }

  def uploadFile = StackAction(parse.multipartFormData, AuthorityKey -> Administrator) { implicit request =>
      request.body.file("file").map { file =>
        import java.io.File

      val filename = file.filename

      //for windows machine
      //val uploadsDir = """D:/ScalaProjects/sport4x4/public/uploads/"""

      //for server
      val uploadsDir = "/srv/sport4x4/uploads/"

      val fileLocation = DateTime.now.year.get + """/""" + DateTime.now.monthOfYear.get +
        """/""" + DateTime.now.dayOfMonth.get + """/""" + filename
        val newFile = new File(uploadsDir + fileLocation)

        val parentDir = newFile.getParentFile()
        if(!parentDir.exists()){
          parentDir.mkdirs()
          Console.println("Directory created: "+ parentDir.getPath)
        }
        file.ref.moveTo(newFile)
        Console.println("File uploaded: "+ newFile.getPath)
        //for windows machine
        //Ok("/assets/uploads/" + fileLocation)
        //for server
        Ok("/uploads/" + fileLocation)
      }.getOrElse {
        InternalServerError("Failed to upload file")
      }
  }

  def deleteFile = StackAction(AuthorityKey -> Master) { implicit request =>
    val fileUrl: Option[String] = request.body.asFormUrlEncoded.flatMap(m => m.get("fileUrl").flatMap(_.headOption))

    // Comment following two lines and uncomment third when not in windows machine
    //val url = """D:/ScalaProjects/sport4x4/public"""+fileUrl.get.substring(7)
    //val file = new File(url)
    val file = new File("/srv/sport4x4/" + fileUrl.get)

    if(file.delete()){
      Console.println("deleted "+file)
      Ok("File deleted")
    }else {
      Console.println("not deleted " + file)
      InternalServerError("Failed to delete a file")
    }

  }

  def getFiles = StackAction(AuthorityKey -> Administrator) { implicit request =>

    // for windows machine
    //val uploadsDir = new File("""D:/ScalaProjects/sport4x4/public/uploads""")
    //val pathToUploads = "/assets/uploads/"

    // for server
    val uploadsDir = new File("/srv/sport4x4/uploads")
    val pathToUploads = "/srv/sport4x4/uploads/"

    if(uploadsDir.exists()) {
      val fileList = {
        for {
          yearsDir <- uploadsDir.listFiles
          if yearsDir.isDirectory;
          monthsDir <- yearsDir.listFiles
          if monthsDir.isDirectory;
          filesDir <- monthsDir.listFiles
          if filesDir.isDirectory
          file <- filesDir.listFiles()
        } yield Json.obj(
          "name" -> file.getName,
          "fileType" -> file.getName.substring(file.getName.lastIndexOf('.')),
          //for windows machine
          //"path" -> JsString(pathToUploads + yearsDir.getName +"/"+ monthsDir.getName +"/"+ filesDir.getName +"/"+ file.getName),
          //for server
          "path" -> JsString("/uploads/" + yearsDir.getName +"/"+ monthsDir.getName +"/"+ filesDir.getName +"/"+ file.getName),
          "size" -> file.length
        )
      }
      Ok(Json.toJson(fileList))
    }
    else {
      Ok(Json.toJson(Array[JsObject]()))
    }

  }

  protected val fullTemplate:
  User => Template = html.master.masterTemplate.apply

}

object Mastercms extends Mastercms