<template>
  <form class="result-form" @submit.prevent="handleSubmit">
    <section class="section">
      <h2 class="section__title">収入情報</h2>
      <div class="grid">
        <label class="field">
          <span class="field__label">ベース収入</span>
          <input
            type="number"
            min="0"
            class="field__input"
            v-model.number="form.baseIncome"
          />
        </label>
        <label class="field">
          <span class="field__label">チップ枚数</span>
          <input
            type="number"
            min="0"
            class="field__input"
            v-model.number="form.tipCount"
          />
        </label>
        <label class="field">
          <span class="field__label">チップ収入</span>
          <input
            type="number"
            min="0"
            class="field__input"
            v-model.number="form.tipIncome"
          />
        </label>
        <label class="field">
          <span class="field__label">その他収入</span>
          <input
            type="number"
            min="0"
            class="field__input"
            v-model.number="form.otherIncome"
          />
        </label>
      </div>
    </section>

    <section class="section section--total">
      <span>合計収入</span>
      <strong>{{ currency(totalIncome) }}</strong>
    </section>

    <button type="submit" class="submit">登録する</button>
  </form>
</template>

<script setup lang="ts">
import { computed, reactive } from 'vue'
import { useFetch } from '@/composables/useFetch'

interface GameResultForm {
  playedAt: string
  place: number
  gameType: 'SANMA' | 'YONMA'
  baseIncome: number
  tipCount: number
  tipIncome: number
  otherIncome: number
  note: string
}

const props = defineProps<{
  userId: number
}>()

const form = reactive<GameResultForm>({
  playedAt: new Date().toISOString().slice(0, 10),
  place: 1,
  gameType: 'SANMA',
  baseIncome: 0,
  tipCount: 0,
  tipIncome: 0,
  otherIncome: 0,
  note: ''
})

const totalIncome = computed(
  () => form.baseIncome + form.tipIncome + form.otherIncome
)

const { request, loading } = useFetch()

const handleSubmit = async () => {
  await request(`/users/${props.userId}/results`, {
    method: 'POST',
    body: {
      gameType: form.gameType,
      playedAt: form.playedAt,
      place: form.place,
      baseIncome: form.baseIncome,
      tipCount: form.tipCount,
      tipIncome: form.tipIncome,
      otherIncome: form.otherIncome,
      totalIncome: totalIncome.value,
      note: form.note
    }
  })
}

const currency = (value: number) =>
  Intl.NumberFormat('ja-JP', { style: 'currency', currency: 'JPY' }).format(
    value || 0
  )
</script>

<style scoped>
.result-form {
  display: flex;
  flex-direction: column;
  gap: 1.5rem;
}

.section {
  background: #fff;
  border-radius: 0.75rem;
  padding: 1.25rem;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
}

.section__title {
  font-size: 1.1rem;
  margin-bottom: 1rem;
}

.grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(180px, 1fr));
  gap: 0.75rem 1rem;
}

.field {
  display: flex;
  flex-direction: column;
  gap: 0.25rem;
  font-size: 0.9rem;
}

.field__input {
  border: 1px solid #d5dbe0;
  border-radius: 0.4rem;
  padding: 0.55rem 0.65rem;
  font-size: 1rem;
}

.section--total {
  display: flex;
  align-items: center;
  justify-content: space-between;
  font-size: 1rem;
}

.section--total strong {
  font-size: 1.4rem;
  color: #0f6df2;
}

.submit {
  align-self: flex-end;
  border: none;
  border-radius: 999px;
  padding: 0.65rem 1.5rem;
  font-size: 0.95rem;
  font-weight: 600;
  color: #fff;
  background: linear-gradient(120deg, #0f6df2, #5f9bff);
  cursor: pointer;
  transition: opacity 0.2s ease;
}

.submit:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}
</style>
