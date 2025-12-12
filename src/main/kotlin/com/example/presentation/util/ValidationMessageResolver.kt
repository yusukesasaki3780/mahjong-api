package com.example.presentation.util

import com.example.common.error.FieldError
import com.example.presentation.dto.ValidationMessageItem
import org.valiktor.ConstraintViolation
import org.valiktor.constraints.Email
import org.valiktor.constraints.Greater
import org.valiktor.constraints.GreaterOrEqual
import org.valiktor.constraints.Less
import org.valiktor.constraints.LessOrEqual
import org.valiktor.constraints.NotBlank
import org.valiktor.constraints.NotEmpty
import org.valiktor.constraints.NotNull
import org.valiktor.constraints.Size

/**
 * バリデーションエラーを日本語メッセージへ整形するヘルパー。
 */
object ValidationMessageResolver {

    private const val DEFAULT_MESSAGE = "入力内容に誤りがあります。"

    private val fieldLabels = mapOf(
        "name" to "氏名",
        "nickname" to "ニックネーム",
        "storeName" to "店舗名",
        "storeId" to "店舗",
        "prefectureCode" to "都道府県",
        "email" to "メールアドレス",
        "password" to "パスワード",
        "passwordConfirm" to "確認用パスワード",
        "workDate" to "勤務日",
        "startTime" to "開始時刻",
        "endTime" to "終了時刻",
        "start" to "開始日",
        "end" to "終了日",
        "yearMonth" to "年月",
        "breaks" to "休憩",
        "memo" to "メモ",
        "yonmaGameFee" to "四麻ゲーム代",
        "sanmaGameFee" to "三麻ゲーム代",
        "sanmaGameFeeBack" to "三麻ゲーム代バック",
        "yonmaTipUnit" to "四麻チップ単価",
        "sanmaTipUnit" to "三麻チップ単価",
        "wageType" to "賃金体系",
        "hourlyWage" to "時給",
        "fixedSalary" to "固定給",
        "nightRateMultiplier" to "深夜割増率",
        "baseMinWage" to "最低賃金",
        "incomeTaxRate" to "所得税率",
        "transportPerShift" to "交通費",
        "tipCount" to "チップ枚数",
        "tipIncome" to "チップ収入",
        "otherIncome" to "その他収入",
        "totalIncome" to "総収入",
        "place" to "着順",
        "playedAt" to "対局日",
        "gameType" to "ゲーム種別"
    )

    private val codeMessages = mapOf(
        "INVALID_USER_ID" to "ユーザーIDの指定が正しくありません。",
        "INVALID_YEAR_MONTH" to "年月は YYYY-MM 形式で入力してください。",
        "REQUIRED" to "必須項目が入力されていません。",
        "REQUIRED_FOR_BREAKS" to "休憩を更新する場合は勤務日と開始・終了時刻が必要です。",
        "INVALID_RANGE_TYPE" to "表示期間の指定が不正です。",
        "INVALID_GAME_TYPE" to "ゲーム種別の指定が不正です。",
        "INVALID_DATETIME" to "日付は ISO8601 形式で入力してください。",
        "INVALID_DATE" to "日付は YYYY-MM-DD 形式で入力してください。",
        "INVALID_DATE_RANGE" to "終了日は開始日以降で指定してください。",
        "INVALID_TIME_FORMAT" to "時刻は HH:mm 形式で入力してください。",
        "INVALID_SHIFT_ID" to "シフトIDの指定が正しくありません。",
        "INVALID_RESULT_ID" to "成績IDの指定が正しくありません。",
        "BOTH_REQUIRED" to "開始時刻と終了時刻を両方指定してください。",
        "INVALID_RANGE" to "期間の指定が不正です。",
        "BREAK_TIME_REQUIRED" to "休憩時間を入力してください。",
        "UNKNOWN_BREAK" to "指定された休憩が見つかりません。",
        "INVALID_PLAYED_AT" to "対局日は YYYY-MM-DD 形式で入力してください。",
        "PLACE_OUT_OF_RANGE" to "着順は1〜4の範囲で入力してください。",
        "INCOMPLETE_INCOME_SET" to "収入関連の値が不足しています。",
        "EMAIL_ALREADY_EXISTS" to "このメールアドレスは既に使用されています。",
        "PASSWORD_NOT_MATCH" to "確認用パスワードが一致しません。",
        "WEAK_PASSWORD" to "パスワードは8文字以上で、英大文字・英小文字・数字・記号のうち3種類以上を含めてください。",
        "TOTAL_INCOME_MISMATCH" to "総収入の計算結果が一致しません。",
        "STORE_NOT_FOUND" to "選択した店舗が存在しません。",
        "SHIFT_OVERLAP" to "シフト時間帯が既存のシフトと重複しています。",
        "INVALID_BREAK_RANGE" to "休憩終了時刻は開始時刻より後にしてください。",
        "BREAK_BEFORE_SHIFT" to "休憩開始時刻は勤務開始時刻以降に設定してください。",
        "BREAK_AFTER_SHIFT" to "休憩終了時刻は勤務終了時刻以内で指定してください。",
        "BREAK_OVERLAP" to "休憩時間が互いに重複しています。",
        "BREAK_OUTSIDE" to "休憩時間が勤務時間外です。",
        "ID_REQUIRED_FOR_DELETE" to "削除対象の休憩IDが必要です。",
        "FIXED_WAGE_REQUIRED" to "賃金体系が固定給の場合、固定給を入力してください。"
    )

