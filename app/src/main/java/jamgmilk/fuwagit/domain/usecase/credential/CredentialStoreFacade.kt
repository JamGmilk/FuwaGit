package jamgmilk.fuwagit.domain.usecase.credential

import androidx.fragment.app.FragmentActivity
import jamgmilk.fuwagit.core.result.AppResult
import jamgmilk.fuwagit.domain.model.credential.HttpsCredential
import jamgmilk.fuwagit.domain.model.credential.SshKey
import javax.inject.Inject

class CredentialStoreFacade @Inject constructor(
    private val sessionFacade: CredentialSessionFacade,
    private val masterPasswordFacade: MasterPasswordFacade,
    private val biometricAuthFacade: BiometricAuthFacade,
    private val credentialRepository: jamgmilk.fuwagit.domain.repository.CredentialRepository
) {

    // ========== 会话状态管理 ==========

    fun isMasterPasswordSet(): Boolean = credentialRepository.isMasterPasswordSet()

    fun isBiometricEnabled(): Boolean = credentialRepository.isBiometricEnabled()

    fun isUnlocked(): Boolean = credentialRepository.isUnlocked()

    fun getMasterPasswordHint(): String? = credentialRepository.getMasterPasswordHint()

    fun lock() {
        credentialRepository.lock()
    }

    // ========== 密码管理 ==========

    suspend fun setupMasterPassword(
        password: String,
        confirmPassword: String,
        hint: String?
    ): AppResult<Unit> = masterPasswordFacade.setupMasterPassword(password, confirmPassword, hint)

    suspend fun unlockWithPassword(password: String): AppResult<Unit> =
        masterPasswordFacade.unlockWithPassword(password)

    suspend fun changeMasterPassword(
        oldPassword: String,
        newPassword: String,
        hint: String?
    ): AppResult<Unit> = masterPasswordFacade.changePassword(oldPassword, newPassword, hint)

    // ========== HTTPS 凭证管理 ==========

    suspend fun getHttpsCredentials(): AppResult<List<HttpsCredential>> =
        sessionFacade.getHttpsCredentials()

    suspend fun addHttpsCredential(host: String, username: String, password: String): AppResult<String> =
        sessionFacade.addHttpsCredential(host, username, password)

    suspend fun updateHttpsCredential(
        uuid: String,
        host: String?,
        username: String?,
        password: String?
    ): AppResult<Unit> = sessionFacade.updateHttpsCredential(uuid, host, username, password)

    suspend fun deleteHttpsCredential(uuid: String): AppResult<Unit> =
        sessionFacade.deleteHttpsCredential(uuid)

    suspend fun getHttpsPassword(uuid: String): AppResult<String> =
        sessionFacade.getHttpsPassword(uuid)

    // ========== SSH 密钥管理 ==========

    suspend fun getSshKeys(): AppResult<List<SshKey>> = sessionFacade.getSshKeys()

    suspend fun addSshKey(
        name: String,
        type: String,
        publicKey: String,
        privateKey: String,
        passphrase: String?,
        fingerprint: String
    ): AppResult<String> = sessionFacade.addSshKey(name, type, publicKey, privateKey, passphrase, fingerprint)

    suspend fun deleteSshKey(uuid: String): AppResult<Unit> = sessionFacade.deleteSshKey(uuid)

    suspend fun getSshPrivateKey(uuid: String): AppResult<String> = sessionFacade.getSshPrivateKey(uuid)

    // ========== 导入/导出 ==========

    suspend fun exportCredentials(): AppResult<String> = sessionFacade.exportCredentials()

    suspend fun importCredentials(jsonData: String): AppResult<Unit> = sessionFacade.importCredentials(jsonData)

    // ========== 生物识别认证 ==========

    suspend fun enableBiometric(
        activity: FragmentActivity,
        onResult: (AppResult<Unit>) -> Unit
    ) = biometricAuthFacade.enableBiometric(activity, onResult)

    fun unlockWithBiometric(
        activity: FragmentActivity,
        onResult: (AppResult<Unit>) -> Unit
    ) = biometricAuthFacade.unlockWithBiometric(activity, onResult)

    fun disableBiometric() {
        credentialRepository.disableBiometric()
    }
}

/**
 * Facade for credential session operations (HTTPS + SSH + Import/Export).
 * 整合 HTTPS、SSH 和导入/导出相关的用例。
 */
