import org.eclipse.jgit.api.Git
import org.eclipse.jgit.lib.RepositoryBuilder
import org.eclipse.jgit.transport.CredentialsProvider
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider
import java.io.File


// http://www.eclipse.org/jgit
// https://www.vogella.com/tutorials/JGit/article.html

fun main(args: Array<String>) {
    val username = "ky-sc"
    val password = "6b792d7363"
    val repo = RepositoryBuilder()
            .readEnvironment()
            .findGitDir() //.setGitDir(File("./repTemp/.git"))
            .setMustExist(true)
            .build();
    val git = Git(repo)
    val cp: CredentialsProvider = UsernamePasswordCredentialsProvider(username, password)
    git.pull().setCredentialsProvider(cp).call()
}


fun clone(localRepoDir: File) {
    if (!localRepoDir.exists()) {
        Git.cloneRepository()
                .setURI("https://github.com/eclipse/jgit.git")
                .setDirectory(localRepoDir)
                .call()
    }

}