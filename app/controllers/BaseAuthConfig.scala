package controllers

import jp.t2v.lab.play2.auth._
import play.api.mvc.RequestHeader
import play.api.mvc.Results._

import scala.concurrent.{ExecutionContext, Future}
import scala.reflect._

import scala.collection.concurrent.TrieMap
import scala.util.Random
import java.security.SecureRandom

import models.Role.{Administrator, Master}
import models.{Account, Role}

import scala.annotation.tailrec

import scala.reflect.ClassTag

trait BaseAuthConfig  extends AuthConfig {

  /**
    * A type that is used to identify a user.
    * `String`, `Int`, `Long` and so on.
    */
  type Id = Int

  /**
    * A type that represents a user in your application.
    * `User`, `Account` and so on.
    */
  type User = Account

  /**
    * A type that is defined by every action for authorization.
    *
    * sealed trait Role
    * case object Administrator extends Role
    * case object Master extends Role
    */
  type Authority = Role

  /**
    * A `ClassTag` is used to retrieve an id from the Cache API.
    * Use something like this:
    */
  val idTag: ClassTag[Id] = classTag[Id]

  /**
    * The session timeout in seconds
    */
  val sessionTimeoutInSeconds = 3600

  /**
    * A function that returns a `User` object from an `Id`.
    * You can alter the procedure to suit your application.
    */
  def resolveUser(id: Id)(implicit ctx: ExecutionContext) = Future.successful(Account.findById(id))


  def authorizationFailed(request: RequestHeader)(implicit ctx: ExecutionContext) = throw new AssertionError("don't use")

  /**
    * If authorization failed (usually incorrect password) redirect the user as follows:
    */
  override def authorizationFailed(request: RequestHeader, user: User, authority: Option[Authority])(implicit ctx: ExecutionContext) = {
    Future.successful(Forbidden("no permission"))
  }

  /**
    * A function that determines what `Authority` a user has.
    */
  def authorize(user: User, authority: Authority)(implicit ctx: ExecutionContext) = {
    Future.successful((user.role, authority) match {
      case (Administrator, Administrator) => true
      case (Master, _) => true
      case _ => false
    })
  }

  /**
    * (Optional)
    * You can custom SessionID Token handler.
    * Default implementation use Cookie.
    */
  override lazy val tokenAccessor = new CookieTokenAccessor(
    /*
     * Whether use the secure option or not use it in the cookie.
     * Following code is default.
     */
    cookieSecureOption = false,
    cookieMaxAge       = Some(sessionTimeoutInSeconds)
  )

  override lazy val idContainer: AsyncIdContainer[Id] = new AsyncIdContainer[Id] {

    private val tokenSuffix = ":token"
    private val userIdSuffix = ":userId"
    private val random = new Random(new SecureRandom())

    override def startNewSession(userId: Id, timeoutInSeconds: Int)(implicit request: RequestHeader, context: ExecutionContext): Future[AuthenticityToken] = {
      removeByUserId(userId)
      val token = generate()
      store(token, userId, timeoutInSeconds)

      Future.successful(token)
    }

    @tailrec
    private final def generate(): AuthenticityToken = {
      val table = "abcdefghijklmnopqrstuvwxyz1234567890_.~*'()"
      val token = Iterator.continually(random.nextInt(table.size)).map(table).take(64).mkString
      if (syncGet(token).isDefined) generate() else token
    }

    private def removeByUserId(userId: Id) {
      GlobalMap.container.get(userId.toString + userIdSuffix).map(_.asInstanceOf[String]) foreach unsetToken
      unsetUserId(userId)
    }

    override def remove(token: AuthenticityToken)(implicit context: ExecutionContext): Future[Unit] = {
      get(token).map(_ foreach unsetUserId)
      Future.successful(unsetToken(token))
    }

    private def unsetToken(token: AuthenticityToken) {
      GlobalMap.container.remove(token + tokenSuffix)
    }
    private def unsetUserId(userId: Id) {
      GlobalMap.container.remove(userId.toString + userIdSuffix)
    }

    override def get(token: AuthenticityToken)(implicit context: ExecutionContext): Future[Option[Id]] = {
      Future.successful(syncGet(token))
    }

    private def syncGet(token: AuthenticityToken): Option[Id] = {
      GlobalMap.container.get(token + tokenSuffix).map(_.asInstanceOf[Id])
    }

    private def store(token: AuthenticityToken, userId: Id, timeoutInSeconds: Int) {
      GlobalMap.container.put(token + tokenSuffix, userId.asInstanceOf[AnyRef]/*, timeoutInSeconds*/) // TODO:
      GlobalMap.container.put(userId.toString + userIdSuffix, token.asInstanceOf[AnyRef]/*, timeoutInSeconds*/) // TODO:
    }

    override def prolongTimeout(token: AuthenticityToken, timeoutInSeconds: Int)(implicit request: RequestHeader, context: ExecutionContext): Future[Unit] = {
      Future.successful(syncGet(token).foreach(store(token, _, timeoutInSeconds)))
    }

  }

}

object GlobalMap {
  private[controllers] val container: TrieMap[String, AnyRef] = new TrieMap[String, AnyRef]()
}