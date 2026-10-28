<template>
  <div class="comments app-card">
    <h3 class="comments-title">
      <el-icon><ChatDotRound /></el-icon>
      留言咨询
      <span v-if="total" class="count">{{ total }}</span>
    </h3>

    <!-- ============ 发表留言 ============ -->
    <div class="editor">
      <div v-if="!userStore.isLogin" class="tip-row">
        <span class="text-secondary">登录后才能留言咨询</span>
        <el-button type="primary" size="small" @click="goLogin">去登录</el-button>
      </div>

      <div v-else-if="isOwner" class="tip-row">
        <span class="text-secondary">这是你发布的商品，直接在下方回复买家留言即可</span>
      </div>

      <div v-else-if="!commentable" class="tip-row">
        <span class="text-secondary">该商品当前状态不支持留言咨询</span>
      </div>

      <template v-else>
        <el-input
          v-model="content"
          type="textarea"
          :rows="3"
          maxlength="500"
          show-word-limit
          placeholder="想问点什么？比如：还能小刀吗？配件齐全吗？能当面验机吗？"
        />
        <div class="editor-actions">
          <el-button type="primary" :loading="submitting" @click="submitComment">
            发表留言
          </el-button>
        </div>
      </template>
    </div>

    <el-divider class="divider" />

    <!-- ============ 留言列表 ============ -->
    <div v-loading="loading" class="list">
      <el-empty
        v-if="!loading && !comments.length"
        description="还没有人留言，来问第一个问题吧"
        :image-size="70"
      />

      <div v-for="item in comments" :key="item.id" class="thread">
        <!-- 一级留言 -->
        <div class="comment">
          <el-avatar :size="34" :src="item.userAvatar">
            {{ (item.userNickname || '?').charAt(0) }}
          </el-avatar>
          <div class="comment-body">
            <div class="comment-head">
              <span class="nickname">{{ item.userNickname || '匿名用户' }}</span>
              <span class="text-secondary time">{{ fromNow(item.createTime) }}</span>
            </div>
            <div class="content">{{ item.content }}</div>
            <div class="comment-actions">
              <el-button v-if="canReply" link type="primary" size="small" @click="startReply(item)">
                回复
              </el-button>
              <el-button
                v-if="item.canDelete"
                link
                type="danger"
                size="small"
                @click="handleDelete(item)"
              >
                删除
              </el-button>
            </div>
          </div>
        </div>

        <!-- 回复一级留言的输入框 -->
        <div v-if="isReplyTarget(item)" class="reply-editor">
          <el-input
            v-model="replyContent"
            type="textarea"
            :rows="2"
            maxlength="500"
            show-word-limit
            :placeholder="`回复 ${replyTarget.nickname}`"
          />
          <div class="editor-actions">
            <el-button size="small" @click="cancelReply">取消</el-button>
            <el-button
              type="primary"
              size="small"
              :loading="replySubmitting"
              @click="submitReply(item.id)"
            >
              回复
            </el-button>
          </div>
        </div>

        <!-- 该留言下的回复（只做两级，回复的回复也归到这里） -->
        <div v-if="item.replies?.length" class="replies">
          <div v-for="reply in item.replies" :key="reply.id">
            <div class="comment reply-item">
              <el-avatar :size="28" :src="reply.userAvatar">
                {{ (reply.userNickname || '?').charAt(0) }}
              </el-avatar>
              <div class="comment-body">
                <div class="comment-head">
                  <span class="nickname">{{ reply.userNickname || '匿名用户' }}</span>
                  <span v-if="reply.replyUserNickname" class="text-secondary at">
                    回复 {{ reply.replyUserNickname }}
                  </span>
                  <span class="text-secondary time">{{ fromNow(reply.createTime) }}</span>
                </div>
                <div class="content">{{ reply.content }}</div>
                <div class="comment-actions">
                  <el-button
                    v-if="canReply"
                    link
                    type="primary"
                    size="small"
                    @click="startReply(reply)"
                  >
                    回复
                  </el-button>
                  <el-button
                    v-if="reply.canDelete"
                    link
                    type="danger"
                    size="small"
                    @click="handleDelete(reply)"
                  >
                    删除
                  </el-button>
                </div>
              </div>
            </div>

            <!-- 回复某条回复的输入框：后端会把它归并到同一条一级留言下 -->
            <div v-if="isReplyTarget(reply)" class="reply-editor nested">
              <el-input
                v-model="replyContent"
                type="textarea"
                :rows="2"
                maxlength="500"
                show-word-limit
                :placeholder="`回复 ${replyTarget.nickname}`"
              />
              <div class="editor-actions">
                <el-button size="small" @click="cancelReply">取消</el-button>
                <el-button
                  type="primary"
                  size="small"
                  :loading="replySubmitting"
                  @click="submitReply(reply.id)"
                >
                  回复
                </el-button>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { createComment, deleteComment, listComments } from '@/api/comment'
import { fromNow } from '@/utils/format'
import { useUserStore } from '@/stores/user'

