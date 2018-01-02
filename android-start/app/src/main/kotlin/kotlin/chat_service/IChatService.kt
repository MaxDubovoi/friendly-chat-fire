package kotlin.chat_service

import com.google.android.gms.common.api.GoogleApiClient

/**
 * Created by maxim on 12/20/2017.
 */
interface IChatService {
    fun signOut()
    fun sendMessage()
    fun uploadImage()
    fun changeSettings()
    fun inviteFriend()
    fun signInWithGoogle(idToken: String, connectionFailedListener: GoogleApiClient.OnConnectionFailedListener): GoogleApiClient
}