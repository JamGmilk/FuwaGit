package jamgmilk.fuwagit.domain.usecase.credential

import androidx.fragment.app.FragmentActivity
import jamgmilk.fuwagit.core.result.AppResult
import jamgmilk.fuwagit.domain.model.credential.HttpsCredential
import jamgmilk.fuwagit.domain.model.credential.SshKey
import jamgmilk.fuwagit.domain.repository.BiometricRepository
import jamgmilk.fuwagit.domain.repository.CredentialRepository
import javax.inject.Inject

class CredentialStoreFacade @Inject constructor(
    private val credentialRepository: CredentialRepository,
    private val biometricRepository: BiometricRepository,
    private val enableBiometricUseCase: EnableBiometricUseCase,
    private val unlockWithBiometricUseCase: UnlockWithBiometricUseCase
) {
    suspend fun enableBiometric(
        activity: FragmentActivity,
        title: String,
        subtitle: String,
        negativeButtonText: String
    ): AppResult<Unit> {
        return enableBiometricUseCase(activity, title, subtitle, negativeButtonText)
    }

    suspend fun unlockWithBiometric(
        activity: FragmentActivity,
        title: String,
        subtitle: String,
        negativeButtonText: String
    ): AppResult<Unit> {
        return unlockWithBiometricUseCase(activity, title, subtitle, negativeButtonText)
    }

    suspend fun setupMasterPassword(password: String, hint: String?): AppResult<Unit> {
        return credentialRepository.setupMasterPassword(password, hint)
    }

    suspend fun changeMasterPassword(oldPassword: String, newPassword: String, hint: String?): AppResult<Unit> {
        return credentialRepository.changeMasterPassword(oldPassword, newPassword, hint)
    }

    fun isMasterPasswordSet(): Boolean {
        return credentialRepository.isMasterPasswordSet()
    }

    fun isBiometricEnabled(): Boolean {
        return biometricRepository.isBiometricEnabled()
    }

    fun getMasterPasswordHint(): String? {
        return credentialRepository.getMasterPasswordHint()
    }

    suspend fun disableBiometric(): AppResult<Unit> {
        return biometricRepository.disableBiometric()
    }

    suspend fun unlockWithPassword(password: String): AppResult<Unit> {
        return credentialRepository.unlockWithPassword(password)
    }

    suspend fun isUnlocked(): Boolean {
        return credentialRepository.isUnlocked()
    }

    fun lock() {
        credentialRepository.lock()
    }

    suspend fun getHttpsCredentials(): AppResult<List<HttpsCredential>> {
        return credentialRepository.getAllHttpsCredentials()
    }

    suspend fun addHttpsCredential(host: String, username: String, password: String): AppResult<Unit> {
        return credentialRepository.addHttpsCredential(host, username, password).map { }
    }

    suspend fun deleteHttpsCredential(uuid: String): AppResult<Unit> {
        return credentialRepository.deleteHttpsCredential(uuid)
    }

    suspend fun getSshKeys(): AppResult<List<SshKey>> {
        return credentialRepository.getAllSshKeys()
    }

    suspend fun addSshKey(name: String, type: String, publicKey: String, privateKey: String, passphrase: String?, fingerprint: String): AppResult<Unit> {
        return credentialRepository.addSshKey(name, type, publicKey, privateKey, passphrase, fingerprint).map { }
    }

    suspend fun deleteSshKey(uuid: String): AppResult<Unit> {
        return credentialRepository.deleteSshKey(uuid)
    }

    suspend fun exportCredentials(): AppResult<String> {
        return credentialRepository.exportCredentials()
    }

    suspend fun importCredentials(jsonData: String): AppResult<Unit> {
        return credentialRepository.importCredentials(jsonData)
    }

    suspend fun getHttpsPassword(uuid: String): AppResult<String> {
        return credentialRepository.getHttpsPassword(uuid)
    }

    suspend fun getSshPrivateKey(uuid: String): AppResult<String> {
        return credentialRepository.getSshPrivateKey(uuid)
    }

    suspend fun getSshPassphrase(uuid: String): AppResult<String?> {
        return credentialRepository.getSshPassphrase(uuid)
    }
}