const props = defineProps({
  productId: { type: [Number, String], required: true },
  /** 当前用户是不是这个商品的发布者 */
  isOwner: { type: Boolean, default: false },
  /** 商品当前是否允许留言（待审核/已下架的商品不允许） */
  commentable: { type: Boolean, default: true }
})

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const loading = ref(false)
const comments = ref([])
const total = ref(0)

const content = ref('')
const submitting = ref(false)

const replyTarget = ref(null)
const replyContent = ref('')
const replySubmitting = ref(false)

/** 未登录不能回复；自己的商品可以回复但不能发一级留言（后端也会拦） */
const canReply = computed(() => userStore.isLogin)

function isReplyTarget(comment) {
  return replyTarget.value?.id === comment.id
}

async function loadComments() {
  loading.value = true
  try {
    const data = await listComments(props.productId)
    comments.value = data || []
    // 总数 = 一级留言 + 所有回复
    total.value = comments.value.reduce((sum, c) => sum + 1 + (c.replies?.length || 0), 0)
  } finally {
    loading.value = false
  }
}

function goLogin() {
  router.push({ path: '/login', query: { redirect: route.fullPath } })
}

async function submitComment() {
  if (!content.value.trim()) {
    ElMessage.warning('请先填写留言内容')
    return
  }

  submitting.value = true
  try {
    await createComment({ productId: Number(props.productId), content: content.value.trim() })
    ElMessage.success('留言成功')
    content.value = ''
    await loadComments()
  } finally {
    submitting.value = false
  }
}

function startReply(comment) {
  replyTarget.value = { id: comment.id, nickname: comment.userNickname || '该用户' }
  replyContent.value = ''
}

function cancelReply() {
  replyTarget.value = null
  replyContent.value = ''
}

/**
 * 提交回复。
 * 传的是"被点击那条留言的 id"，而不是父留言的 id ——
 * 如果点的是某条回复，后端会自动把它归并到同一条一级留言下，
 * 同时把"被回复人"记成那条回复的作者。
 * 前端不用关心层级，只表达"我在回复这一条"。
 */
async function submitReply(targetCommentId) {
  if (!replyContent.value.trim()) {
    ElMessage.warning('请先填写回复内容')
    return
  }

  replySubmitting.value = true
  try {
    await createComment({
      productId: Number(props.productId),
      content: replyContent.value.trim(),
      parentId: targetCommentId
    })
    ElMessage.success('回复成功')
    cancelReply()
    await loadComments()
  } finally {
    replySubmitting.value = false
  }
}

async function handleDelete(comment) {
  try {
    await ElMessageBox.confirm('确定删除这条留言吗？', '提示', { type: 'warning' })
  } catch {
    return
  }
  await deleteComment(comment.id)
  ElMessage.success('已删除')
  await loadComments()
}

onMounted(loadComments)

// 商品变了要重新拉留言（详情页在商品间跳转时）
watch(() => props.productId, loadComments)

defineExpose({ reload: loadComments })
</script>

<style scoped>
.comments {
  margin-top: 16px;
  padding: 18px 20px 22px;
}

.comments-title {
  display: flex;
  align-items: center;
  gap: 6px;
  margin: 0 0 16px;
  font-size: 15px;
}

.count {
  padding: 1px 8px;
  border-radius: 10px;
  background: var(--el-color-primary-light-9);
  color: var(--el-color-primary);
  font-size: 12px;
  font-weight: 400;
}

.tip-row {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 14px 16px;
  background: #f8fafc;
  border-radius: 6px;
  font-size: 13px;
}

.editor-actions {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
  margin-top: 10px;
}

.divider {
  margin: 18px 0 14px;
}

.list {
  min-height: 100px;
}

.thread + .thread {
  margin-top: 4px;
  padding-top: 14px;
  border-top: 1px dashed var(--app-border);
}

.comment {
  display: flex;
  gap: 12px;
  padding: 10px 0;
}

.comment-body {
  flex: 1;
  min-width: 0;
}

.comment-head {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
  margin-bottom: 4px;
}

.nickname {
  font-size: 13px;
  font-weight: 600;
  color: var(--app-text-primary);
}

.at {
  font-size: 12px;
}

.time {
  font-size: 12px;
  margin-left: auto;
}

.content {
  font-size: 13px;
  line-height: 1.8;
  color: var(--app-text-regular);
  word-break: break-word;
  white-space: pre-wrap;
}

.comment-actions {
  margin-top: 2px;
}

.comment-actions :deep(.el-button + .el-button) {
  margin-left: 12px;
}

.replies {
  margin: 4px 0 8px 46px;
  padding: 4px 14px;
  background: #f8fafc;
  border-radius: 6px;
}

.reply-item {
  padding: 8px 0;
}

.reply-item + .reply-item {
  border-top: 1px solid #eef2f7;
}

.reply-editor {
  margin: 8px 0 12px 46px;
}

.reply-editor.nested {
  margin-left: 0;
}
</style>
