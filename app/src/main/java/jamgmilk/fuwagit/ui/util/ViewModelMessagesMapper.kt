package jamgmilk.fuwagit.ui.util

import jamgmilk.fuwagit.R

/**
 * Maps hardcoded ViewModel messages to string resource IDs for localization.
 * This allows ViewModels to remain framework-agnostic while the UI layer handles localization.
 */
object ViewModelMessagesMapper {

    /**
     * Maps a ViewModel message to its localized string resource ID.
     * @param message The hardcoded message from the ViewModel
     * @param defaultMessage Fallback resource ID if no match is found
     * @return The resource ID for the localized string
     */
    fun mapMessageToResource(message: String?, defaultMessage: Int = R.string.vm_unknown_error): Int {
        if (message == null) return defaultMessage

        return when {
            // Credential messages
            message == "Passwords do not match" -> R.string.vm_passwords_do_not_match
            message == "New password must be at least 6 characters" -> R.string.vm_password_min_8_chars
            message == "Incorrect old password" -> R.string.vm_incorrect_old_password

            // Branch delete messages
            message.contains("deleted successfully") -> R.string.vm_branch_deleted_success
            message.contains("haven't been merged") -> R.string.vm_branch_not_fully_merged
            message.contains("currently checked out") -> R.string.vm_branch_checked_out

            // Merge messages
            message.contains("Successfully merged") -> R.string.vm_merge_success
            message.contains("Resolve the conflicts manually") -> R.string.vm_merge_conflict_hint
            message.contains("unmerged commits") -> R.string.vm_merge_not_fully_merged
            message.contains("up to date") -> R.string.vm_merge_up_to_date

            // Rebase messages
            message.contains("Successfully rebased") -> R.string.vm_rebase_success
            message.contains("git rebase --continue") -> R.string.vm_rebase_conflict_hint
            message.contains("already up to date") -> R.string.vm_rebase_up_to_date
            message.contains("git rebase --abort") -> R.string.vm_rebase_abort_hint

            // Conflict messages
            message == "Conflicts resolved" -> R.string.vm_conflicts_resolved

            // Status messages
            message == "Select a target repo" -> R.string.vm_select_target_repo
            message == "Select a target repo path" -> R.string.vm_select_target_repo_path
            message == "Git repository" -> R.string.vm_git_repository
            message == "Not a git repository" -> R.string.vm_not_git_repository
            message.contains("have been discarded") -> R.string.vm_changes_discarded
            message.contains("not staged or locked") -> R.string.vm_discard_error_hint

            // Clean messages
            message == "No untracked files to clean" -> R.string.vm_no_untracked_files
            message.contains("Failed to clean") -> R.string.vm_clean_failed
            message.contains("Failed to get untracked files") -> R.string.vm_clean_get_files_failed
            message.contains("Failed to get file list") -> R.string.vm_clean_get_list_failed

            // Generic unknown error
            message == "Unknown error" -> R.string.vm_unknown_error

            // Fallback: return a generic error resource
            else -> R.string.vm_unknown_error
        }
    }
}
