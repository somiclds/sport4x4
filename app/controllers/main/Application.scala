package controllers.main

import play.api.data.Form
import play.api.data.Forms._
import play.api.data.validation.{Constraint, Invalid, Valid}
import play.api.mvc._
import models.App
object Application extends Controller {

  private val postsPerPage = 5;

  private val applicationForm = Form {
    mapping(
      "auto_make" -> text.verifying("Markė yra privaloma", auto_make => !auto_make.isEmpty()),
      "license_plate" -> text
        .verifying("Valstybinis nr. yra privalomas", license_plate => !license_plate.isEmpty())
        .verifying("Automobilis su tokiais numeriais jau užregistruotas", lplate => App.findByPlate(lplate).isEmpty),
      "driver" -> text.verifying("Vairuotojo laukas yra privalomas", driver => !driver.isEmpty()),
      "email" -> email.verifying("El. paštas yra privalomas", email => !email.isEmpty),
      "phone_numb" -> text.verifying("Blogas telefono numeris", phone_numb => !phone_numb.isEmpty() && phone_numb.matches("^\\+?\\d+$" +
        "")),
      "address" -> text.verifying("Adresas yra privalomas", address => !address.isEmpty()),
      "member_1" -> text,
      "member_2" -> text,
      "member_3" -> text,
      "member_4" -> text
    )(App.create)(_.map(u => (u.auto_make, u.license_plate, u.driver, u.email, u.phone_numb,
      u.address, u.member_1, u.member_2, u.member_3, u.member_4)))
  }

  // t - type (RallyRaid or TrophyRaid
  // cat - category (Naujienos, Dokumentai, etc.)
  def index(t: String, cat: String, page: Int) = Action {

    val typeNum = t.toLowerCase match {
      case "" => 0
      case "rallyraid" => 1
      case "trophyraid" => 2
      case _ => -1
    }
    val categoryNum = cat.toLowerCase match {
      case "naujienos" => 1
      case "dokumentai" => 2
      case "asmenybes" => 3
      case "remejai" => 4
      case "istorija" => 5
      case _ => -1
    }

    val posts = models.Post.postCount(typeNum, categoryNum)


    if(typeNum == -1 || categoryNum == -1){
      Redirect(routes.Application.index("", "Naujienos", 1))
    }
    else if(typeNum == 0 && categoryNum == 1){
      //Ok(views.html.main.news("4x4sport/Naujienos", 0))
      Ok(views.html.main.index("4x4sport/Naujienos")(typeNum, categoryNum, page, postsPerPage))
    }
    else if(categoryNum == 2) {
      Ok(views.html.main.docs(applicationForm)("4x4sport/"+cat, typeNum, page, postsPerPage))
    }
    else{
      Ok(views.html.main.index(t.capitalize+"/"+cat.capitalize)(typeNum, categoryNum, page, postsPerPage))
    }
  }

  def showPost(id: Int) = Action {
    val post = models.Post.findById(id)
    if(post != null){
      Ok(views.html.main.showpost(post))
    }
    else {
     BadRequest("Post with id: '"+id+"' was not found")
    }
  }

  def applyToCompetition = Action { implicit request =>
    applicationForm.bindFromRequest.fold(
      formWithErrors => BadRequest(views.html.main.docs(formWithErrors)("4x4sport/Dokumentai", 2, 1, postsPerPage)),
      app => {
        App.insert(app.get)
        Ok(views.html.main.redirect("Jūsų paraiška buvo sėkmingai išsiųsta!"))
      }
    )
  }

}

