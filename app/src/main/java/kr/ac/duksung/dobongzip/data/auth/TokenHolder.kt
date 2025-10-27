// kr/ac/duksung/dobongzip/data/auth/TokenHolder.kt
package kr.ac.duksung.dobongzip.data.auth

object TokenHolder {
    @Volatile
    var accessToken: String? = null
}
