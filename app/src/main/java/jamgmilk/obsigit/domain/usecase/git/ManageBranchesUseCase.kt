package jamgmilk.obsigit.domain.usecase.git

import jamgmilk.obsigit.data.source.JGitDataSource
import java.io.File

class ManageBranchesUseCase {
    
    suspend fun checkout(repoPath: String, branchName: String): Result<Unit> {
        return try {
            val dir = File(repoPath)
            JGitDataSource.withGitLock { 
                JGitDataSource.checkoutBranch(dir, branchName) 
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun create(repoPath: String, branchName: String): Result<Unit> {
        return try {
            val dir = File(repoPath)
            JGitDataSource.withGitLock { 
                JGitDataSource.createBranch(dir, branchName) 
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun delete(repoPath: String, branchName: String, force: Boolean = false): Result<Unit> {
        return try {
            val dir = File(repoPath)
            JGitDataSource.withGitLock { 
                JGitDataSource.deleteBranch(dir, branchName, force) 
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun merge(repoPath: String, branchName: String): Result<Unit> {
        return try {
            val dir = File(repoPath)
            JGitDataSource.withGitLock { 
                JGitDataSource.mergeBranch(dir, branchName) 
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun rebase(repoPath: String, branchName: String): Result<Unit> {
        return try {
            val dir = File(repoPath)
            JGitDataSource.withGitLock { 
                JGitDataSource.rebaseBranch(dir, branchName) 
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
