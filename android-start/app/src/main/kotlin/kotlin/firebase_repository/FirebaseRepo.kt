package kotlin.firebase_repository

import android.content.ContentValues.TAG
import android.net.Uri
import android.support.v4.app.FragmentActivity
import android.util.Log
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.tasks.OnCompleteListener
import com.google.firebase.appindexing.Action
import com.google.firebase.appindexing.Indexable
import com.google.firebase.auth.AuthResult
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.storage.StorageReference

/**
 * Created by maxim on 12/20/2017.
 */
class FirebaseRepo(private val activityContext: FragmentActivity) : IFirebaseRepo {
    override fun putImageInStorage(storageReference: StorageReference, uri: Uri?, key: String) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getMessageIndexable(friendlyMessage: FriendlyMessage): Indexable {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getMessageViewAction(friendlyMessage: FriendlyMessage): Action {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun applyRetrievedLengthLimit() {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun sendInvitation() {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun causeCrash() {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    private val mFirebaseAuth = FirebaseAuth.getInstance()

    override fun firebaseAuthWithGoogle(acct: GoogleSignInAccount?, onCompleteListener: OnCompleteListener<AuthResult>) {
        Log.d(TAG, "firebaseAuthWithGooogle:" + acct!!.id!!)
        val credential = GoogleAuthProvider.getCredential(acct.idToken, null)
        mFirebaseAuth.signInWithCredential(credential)
                .addOnCompleteListener(onCompleteListener)
    }
}