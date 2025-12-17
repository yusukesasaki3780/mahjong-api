package com.example.infrastructure.db.tables

import com.example.domain.model.ShiftSlotType
import org.jetbrains.exposed.sql.ReferenceOption
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.kotlin.datetime.date
import org.jetbrains.exposed.sql.kotlin.datetime.timestamp

object ShiftRequirementsTable : Table("shift_requirements") {
    val id = long("id").autoIncrement()
    val storeId = reference(
        name = "store_id",
        refColumn = StoreMasterTable.id,
        onDelete = ReferenceOption.CASCADE,
        onUpdate = ReferenceOption.RESTRICT
    )
    val targetDate = date("target_date")
    val shiftType = enumerationByName("shift_type", length = 20, klass = ShiftSlotType::class)
    val startRequired = integer("start_required")
    val endRequired = integer("end_required")
    val createdAt = timestamp("created_at")
    val updatedAt = timestamp("updated_at")

    override val primaryKey: PrimaryKey = PrimaryKey(id)

    init {
        uniqueIndex(
            "uq_shift_requirements_store_date_type",
            storeId,
            targetDate,
            shiftType
        )
    }
}
