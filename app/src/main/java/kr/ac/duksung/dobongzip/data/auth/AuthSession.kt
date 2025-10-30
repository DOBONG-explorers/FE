// kr/ac/duksung/dobongzip/data/auth/AuthSession.kt
package kr.ac.duksung.dobongzip.data.auth

import java.util.concurrent.atomic.AtomicReference
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

object AuthSession {
    private val tokenRef = AtomicReference<String?>(null)

    private val _tokenFlow = MutableStateFlow<String?>(null)
    val tokenFlow = _tokenFlow.asStateFlow()

    fun setToken(token: String?) {
        tokenRef.set(token)
        _tokenFlow.value = token
    }

    fun getToken(): String? = tokenRef.get()

    fun clear() = setToken(null)
}
