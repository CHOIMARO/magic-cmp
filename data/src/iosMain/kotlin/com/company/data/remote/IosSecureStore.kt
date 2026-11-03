package com.company.data.remote

import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.alloc
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.value
import platform.CoreFoundation.CFDictionaryAddValue
import platform.CoreFoundation.CFDictionaryCreateMutable
import platform.CoreFoundation.CFDictionaryRef
import platform.CoreFoundation.CFRelease
import platform.CoreFoundation.CFStringRef
import platform.CoreFoundation.CFTypeRef
import platform.CoreFoundation.CFTypeRefVar
import platform.CoreFoundation.kCFBooleanTrue
import platform.CoreFoundation.kCFTypeDictionaryKeyCallBacks
import platform.CoreFoundation.kCFTypeDictionaryValueCallBacks
import platform.Foundation.CFBridgingRelease
import platform.Foundation.CFBridgingRetain
import platform.Foundation.NSData
import platform.Foundation.NSString
import platform.Foundation.NSUTF8StringEncoding
import platform.Foundation.NSUserDefaults
import platform.Foundation.create
import platform.Foundation.dataUsingEncoding
import platform.Security.SecItemAdd
import platform.Security.SecItemCopyMatching
import platform.Security.SecItemDelete
import platform.Security.errSecSuccess
import platform.Security.kSecAttrAccessible
import platform.Security.kSecAttrAccessibleAfterFirstUnlockThisDeviceOnly
import platform.Security.kSecAttrAccount
import platform.Security.kSecAttrService
import platform.Security.kSecClass
import platform.Security.kSecClassGenericPassword
import platform.Security.kSecMatchLimit
import platform.Security.kSecMatchLimitOne
import platform.Security.kSecReturnData
import platform.Security.kSecValueData

/**
 * Keeps values in the Keychain as generic passwords. The values stay on this device only.
 *
 * The Keychain keeps the values after the user deletes the app. So on the first start after an installation, the
 * store removes the old values.
 */
@OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
internal class IosSecureStore : SecureStore {

    init {
        // 앱을 지우고 다시 설치하면 이전 계정으로 로그인된 채 시작하지 않게 한다.
        val defaults = NSUserDefaults.standardUserDefaults
        if (!defaults.boolForKey(KEY_INSTALLED)) {
            withQuery(account = null) { SecItemDelete(it) }
            defaults.setBool(true, forKey = KEY_INSTALLED)
        }
    }

    override fun read(key: String): String? = memScoped {
        val result = alloc<CFTypeRefVar>()
        val status = withQuery(key, kSecReturnData to kCFBooleanTrue, kSecMatchLimit to kSecMatchLimitOne) {
            SecItemCopyMatching(it, result.ptr)
        }
        if (status != errSecSuccess) return@memScoped null
        val data = CFBridgingRelease(result.value) as? NSData ?: return@memScoped null
        NSString.create(data = data, encoding = NSUTF8StringEncoding)?.toString()
    }

    override fun write(key: String, value: String) {
        val data = NSString.create(string = value).dataUsingEncoding(NSUTF8StringEncoding) ?: return
        remove(key)
        val dataRef = CFBridgingRetain(data)
        try {
            withQuery(
                key,
                kSecValueData to dataRef,
                kSecAttrAccessible to kSecAttrAccessibleAfterFirstUnlockThisDeviceOnly,
            ) { SecItemAdd(it, null) }
        } finally {
            CFBridgingRelease(dataRef)
        }
    }

    override fun remove(key: String) {
        withQuery(key) { SecItemDelete(it) }
    }

    /**
     * Makes a Keychain query for the service of the app and gives it to [block].
     *
     * @param account Name of the value, or null for all values of the service.
     * @param extra More attributes of the query.
     */
    private fun <T> withQuery(
        account: String?,
        vararg extra: Pair<CFStringRef?, CFTypeRef?>,
        block: (CFDictionaryRef?) -> T,
    ): T {
        val serviceRef = CFBridgingRetain(SERVICE)
        val accountRef = account?.let { CFBridgingRetain(it) }
        val query = CFDictionaryCreateMutable(null, 0, kCFTypeDictionaryKeyCallBacks.ptr, kCFTypeDictionaryValueCallBacks.ptr)
        try {
            CFDictionaryAddValue(query, kSecClass, kSecClassGenericPassword)
            CFDictionaryAddValue(query, kSecAttrService, serviceRef)
            if (accountRef != null) CFDictionaryAddValue(query, kSecAttrAccount, accountRef)
            extra.forEach { (key, value) -> CFDictionaryAddValue(query, key, value) }
            return block(query)
        } finally {
            CFRelease(query)
            CFBridgingRelease(serviceRef)
            accountRef?.let { CFBridgingRelease(it) }
        }
    }

    private companion object {
        const val SERVICE = "com.choimaro.magic.session"

        /** Key of NSUserDefaults. NSUserDefaults is removed with the app, so its absence marks a new installation. */
        const val KEY_INSTALLED = "session_store_installed"
    }
}
