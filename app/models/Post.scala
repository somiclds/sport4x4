package models

import java.net.URLDecoder

import org.joda.time.DateTime
import scalikejdbc.QueryDSL.select
import scalikejdbc._
import sqls.count

case class Post(id: Int, title: String, body: String, author_id: Int, editor_id: Int,
                created: DateTime, modified: DateTime, post_category: Int, post_type: Int)

object Post extends SQLSyntaxSupport[Post] {

  private val auto = AutoSession
  //private val (p, b) = (Post.syntax("p"), PostBody.syntax("b"))
  private val a = Post.syntax("a")
  def apply(a: SyntaxProvider[Post])(rs: WrappedResultSet): Post = autoConstruct(rs, a)

  def findById(id: Int)(implicit s: DBSession = auto): Post = withSQL {
    select.from(Post as a).where.eq(a.id, id)
  }.map(Post(a)).single().apply().get

  def postCount(post_type: Int, post_category: Int)(implicit s: DBSession = auto): Int = {
    // post_type 0 means that type does not matter
    if (post_type != 0) {
      withSQL {
        select(count).from(Post as a).where.eq(a.post_category, post_category).and.eq(a.post_type, post_type)
      }.map(_.int(1)).single.apply().get
    }
    else {
      withSQL {
        select(count).from(Post as a).where.eq(a.post_category, post_category)
      }.map(_.int(1)).single.apply().get
    }
  }

  def pagesCount(post_type: Int, post_category: Int, postsPerPage: Int): Int = {
    Math.ceil(postCount(post_type, post_category).toDouble / postsPerPage).toInt
  }

  def findAllPosts()(implicit s: DBSession = auto): List[Post] = withSQL {
    select.from(Post as a).orderBy(a.created).desc
  }.map(Post(a)).list.apply()

  def findByCategory(post_category: Int)(implicit s: DBSession = auto): List[Post] = withSQL {
    select.from(Post as a).where.eq(a.post_category, post_category)
  }.map(Post(a)).list.apply()

  def findNews(page: Int, postsPerPage: Int)(implicit s: DBSession = auto): List[Post] = {
    val pCount = postCount(0, 1)
    if (postsPerPage < pCount) {
      val offset = (page - 1) * postsPerPage
      withSQL {
        select
          .from(Post as a)
          .where.eq(a.post_category, 1)
          .orderBy(a.created).desc
          .limit(postsPerPage)
          .offset(offset)
      }.map(Post(a)).list.apply()
    }
    else {
      withSQL {
        select.from(Post as a).where.eq(a.post_category, 1).orderBy(a.created).desc
      }.map(Post(a)).list.apply()
    }
  }

  def findPosts(post_type: Int, post_category: Int, page: Int, postsPerPage: Int)(implicit s: DBSession = auto): List[Post] = {
    val pCount = postCount(post_type, post_category)
    if (postsPerPage < pCount) {
      val offset = (page - 1) * postsPerPage
      withSQL {
        select
          .from(Post as a)
          .where.eq(a.post_category, post_category).and.eq(a.post_type, post_type)
          .orderBy(a.created).desc
          .limit(postsPerPage)
          .offset(offset)
      }.map(Post(a)).list.apply()
    }
    else {
      withSQL {
        select
          .from(Post as a).where.eq(a.post_category, post_category).and.eq(a.post_type, post_type).orderBy(a.created).desc
      }.map(Post(a)).list.apply()
    }
  }

  def create(title: String, body: String, post_category: Int, post_type: Int): Option[Post] = {
    Option(Post(0, title, body, 0, 0, DateTime.now(), DateTime.now(), post_category, post_type))
  }

  def insert(p: Post, author_id: Int)(implicit s: DBSession = auto) = {
    withSQL {
      insertInto(Post).values(null, p.title, p.body, author_id, author_id, DateTime.now(), DateTime.now(), p.post_category, p.post_type)
    }.update().apply()
  }

  def delete(id: Int)(implicit s: DBSession = auto) = {
    withSQL {
      deleteFrom(Post).where.eq(Post.column.id, id)
    }.update.apply()
  }

  def update(id: Int, p: Post, editor_id: Int)(implicit s: DBSession = auto): Option[Post] = {
    withSQL {
      QueryDSL.update(Post).set(
        Post.column.title -> p.title,
        Post.column.body -> p.body,
        Post.column.post_type -> p.post_type,
        Post.column.post_category -> p.post_category,
        Post.column.editor_id -> editor_id,
        Post.column.modified -> DateTime.now()
      ).where.eq(Post.column.id, id)
    }.update.apply()
    Option(Post(p.id, p.title, p.body, p.author_id, editor_id, p.created, DateTime.now(), p.post_category, p.post_type))
  }

  /*def findBody(id: Int)(implicit s: DBSession = auto): Option[PostBody] = withSQL {
    select.from(PostBody as b).leftJoin(Post as p).on(b.post_id, p.id)
      .where.eq(p.id, id)
  }.map(PostBody(b)).single.apply()*/

}