    fun defaultMessage(): String = DEFAULT_MESSAGE

    fun fromConstraint(violation: ConstraintViolation): ValidationMessageItem =
        ValidationMessageItem(
            field = violation.property,
            message = constraintMessage(violation)
        )

    fun fromFieldErrors(errors: List<FieldError>): List<ValidationMessageItem> =
        errors.map {
            ValidationMessageItem(
                field = it.field,
                message = fieldErrorMessage(it)
            )
        }

    private fun constraintMessage(violation: ConstraintViolation): String {
        val label = labelFor(violation.property)
        val constraint = violation.constraint
        return when (constraint) {
            is NotBlank, is NotEmpty, is NotNull -> "${label}は必須項目です。"
            is Size -> sizeMessage(label, constraint)
            is Email -> "${label}はメールアドレス形式で入力してください。"
            is Greater<*> -> "${label}は${constraint.value}より大きい値で入力してください。"
            is GreaterOrEqual<*> -> "${label}は${constraint.value}以上で入力してください。"
            is Less<*> -> "${label}は${constraint.value}より小さい値で入力してください。"
            is LessOrEqual<*> -> "${label}は${constraint.value}以下で入力してください。"
            else -> when (constraint.name) {
                "Positive" -> "${label}は正の数値で入力してください。"
                "PositiveOrZero" -> "${label}は0以上の値で入力してください。"
                "NonNegative" -> "${label}は0以上の値で入力してください。"
                "NightRate" -> "${label}は1.0以上の値で入力してください。"
                "TaxRate" -> "${label}は0以上1以下で入力してください。"
                else -> "${label}の入力内容を確認してください。"
            }
        }
    }

    private fun sizeMessage(label: String, constraint: Size): String = when {
        constraint.min > 0 && constraint.max < Int.MAX_VALUE ->
            "${label}は${constraint.min}〜${constraint.max}文字以内で入力してください。"
        constraint.min > 0 ->
            "${label}は${constraint.min}文字以上で入力してください。"
        constraint.max < Int.MAX_VALUE ->
            "${label}は${constraint.max}文字以内で入力してください。"
        else -> "${label}の長さを確認してください。"
    }

    private fun fieldErrorMessage(error: FieldError): String {
        codeMessages[error.code]?.let { return it }
        val sanitized = error.message?.takeIf { it.isNotBlank() && !it.contains('�') }
        return sanitized ?: "${labelFor(error.field)}の入力内容を確認してください。"
    }

    private fun labelFor(property: String?): String {
        val key = property
            ?.substringAfterLast(".")
            ?.replace(Regex("\\[.+?]"), "")
        return fieldLabels[key] ?: key ?: "この項目"
    }
}
