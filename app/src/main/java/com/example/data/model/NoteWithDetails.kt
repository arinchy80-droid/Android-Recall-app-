package com.example.data.model

import androidx.room.Embedded
import androidx.room.Junction
import androidx.room.Relation
import com.example.data.local.entity.ActionItemEntity
import com.example.data.local.entity.CommitmentEntity
import com.example.data.local.entity.MediaEntity
import com.example.data.local.entity.NoteEntity
import com.example.data.local.entity.NotePersonCrossRef
import com.example.data.local.entity.NoteRevisionEntity
import com.example.data.local.entity.PersonEntity

data class NoteWithDetails(
    @Embedded
    val note: NoteEntity,

    @Relation(
        parentColumn = "id",
        entityColumn = "id",
        associateBy = Junction(
            value = NotePersonCrossRef::class,
            parentColumn = "noteId",
            entityColumn = "personId"
        )
    )
    val people: List<PersonEntity> = emptyList(),

    @Relation(
        parentColumn = "id",
        entityColumn = "noteId"
    )
    val media: List<MediaEntity> = emptyList(),

    @Relation(
        parentColumn = "id",
        entityColumn = "noteId"
    )
    val actionItems: List<ActionItemEntity> = emptyList(),

    @Relation(
        parentColumn = "id",
        entityColumn = "noteId"
    )
    val revisions: List<NoteRevisionEntity> = emptyList(),

    @Relation(
        parentColumn = "id",
        entityColumn = "noteId"
    )
    val commitments: List<CommitmentEntity> = emptyList()
)
