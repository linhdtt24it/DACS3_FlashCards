package com.example.flashcards.repository

import android.util.Log
import com.example.flashcards.model.Folder
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class FolderRepository {
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val currentUserId get() = auth.currentUser?.uid ?: ""

    private val foldersCollection get() =
        db.collection("users").document(currentUserId).collection("folders")

    fun getFolders(): Flow<List<Folder>> = callbackFlow {
        if (currentUserId.isEmpty()) { trySend(emptyList()); return@callbackFlow }
        val listener = foldersCollection
            .orderBy("createdAt", com.google.firebase.firestore.Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) { close(error); return@addSnapshotListener }
                trySend(snapshot?.toObjects(Folder::class.java) ?: emptyList())
            }
        awaitClose { listener.remove() }
    }

    suspend fun saveFolder(folder: Folder) {
        if (currentUserId.isEmpty()) return
        try {
            foldersCollection.document(folder.id).set(folder).await()
        } catch (e: Exception) {
            Log.e("FolderRepository", "Error saving folder", e)
        }
    }

    suspend fun deleteFolder(folderId: String) {
        if (currentUserId.isEmpty()) return
        try {
            foldersCollection.document(folderId).delete().await()
        } catch (e: Exception) {
            Log.e("FolderRepository", "Error deleting folder", e)
        }
    }

    suspend fun addSetToFolder(folderId: String, setId: String) {
        if (currentUserId.isEmpty()) return
        try {
            foldersCollection.document(folderId)
                .update("setIds", com.google.firebase.firestore.FieldValue.arrayUnion(setId))
                .await()
        } catch (e: Exception) {
            Log.e("FolderRepository", "Error adding set to folder", e)
        }
    }

    suspend fun addSetsToFolder(folderId: String, setIds: List<String>) {
        if (currentUserId.isEmpty()) return
        try {
            foldersCollection.document(folderId)
                .update(
                    "setIds", com.google.firebase.firestore.FieldValue.arrayUnion(*setIds.toTypedArray()),
                    "studySetIds", com.google.firebase.firestore.FieldValue.arrayUnion(*setIds.toTypedArray())
                )
                .await()
        } catch (e: Exception) {
            Log.e("FolderRepository", "Error adding sets to folder", e)
            throw e
        }
    }

    suspend fun removeSetFromFolder(folderId: String, setId: String) {
        if (currentUserId.isEmpty()) return
        try {
            foldersCollection.document(folderId)
                .update("setIds", com.google.firebase.firestore.FieldValue.arrayRemove(setId))
                .await()
        } catch (e: Exception) {
            Log.e("FolderRepository", "Error removing set from folder", e)
        }
    }
}
