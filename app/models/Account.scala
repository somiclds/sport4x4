package models

import java.text.Normalizer.Form

import org.mindrot.jbcrypt.BCrypt
import scalikejdbc._
import _root_.play.twirl.api.Html

case class Account(id: Int, email: String, password: String, name: String, role: Role)

object Account extends SQLSyntaxSupport[Account] {

  private val a = Account.syntax("a")

  def apply(a: SyntaxProvider[Account])(rs: WrappedResultSet): Account = autoConstruct(rs, a)

  private val auto = AutoSession

  def authenticate(email: String, password: String)(implicit s: DBSession = auto): Option[Account] = {
    findByEmail(email).filter { account => BCrypt.checkpw(password, account.password) }
  }

  def findByEmail(email: String)(implicit s: DBSession = auto): Option[Account] = withSQL {
    select.from(Account as a).where.eq(a.email, email)
  }.map(Account(a)).single.apply()

  def findById(id: Int)(implicit s: DBSession = auto): Option[Account] = withSQL {
    select.from(Account as a).where.eq(a.id, id)
  }.map(Account(a)).single.apply()

  def findAll()(implicit s: DBSession = auto): List[Account] = withSQL {
    select.from(Account as a)
  }.map(Account(a)).list.apply()

  def create(email: String, password: String, name: String, role: String)(implicit s: DBSession = auto): Option[Account] = {
    withSQL {
      val pass = BCrypt.hashpw(password, BCrypt.gensalt())
      insert.into(Account).values(null, email, pass, name, role)
    }.update.apply()
    Option(Account(0, email, password, name, Role.valueOf(role)))
  }

  def delete(id: Int)(implicit s: DBSession = auto) = {
    withSQL {
      deleteFrom(Account).where.eq(Account.column.id, id)
    }.update.apply()
  }

}