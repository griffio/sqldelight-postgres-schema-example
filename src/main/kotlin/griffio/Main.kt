package griffio

import app.cash.sqldelight.driver.jdbc.asJdbcDriver
import griffio.migrations.Categories
import griffio.migrations.Posts
import griffio.migrations.Reactions
import griffio.migrations.Threads
import griffio.migrations.User_profiles as UserProfiles
import griffio.queries.Sample

import org.postgresql.ds.PGSimpleDataSource
import java.time.OffsetDateTime

private fun getSqlDriver() = PGSimpleDataSource().apply {
    setURL("jdbc:postgresql://localhost:5432/forum-sample")
    applicationName = "App Main"
}.asJdbcDriver()

val driver = getSqlDriver()
val forum = Sample(driver)

fun stringIdentifier(n: Int) = (1..n).map { ('A'..'Z').random() }.joinToString("")
fun longIdentifier(n: Int) = (1..n).map { (1..10).random() }.joinToString("").toLong()
fun email(username: String) = "${username}@example.org"

fun newUserProfile(username: String, email: String, bio: String?) =
    UserProfiles(
        user_id = -1,
        email = email,
        username = username,
        bio = bio,
        is_banned = false,
        created_at = OffsetDateTime.MIN
    )

fun newCategory(name: String, slug: String, description: String, displayOrder: Int) =
    Categories(
        category_id = -1,
        name = name,
        slug = slug,
        description = description,
        display_order = displayOrder,
        created_at = OffsetDateTime.MIN
    )

fun newThread(category: Categories, userProfile: UserProfiles, title: String ) =
    Threads(-1, category.category_id, userProfile.user_id, title,
        is_locked = false,
        is_pinned = false,
        reply_count = 0,
        last_post_at = OffsetDateTime.MIN,
        created_at = OffsetDateTime.MIN
    )

fun newPost(thread: Threads, userProfile: UserProfiles, parentPost: Posts?, body: String) =
    Posts(
        post_id = -1,
        thread_id = thread.thread_id,
        author_id = userProfile.user_id,
        parent_post_id = parentPost?.post_id,
        body = body,
        is_deleted = false,
        created_at = OffsetDateTime.MIN,
        updated_at = OffsetDateTime.MIN
    )

fun newReaction(post: Posts, userProfile: UserProfiles, kind: String) =
    Reactions(
        reaction_id = -1,
        post_id = post.post_id,
        user_id = userProfile.user_id,
        kind = kind,
        created_at = OffsetDateTime.MIN
    )

fun UserProfiles.insert() = forum.profileQueries.insert(this).executeAsOne()

fun Categories.insert() = forum.categoryQueries.insert(this).executeAsOne()

fun Threads.insert() = forum.threadQueries.insert(this).executeAsOne()

fun Posts.insert() = forum.postQueries.insert(this).executeAsOne()

fun Reactions.insert() = forum.reactionQueries.insert(this).executeAsOne()

fun main() {

    val userName = stringIdentifier(10).lowercase()
    val emailAddress = email(userName).lowercase()
    val profile = newUserProfile(userName, emailAddress, "bio for $userName").insert()
    val category = newCategory("Category $userName", userName, "description for $userName", 1).insert()
    val thread = newThread(category, profile, "Thread ${stringIdentifier(10)}").insert()
    val post = newPost(thread, profile, null, "Post ${stringIdentifier(128)}").insert()
    val reaction = newReaction(post, profile, "like").insert()

    println(profile)
    println(category)
    println(thread)
    println(post)
    println(reaction)

    println(forum.postQueries.updateBody("Updated body", post.post_id).value)
    println(forum.postQueries.updateDeleted(true, post.post_id).value)
}
