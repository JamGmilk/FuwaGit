package jamgmilk.obsigit

import org.eclipse.jgit.api.Git
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider
import java.io.File

class GitManager(private val repoPath: File) {
    private val git: Git = Git.open(repoPath)

    // git status
    fun getStatus() {
        val status = git.status().call()
        println("Modified: ${status.modified}")
        println("Untracked: ${status.untracked}")
    }

    // git add -A
    fun addAll() {
        git.add().addFilepattern(".").call()
    }

    // git commit -m "message"
    fun commit(message: String) {
        git.commit().setMessage(message).call()
    }

    // git push
    fun push(token: String) {
        git.push()
            .setCredentialsProvider(UsernamePasswordCredentialsProvider("token", token))
            .call()
    }

    // git pull
    fun pull() {
        git.pull().call()
    }
}