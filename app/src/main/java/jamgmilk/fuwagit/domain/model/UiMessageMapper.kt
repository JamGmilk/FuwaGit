package jamgmilk.fuwagit.domain.model

import jamgmilk.fuwagit.R

fun UiMessage.toResource(): Int = when (this) {
    is UiMessage.Credential.PasswordMismatch -> R.string.vm_passwords_do_not_match
    is UiMessage.Credential.PasswordMinChars -> R.string.vm_password_min_chars
    is UiMessage.Credential.IncorrectOldPassword -> R.string.vm_incorrect_old_password

    is UiMessage.Branch.Deleted -> R.string.vm_branch_deleted_success
    is UiMessage.Branch.NotFullyMerged -> R.string.vm_branch_not_fully_merged
    is UiMessage.Branch.CannotDeleteCurrentBranch -> R.string.vm_branch_checked_out

    is UiMessage.Merge.Success -> R.string.vm_merge_success
    is UiMessage.Merge.ConflictHint -> R.string.vm_merge_conflict_hint
    is UiMessage.Merge.NotFullyMerged -> R.string.vm_merge_not_fully_merged
    is UiMessage.Merge.UpToDate -> R.string.vm_merge_up_to_date

    is UiMessage.Rebase.Success -> R.string.vm_rebase_success
    is UiMessage.Rebase.ConflictHint -> R.string.vm_rebase_conflict_hint
    is UiMessage.Rebase.UpToDate -> R.string.vm_rebase_up_to_date
    is UiMessage.Rebase.AbortHint -> R.string.vm_rebase_abort_hint

    is UiMessage.Conflict.Resolved -> R.string.vm_conflicts_resolved

    is UiMessage.Status.SelectTargetRepo -> R.string.vm_select_target_repo
    is UiMessage.Status.SelectTargetRepoPath -> R.string.vm_select_target_repo_path
    is UiMessage.Status.IsGitRepository -> R.string.vm_git_repository
    is UiMessage.Status.NotGitRepository -> R.string.vm_not_git_repository

    is UiMessage.Discard.ChangesDiscarded -> R.string.vm_changes_discarded
    is UiMessage.Discard.NotStagedOrLocked -> R.string.vm_discard_error_hint

    is UiMessage.Clean.NoUntrackedFiles -> R.string.vm_no_untracked_files
    is UiMessage.Clean.Failed -> R.string.vm_clean_failed
    is UiMessage.Clean.GetFilesFailed -> R.string.vm_clean_get_files_failed
    is UiMessage.Clean.GetListFailed -> R.string.vm_clean_get_list_failed

    is UiMessage.Checkout.CheckoutSuccess -> R.string.vm_checkout_success
    is UiMessage.Checkout.CheckoutFailed -> R.string.vm_checkout_failed

    is UiMessage.Create.BranchCreated -> R.string.vm_branch_created_success
    is UiMessage.Create.BranchCreateFailed -> R.string.vm_branch_creation_failed

    is UiMessage.Rename.RenameSuccess -> R.string.vm_branch_renamed_success
    is UiMessage.Rename.RenameFailed -> R.string.vm_branch_rename_failed

    is UiMessage.Tag.LightweightCreated -> R.string.vm_tag_lightweight_created
    is UiMessage.Tag.AnnotatedCreated -> R.string.vm_tag_annotated_created
    is UiMessage.Tag.Deleted -> R.string.vm_tag_deleted
    is UiMessage.Tag.PushSuccess -> R.string.vm_tag_push_success
    is UiMessage.Tag.Failed -> R.string.vm_tag_failed

    is UiMessage.Generic -> R.string.vm_unknown_error
}