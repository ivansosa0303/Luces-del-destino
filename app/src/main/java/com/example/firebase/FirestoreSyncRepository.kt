package com.example.firebase

import android.content.Context
import com.example.R
import com.example.data.model.CompilationRecordEntity
import com.example.data.model.DocumentItemEntity
import com.example.data.model.ProjectEntity
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class FirestoreSyncRepository(context: Context) {

    private val db: FirebaseFirestore = FirebaseFirestore.getInstance(
        context.applicationContext.getString(R.string.firestore_database_id)
    )

    private val auth = Firebase.auth

    private fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("Se requiere iniciar sesión con Google para sincronizar con Firestore.")
    }

    suspend fun syncProjectToCloud(project: ProjectEntity) {
        val uid = requireUserId()
        val docPath = "users/$uid/projects/proj_${project.id}"
        val docRef = db.collection("users").document(uid).collection("projects").document("proj_${project.id}")

        try {
            val payload = mapOf(
                "userId" to uid,
                "name" to project.name,
                "description" to project.description,
                "colorHex" to project.colorHex,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            docRef.set(payload).await()
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docPath)
            throw e
        }
    }

    suspend fun syncDocumentToCloud(doc: DocumentItemEntity) {
        val uid = requireUserId()
        val docPath = "users/$uid/documents/doc_${doc.id}"
        val docRef = db.collection("users").document(uid).collection("documents").document("doc_${doc.id}")

        try {
            val payload = mapOf(
                "userId" to uid,
                "projectId" to "proj_${doc.projectId}",
                "title" to doc.title,
                "fileName" to doc.fileName,
                "extension" to doc.extension,
                "mimeType" to doc.mimeType,
                "fileSizeBytes" to doc.fileSizeBytes,
                "sha256Checksum" to doc.sha256Checksum,
                "textSummaryOrPreview" to doc.textSummaryOrPreview.take(2000),
                "orderIndex" to doc.orderIndex,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            docRef.set(payload).await()
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docPath)
            throw e
        }
    }

    suspend fun syncCompilationToCloud(record: CompilationRecordEntity) {
        val uid = requireUserId()
        val docPath = "users/$uid/compilations/comp_${record.id}"
        val docRef = db.collection("users").document(uid).collection("compilations").document("comp_${record.id}")

        try {
            val payload = mapOf(
                "userId" to uid,
                "projectId" to (record.projectId?.let { "proj_$it" } ?: ""),
                "projectName" to record.projectName,
                "title" to record.title,
                "outputFileName" to record.outputFileName,
                "outputFormat" to record.outputFormat,
                "fileSizeBytes" to record.fileSizeBytes,
                "sha256Checksum" to record.sha256Checksum,
                "sourceItemCount" to record.sourceItemCount,
                "createdAt" to FieldValue.serverTimestamp()
            )
            docRef.set(payload).await()
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docPath)
            throw e
        }
    }

    suspend fun syncAllToCloud(
        projects: List<ProjectEntity>,
        documents: List<DocumentItemEntity>,
        compilations: List<CompilationRecordEntity>
    ): Int {
        var count = 0
        for (p in projects) {
            syncProjectToCloud(p)
            count++
        }
        for (d in documents) {
            syncDocumentToCloud(d)
            count++
        }
        for (c in compilations) {
            syncCompilationToCloud(c)
            count++
        }
        return count
    }
}