class CredentialSessionFacade @Inject constructor(
    private val getHttpsCredentialsUseCase: GetHttpsCredentialsUseCase,
    private val addHttpsCredentialUseCase: AddHttpsCredentialUseCase,
    private val updateHttpsCredentialUseCase: UpdateHttpsCredentialUseCase,
    private val deleteHttpsCredentialUseCase: DeleteHttpsCredentialUseCase,
    private val getHttpsPasswordUseCase: GetHttpsPasswordUseCase,
    private val getSshKeysUseCase: GetSshKeysUseCase,
    private val addSshKeyUseCase: AddSshKeyUseCase,
    private val deleteSshKeyUseCase: DeleteSshKeyUseCase,
    private val getSshPrivateKeyUseCase: GetSshPrivateKeyUseCase,
    private val exportCredentialsUseCase: ExportCredentialsUseCase,
    private val importCredentialsUseCase: ImportCredentialsUseCase
) {
    suspend fun getHttpsCredentials(): AppResult<List<HttpsCredential>> = getHttpsCredentialsUseCase()

    suspend fun addHttpsCredential(host: String, username: String, password: String): AppResult<String> =
        addHttpsCredentialUseCase(host, username, password)

    suspend fun updateHttpsCredential(
        uuid: String,
        host: String?,
        username: String?,
        password: String?
    ): AppResult<Unit> = updateHttpsCredentialUseCase(uuid, host, username, password)

    suspend fun deleteHttpsCredential(uuid: String): AppResult<Unit> = deleteHttpsCredentialUseCase(uuid)

    suspend fun getHttpsPassword(uuid: String): AppResult<String> = getHttpsPasswordUseCase(uuid)

    suspend fun getSshKeys(): AppResult<List<SshKey>> = getSshKeysUseCase()

    suspend fun addSshKey(
        name: String,
        type: String,
        publicKey: String,
        privateKey: String,
        passphrase: String?,
        fingerprint: String
    ): AppResult<String> = addSshKeyUseCase(name, type, publicKey, privateKey, passphrase, fingerprint)

    suspend fun deleteSshKey(uuid: String): AppResult<Unit> = deleteSshKeyUseCase(uuid)

    suspend fun getSshPrivateKey(uuid: String): AppResult<String> = getSshPrivateKeyUseCase(uuid)

    suspend fun exportCredentials(): AppResult<String> = exportCredentialsUseCase()

    suspend fun importCredentials(jsonData: String): AppResult<Unit> = importCredentialsUseCase(jsonData)
}

/**
 * Facade for master password operations.
 * 整合主密码设置、解锁和修改相关的用例。
 */
class MasterPasswordFacade @Inject constructor(
    private val setupMasterPasswordUseCase: SetupMasterPasswordUseCase,
    private val unlockWithPasswordUseCase: UnlockWithPasswordUseCase,
    private val masterKeyManager: jamgmilk.fuwagit.data.local.security.MasterKeyManager
) {
    suspend fun setupMasterPassword(
        password: String,
        confirmPassword: String,
        hint: String?
    ): AppResult<Unit> = setupMasterPasswordUseCase(password, confirmPassword, hint)

    suspend fun unlockWithPassword(password: String): AppResult<Unit> = unlockWithPasswordUseCase(password)

    suspend fun changePassword(
        oldPassword: String,
        newPassword: String,
        hint: String?
    ): AppResult<Unit> {
        return masterKeyManager.changeMasterPassword(oldPassword, newPassword)
            .onSuccess {
                if (hint != null) {
                    masterKeyManager.setPasswordHint(hint)
                }
            }
    }
}

/**
 * Facade for biometric authentication operations.
 * 整合生物识别认证相关的用例。
 */
class BiometricAuthFacade @Inject constructor(
    private val enableBiometricUseCase: EnableBiometricUseCase,
    private val unlockWithBiometricUseCase: UnlockWithBiometricUseCase
) {
    fun enableBiometric(
        activity: FragmentActivity,
        onResult: (AppResult<Unit>) -> Unit
    ) {
        enableBiometricUseCase(activity, onResult)
    }

    fun unlockWithBiometric(
        activity: FragmentActivity,
        onResult: (AppResult<Unit>) -> Unit
    ) {
        unlockWithBiometricUseCase(activity, onResult)
    }
}
