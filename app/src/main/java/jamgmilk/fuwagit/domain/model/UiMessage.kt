package jamgmilk.fuwagit.domain.model

sealed class UiMessage {
    sealed class Credential : UiMessage() {
        data object PasswordMismatch : Credential()
        data class PasswordMinChars(val count: Int = 6) : Credential()
        data object IncorrectOldPassword : Credential()
    }

    sealed class Branch : UiMessage() {
        data class Deleted(val branchName: String) : Branch()
        data object NotFullyMerged : Branch()
        data object CannotDeleteCurrentBranch : Branch()
    }

    sealed class Merge : UiMessage() {
        data class Success(val branchName: String) : Merge()
        data object ConflictHint : Merge()
        data object NotFullyMerged : Merge()
        data object UpToDate : Merge()
    }

    sealed class Rebase : UiMessage() {
        data class Success(val branchName: String) : Rebase()
        data object ConflictHint : Rebase()
        data object UpToDate : Rebase()
        data object AbortHint : Rebase()
    }

    sealed class Conflict : UiMessage() {
        data object Resolved : Conflict()
    }

    sealed class Status : UiMessage() {
        data object SelectTargetRepo : Status()
        data object SelectTargetRepoPath : Status()
        data object IsGitRepository : Status()
        data object NotGitRepository : Status()
    }

    sealed class Discard : UiMessage() {
        data class ChangesDiscarded(val fileName: String) : Discard()
        data object NotStagedOrLocked : Discard()
    }

    sealed class Clean : UiMessage() {
        data object NoUntrackedFiles : Clean()
        data class Failed(val reason: String) : Clean()
        data class GetFilesFailed(val reason: String) : Clean()
        data class GetListFailed(val reason: String) : Clean()
    }

    sealed class Checkout : UiMessage() {
        data class CheckoutSuccess(val branchName: String) : Checkout()
        data class CheckoutFailed(val reason: String) : Checkout()
    }

    sealed class Create : UiMessage() {
        data class BranchCreated(val branchName: String) : Create()
        data class BranchCreateFailed(val reason: String) : Create()
    }

    sealed class Rename : UiMessage() {
        data class RenameSuccess(val newName: String) : Rename()
        data class RenameFailed(val reason: String) : Rename()
    }

    sealed class Tag : UiMessage() {
        data class LightweightCreated(val tagName: String) : Tag()
        data class AnnotatedCreated(val tagName: String) : Tag()
        data class Deleted(val tagName: String) : Tag()
        data class PushSuccess(val tagName: String) : Tag()
        data class Failed(val reason: String) : Tag()
    }

    data class Generic(val message: String) : UiMessage()
}