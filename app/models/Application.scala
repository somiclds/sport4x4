package models

import org.joda.time.DateTime
import scalikejdbc._

case class App(id: Int, auto_make: String, license_plate: String, driver: String,
                       email: String, phone_numb: String, address: String,
                       member_1: String, member_2: String, member_3: String, member_4: String,
                       sent: DateTime)

object App extends SQLSyntaxSupport[App] {

  private val auto = AutoSession
  private val a = App.syntax("a")
  def apply(a: SyntaxProvider[App])(rs: WrappedResultSet): App = autoConstruct(rs, a)

  def findAllApplications()(implicit s: DBSession = auto): List[App] = withSQL {
    select.from(App as a).orderBy(a.sent).desc
  }.map(App(a)).list.apply()

  def findByPlate(lplate: String)(implicit s: DBSession = auto): Option[App] = withSQL {
    select.from(App as a).where.eq(a.license_plate, lplate)
  }.map(App(a)).single.apply()

  def create(auto_make: String, license_plate: String, driver: String,
             email: String, phone_numb: String, address: String,
             member_1: String, member_2: String, member_3: String, member_4: String): Option[App] = {
    Option(App(0, auto_make, license_plate, driver, email, phone_numb, address,
          member_1, member_2, member_3, member_4, DateTime.now))
  }

  def insert(app: App)(implicit s: DBSession = auto) = {
    withSQL {
      insertInto(App).values(null, app.auto_make, app.license_plate, app.driver, app.email, app.phone_numb, app.address,
        app.member_1, app.member_2, app.member_3, app.member_4, DateTime.now)
    }.update().apply()
  }

  def delete(id: Int)(implicit s: DBSession = auto) = {
    withSQL {
      deleteFrom(App).where.eq(App.column.id, id)
    }.update.apply()
  }

}
