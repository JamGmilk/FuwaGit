package jamgmilk.fuwagit.domain.usecase.git

import jamgmilk.fuwagit.core.result.AppResult
import jamgmilk.fuwagit.core.result.AppException
import jamgmilk.fuwagit.core.util.UrlUtils
import javax.inject.Inject

class ValidateUrlUseCase @Inject constructor() {
    suspend operator fun invoke(url: String): AppResult<UrlUtils.UrlType> {
        if (url.isBlank()) {
            return AppResult.Error(AppException.Validation("URL cannot be empty"))
        }

        return when (val result = UrlUtils.validateGitUrl(url)) {
            is UrlUtils.ValidationResult.Valid -> AppResult.Success(result.type)
            is UrlUtils.ValidationResult.Invalid -> AppResult.Error(AppException.Validation(result.reason))
        }
    }

    fun validateUrlSync(url: String): AppResult<UrlUtils.UrlType> {
        if (url.isBlank()) {
            return AppResult.Error(AppException.Validation("URL cannot be empty"))
        }

        return when (val result = UrlUtils.validateGitUrl(url)) {
            is UrlUtils.ValidationResult.Valid -> AppResult.Success(result.type)
            is UrlUtils.ValidationResult.Invalid -> AppResult.Error(AppException.Validation(result.reason))
        }
    }

    fun getUrlType(url: String): UrlUtils.UrlType {
        return UrlUtils.getUrlType(url)
    }

    fun extractHost(url: String): String? {
        return UrlUtils.extractHost(url)
    }

    fun formatUrlForDisplay(url: String): String {
        return UrlUtils.formatUrlForDisplay(url)
    }

    fun isValid(url: String): Boolean {
        return UrlUtils.validateGitUrl(url) is UrlUtils.ValidationResult.Valid
    }
}