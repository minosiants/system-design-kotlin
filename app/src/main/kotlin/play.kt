




//git@github.com:minosiants/pencil.git
//git@bitbucket.org:minosiants/test.git
//import org.dstadler.jgit.helper.SimpleProgressMonitor;
import arrow.core.Either
import org.eclipse.jgit.api.Git as JGit
import org.eclipse.jgit.internal.transport.sshd.CachingKeyPairProvider
import org.eclipse.jgit.lib.ProgressMonitor
import org.eclipse.jgit.transport.SshSessionFactory
import org.eclipse.jgit.transport.sshd.JGitKeyCache
import org.eclipse.jgit.transport.sshd.SshdSessionFactoryBuilder
import org.eclipse.jgit.util.FS
import java.io.File


sealed interface AppError {

}

typealias AppResult<B> = Either<AppError,B>
@JvmInline
value class Name(private val value:String)
data class Project(val name:Name)

sealed interface Provider {
    fun url():String {
        return when(this){
            is BitBucket -> "bitbucket.org"
            is Generic -> value
        }
    }
    data object BitBucket : Provider
    data class Generic(val value:String):Provider
}


fun MatchGroupCollection.safeGet(name:String):AppResult<String> {
    this.get(name)
}
data class Repo(val name:Name, val project:Project, val provider: Provider){
    fun uri():String {
        return """git@${provider.url()}:${name}/${project.name}.git"""
    }
    companion object {
        fun fromUri(value: String):AppResult<String> {
            Regex.fromLiteral("""git@bitbucket\.com:(?<name>[^/]+)\/(?<project>[^\.]+)\.git""")
                .findAll(value).map { it.groups.get("name").value }
            return null
        }
    }
}

interface Git {
    fun clone()
}

fun main() {
    val cache = JGitKeyCache()
    val privateKey = "id_ed25519"
    val  sshdSessionFactory = SshdSessionFactoryBuilder()
        .setPreferredAuthentications("publickey")
        .setDefaultKeysProvider{f -> CachingKeyPairProvider(listOf(f.toPath().resolve(privateKey)), cache) }
        .setHomeDirectory(FS.DETECTED.userHome())
        .setSshDirectory( File(FS.DETECTED.userHome(), ".ssh"))
        .build(JGitKeyCache())
    SshSessionFactory.setInstance(sshdSessionFactory)
    val result = JGit.cloneRepository()
        .setURI("git@github.com:minosiants/pencil.git")
        .setDirectory(File("/Users/kaspar/stuff/sources/pencil2"))
        .setProgressMonitor(SimpleProgressMonitor())
        .call()
    println("Having repository: " + result.repository.directory);

}


class SimpleProgressMonitor : ProgressMonitor {
    override fun start(totalTasks: Int) {
        println("Starting work on $totalTasks tasks")
    }

    override fun beginTask(title: String, totalWork: Int) {
        println("Start $title: $totalWork")
    }

    override fun update(completed: Int) {
        print("$completed-")
    }

    override fun endTask() {
        println("Done")
    }

    override fun isCancelled(): Boolean {
       return false
    }

    override fun showDuration(enabled: Boolean) {
        // ignored here
    }
}