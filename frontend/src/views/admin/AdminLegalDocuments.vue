<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { ApiError, apiGet, apiPost } from '../../api/http'
import type { AdminLegalDocument } from '../../api/contracts'
import AdminLayout from '../../components/AdminLayout.vue'
import { i18n } from '../../i18n'

const rows = ref<AdminLegalDocument[]>([])
const loading = ref(true)
const busy = ref('')
const error = ref('')
const notice = ref('')

onMounted(loadDocuments)

async function loadDocuments() {
  loading.value = true
  error.value = ''
  try {
    rows.value = await apiGet<AdminLegalDocument[]>('/admin/legal-documents')
  } catch (err) {
    error.value = messageFrom(err)
  } finally {
    loading.value = false
  }
}

async function createPrivacyDraft() {
  busy.value = 'create'
  error.value = ''
  notice.value = ''
  try {
    const created = await apiPost<AdminLegalDocument>('/admin/legal-documents', {
      documentType: 'privacy',
      version: 'privacy-v2',
      title: 'Privacy Policy v2',
      contentUrl: '/legal/privacy-v2',
      legalApprovalId: 'LEGAL-V2',
    })
    rows.value = [created, ...rows.value.filter((row) => row.documentType !== created.documentType || row.version !== created.version)]
    notice.value = i18n.t('admin.legalDocumentDraftCreated', { version: created.version })
  } catch (err) {
    error.value = messageFrom(err)
  } finally {
    busy.value = ''
  }
}

async function publish(row: AdminLegalDocument) {
  busy.value = key(row)
  error.value = ''
  notice.value = ''
  try {
    const updated = await apiPost<AdminLegalDocument>(`/admin/legal-documents/${row.documentType}/${row.version}/publish`)
    rows.value = rows.value.map((item) => item.documentType === updated.documentType
      ? { ...item, status: item.version === updated.version ? updated.status : 'archived' }
      : item)
    notice.value = i18n.t('admin.legalDocumentPublished', { version: updated.version })
  } catch (err) {
    error.value = messageFrom(err)
  } finally {
    busy.value = ''
  }
}

function key(row: AdminLegalDocument) {
  return `${row.documentType}-${row.version}`
}

function versionKey(row: AdminLegalDocument) {
  return row.version
}

function messageFrom(err: unknown) {
  if (err instanceof ApiError || err instanceof Error) return err.message
  return i18n.t('admin.legalDocumentRequestFailed')
}
</script>

<template>
  <AdminLayout>
    <header class="admin-header">
      <div>
        <p class="eyebrow">{{ $t('admin.legalConfiguration') }}</p>
        <h1>{{ $t('admin.legalDocs') }}</h1>
      </div>
      <button data-test="create-legal-document" :disabled="busy === 'create'" @click="createPrivacyDraft">{{ $t('admin.createPrivacyV2') }}</button>
    </header>

    <section v-if="loading" class="status-panel">{{ $t('admin.loadingLegalDocuments') }}</section>
    <section v-else-if="error && !rows.length" class="status-panel danger">{{ error }}</section>

    <template v-else>
      <p v-if="notice" class="notice success">{{ notice }}</p>
      <p v-if="error" class="notice danger">{{ error }}</p>
      <div class="table-wrap">
        <table>
          <thead>
            <tr>
              <th>{{ $t('common.document') }}</th>
              <th>{{ $t('common.version') }}</th>
              <th>{{ $t('common.status') }}</th>
              <th>{{ $t('admin.url') }}</th>
              <th>{{ $t('admin.legalApproval') }}</th>
              <th>{{ $t('common.action') }}</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="row in rows" :key="key(row)">
              <td><strong>{{ row.title }}</strong><span>{{ row.documentType }}</span></td>
              <td>{{ row.version }}</td>
              <td><span class="status-tag" :class="{ active: row.status === 'active', pending: row.status === 'draft' }">{{ row.status }}</span></td>
              <td><code>{{ row.contentUrl }}</code></td>
              <td>{{ row.legalApprovalId ?? '-' }}</td>
              <td>
                <button :data-test="`publish-${versionKey(row)}`" :disabled="row.status === 'active' || busy === key(row)" @click="publish(row)">
                  {{ $t('admin.publish') }}
                </button>
              </td>
            </tr>
            <tr v-if="!rows.length"><td colspan="6">{{ $t('admin.noLegalDocuments') }}</td></tr>
          </tbody>
        </table>
      </div>
    </template>
  </AdminLayout>
</template>
