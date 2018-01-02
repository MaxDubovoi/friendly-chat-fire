package kotlin.firebase_repository

import android.net.Uri
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.tasks.OnCompleteListener
import com.google.firebase.appindexing.Action
import com.google.firebase.appindexing.Indexable
import com.google.firebase.auth.AuthResult
import com.google.firebase.storage.StorageReference

/**
 * Created by maxim on 12/20/2017.
 */
interface IFirebaseRepo {
    fun firebaseAuthWithGoogle(acct: GoogleSignInAccount?, onCompleteListener: OnCompleteListener<AuthResult>)
    fun putImageInStorage(storageReference: StorageReference, uri: Uri?, key: String)
    fun getMessageIndexable(friendlyMessage: FriendlyMessage): Indexable
    fun getMessageViewAction(friendlyMessage: FriendlyMessage): Action
    fun applyRetrievedLengthLimit()
    fun sendInvitation()
    fun causeCrash()

}