<template>
  <div class="app-container publish-page">
    <div class="page-head">
      <h2>{{ isEdit ? '编辑商品' : '发布闲置数码' }}</h2>
      <p class="text-secondary">
        {{
          isEdit
            ? '修改后需要平台重新审核，审核通过才会重新上架'
            : '如实填写商品信息，审核通过后其他用户才能看到'
        }}
      </p>
    </div>

    <el-form
      ref="formRef"
      :model="form"
      :rules="rules"
      label-width="96px"
      label-position="right"
      v-loading="loading"
    >
      <!-- ============ 基本信息 ============ -->
      <el-card shadow="never" class="section">
        <template #header><span class="section-title">基本信息</span></template>

        <el-form-item label="商品分类" prop="categoryId">
          <el-select v-model="form.categoryId" placeholder="请选择分类" style="width: 260px">
            <el-option v-for="c in categories" :key="c.id" :label="c.name" :value="c.id" />
          </el-select>
        </el-form-item>

        <el-form-item label="标题" prop="title">
          <el-input
            v-model.trim="form.title"
            maxlength="100"
            show-word-limit
            placeholder="建议格式：品牌 + 型号 + 关键配置，如 iPhone 14 Pro 256G 暗紫色"
          />
        </el-form-item>

        <el-form-item label="详细描述" prop="description">
          <el-input
            v-model="form.description"
            type="textarea"
            :rows="5"
            maxlength="2000"
            show-word-limit
            placeholder="补充购入时间、使用情况、有无拆修、配件是否齐全、可小刀等信息"
          />
        </el-form-item>

        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="出售价格" prop="price">
              <el-input-number
                v-model="form.price"
                :min="0.01"
                :max="9999999.99"
                :precision="2"
                :step="100"
                controls-position="right"
                style="width: 100%"
              />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="原价" prop="originalPrice">
              <el-input-number
                v-model="form.originalPrice"
                :min="0"
                :max="9999999.99"
                :precision="2"
                :step="100"
                controls-position="right"
                style="width: 100%"
              />
            </el-form-item>
          </el-col>
        </el-row>
        <div class="form-tip">填写原价后，页面会显示「原价划线」，更吸引买家</div>
      </el-card>

      <!-- ============ 数码参数 ============ -->
      <el-card shadow="never" class="section">
        <template #header>
          <span class="section-title">数码参数</span>
          <span class="section-hint"
            >这几项是数码产品区别于普通二手商品的关键，填得越细越容易被搜到</span
          >
        </template>

        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="品牌" prop="brand">
              <el-input
                v-model.trim="form.brand"
                maxlength="50"
                placeholder="如 Apple、小米、联想"
              />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="型号" prop="model">
              <el-input v-model.trim="form.model" maxlength="100" placeholder="如 iPhone 14 Pro" />
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="成色" prop="conditionLevel">
              <el-select v-model="form.conditionLevel" placeholder="请选择成色" style="width: 100%">
                <el-option v-for="c in CONDITION_OPTIONS" :key="c" :label="c" :value="c" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="质保情况" prop="warranty">
              <el-select v-model="form.warranty" placeholder="请选择质保情况" style="width: 100%">
                <el-option v-for="w in WARRANTY_OPTIONS" :key="w" :label="w" :value="w" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>

        <el-form-item label="购入时间" prop="purchaseDate">
          <el-date-picker
            v-model="form.purchaseDate"
            type="date"
            value-format="YYYY-MM-DD"
            placeholder="选择购入日期"
            :disabled-date="disableFuture"
            style="width: 260px"
          />
        </el-form-item>

        <el-form-item label="参数配置">
          <div class="spec-editor">
            <div v-for="(row, index) in specList" :key="index" class="spec-row">
              <el-input v-model.trim="row.key" placeholder="参数名，如 存储" class="spec-key" />
              <el-input
                v-model.trim="row.value"
                placeholder="参数值，如 256GB"
                class="spec-value"
              />
              <el-button
                :icon="Delete"
                circle
                plain
                type="danger"
                @click="specList.splice(index, 1)"
              />
            </div>
            <el-button :icon="Plus" plain @click="specList.push({ key: '', value: '' })">
              添加一项参数
            </el-button>

            <div v-if="presets.length" class="preset-row">
              <span class="text-secondary">常用：</span>
              <el-tag
                v-for="p in presets"
                :key="p"
                class="preset-tag"
                effect="plain"
                @click="addSpec(p)"
              >
                + {{ p }}
              </el-tag>
            </div>
          </div>
        </el-form-item>
      </el-card>

      <!-- ============ 图片 ============ -->
      <el-card shadow="never" class="section">
        <template #header>
          <span class="section-title">商品图片</span>
          <span class="section-hint">最多 9 张，第一张作为封面</span>
        </template>

        <el-form-item label="图片" prop="images">
          <el-upload
            v-model:file-list="fileList"
            list-type="picture-card"
            :limit="9"
            accept="image/jpeg,image/png,image/gif,image/webp,image/bmp"
            :http-request="handleUpload"
            :before-upload="beforeUpload"
            :on-exceed="onExceed"
            :on-remove="onRemove"
          >
            <el-icon><Plus /></el-icon>
          </el-upload>
          <div class="form-tip">
            支持 jpg / png / gif / webp / bmp，单张不超过 10MB。建议拍摄实物图，不要用官网图。
          </div>
        </el-form-item>
      </el-card>

      <!-- ============ 交易信息 ============ -->
      <el-card shadow="never" class="section">
        <template #header><span class="section-title">交易信息</span></template>

        <el-form-item label="交易地点" prop="tradeLocation">
          <el-input
            v-model.trim="form.tradeLocation"
            maxlength="100"
            placeholder="如 学校东门、三号宿舍楼下"
          />
        </el-form-item>

        <el-form-item label="联系方式" prop="contact">
          <el-input
            v-model.trim="form.contact"
            maxlength="100"
            placeholder="如 微信 xxx / QQ 123456"
          />
        </el-form-item>
      </el-card>

      <div class="actions">
        <el-button size="large" @click="router.back()">取消</el-button>
        <el-button type="primary" size="large" :loading="submitting" @click="handleSubmit">
          {{ isEdit ? '保存并重新提交审核' : '提交审核' }}
        </el-button>
      </div>
    </el-form>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Delete, Plus } from '@element-plus/icons-vue'
import { listCategories } from '@/api/category'
import { createProduct, getProduct, updateProduct } from '@/api/product'
import { uploadImage } from '@/api/upload'
import { CONDITION_OPTIONS, WARRANTY_OPTIONS } from '@/utils/format'

/** 不同分类给不同的参数提示，体现"行业针对性强" */
const SPEC_PRESETS = {
  手机: ['内存', '存储', '电池健康', '版本', '是否拆修'],
  笔记本电脑: ['处理器', '内存', '硬盘', '显卡', '屏幕'],
  平板电脑: ['内存', '存储', '屏幕尺寸', '网络版本'],
  '相机/摄像机': ['有效像素', '快门次数', '镜头', '配件'],
  '耳机/音响': ['类型', '连接方式', '降噪', '续航'],
  智能穿戴: ['表盘尺寸', '续航', '网络版本', '配件'],
  游戏设备: ['版本', '手柄数量', '是否破解', '配件'],
  数码配件: ['规格', '接口', '是否原装'],
  电脑外设: ['类型', '连接方式', '轴体', '是否原装'],
  其他数码: ['规格', '成色说明']
}

const route = useRoute()
const router = useRouter()

const productId = computed(() => route.params.id)
const isEdit = computed(() => !!productId.value)

const formRef = ref()
const loading = ref(false)
const submitting = ref(false)
const categories = ref([])
const fileList = ref([])
const specList = ref([])

const form = reactive({
  categoryId: null,
  title: '',
  description: '',
  price: null,
  originalPrice: null,
  brand: '',
  model: '',
  conditionLevel: '',
  warranty: '',
  purchaseDate: null,
  tradeLocation: '',
  contact: ''
})

const rules = {
  categoryId: [{ required: true, message: '请选择商品分类', trigger: 'change' }],
  title: [
    { required: true, message: '请填写商品标题', trigger: 'blur' },
    { min: 2, max: 100, message: '标题长度为 2-100 个字符', trigger: 'blur' }
  ],
  description: [
    { required: true, message: '请填写商品描述', trigger: 'blur' },
    { max: 2000, message: '描述不能超过 2000 个字符', trigger: 'blur' }
  ],
  price: [{ required: true, message: '请填写出售价格', trigger: 'change' }]
}

/** 当前分类对应的参数提示 */
const presets = computed(() => {
  const category = categories.value.find((c) => c.id === form.categoryId)
  if (!category) return []
  const all = SPEC_PRESETS[category.name] || []
  // 已经加过的就不再提示
  const used = new Set(specList.value.map((r) => r.key))
  return all.filter((p) => !used.has(p))
})

function addSpec(key) {
  specList.value.push({ key, value: '' })
}

/** 不能选择未来的日期 */
function disableFuture(date) {
  return date.getTime() > Date.now()
}

/* ---------------- 图片上传 ---------------- */
function beforeUpload(file) {
  const isImage = file.type.startsWith('image/')
  if (!isImage) {
    ElMessage.error('只能上传图片文件')
    return false
  }
  const under10M = file.size / 1024 / 1024 < 10
  if (!under10M) {
    ElMessage.error('图片不能超过 10MB')
    return false
  }
  return true
}

function onExceed() {
  ElMessage.warning('最多上传 9 张图片')
}

function onRemove() {
  // el-upload 会自动从 fileList 移除，这里无需额外处理
}

/**
 * 自定义上传。
 * <p>
 * 关键：**直接返回 Promise，绝不要手动调 options.onSuccess**。
 * el-upload 内部有这么一段：
 * <pre>
 *   const request = httpRequest(options)
 *   if (request instanceof Promise) request.then(options.onSuccess, options.onError)
 * </pre>
 * 如果既返回 Promise 又自己调 onSuccess，回调就会触发两次；
 * 第二次传的是 async 函数返回的 undefined，把 file.response 覆盖成 undefined，
 * 于是取值时退回到 file.url —— 那是浏览器给本地预览生成的 blob: 地址，
 * 存进数据库后只有当前页面能看到，别人和刷新后的自己都看不到。
 * 这个坑真实发生过，见下方 collectImageUrls 的兜底过滤。
 */
function handleUpload(options) {
  return uploadImage(options.file)
}

/**
 * 从 el-upload 的文件列表里取出图片地址。
 * - 新上传的图片：地址在 f.response（上传成功后由 el-upload 写入）
 * - 编辑时回填的图片：地址在 f.url（从接口数据构造）
 * <p>
 * 最后过滤掉 blob:/data: —— 它们是浏览器本地地址，只在当前页面有效，
 * 一旦进了数据库就是"图片永远显示不出来"的坏数据。
 */
function collectImageUrls() {
  return fileList.value
    .map((f) => (typeof f.response === 'string' ? f.response : f.url))
    .filter(
      (url) => typeof url === 'string' && !url.startsWith('blob:') && !url.startsWith('data:')
    )
}

/* ---------------- 数据加载 ---------------- */
async function loadCategories() {
  categories.value = await listCategories()
}

async function loadProduct() {
  loading.value = true
  try {
    const data = await getProduct(productId.value)
    Object.assign(form, {
      categoryId: data.categoryId,
      title: data.title,
      description: data.description,
      price: data.price,
      originalPrice: data.originalPrice,
      brand: data.brand,
      model: data.model,
      conditionLevel: data.conditionLevel,
      warranty: data.warranty,
      purchaseDate: data.purchaseDate,
      tradeLocation: data.tradeLocation,
      contact: data.contact
    })
    // 已上传的图片回填到 el-upload
    fileList.value = (data.images || []).map((url, index) => ({
      name: `图片${index + 1}`,
      url
    }))
    // JSON 文本反序列化成键值对列表
    specList.value = parseSpec(data.spec)
  } finally {
    loading.value = false
  }
}

function parseSpec(spec) {
  if (!spec) return []
  try {
    return Object.entries(JSON.parse(spec)).map(([key, value]) => ({ key, value }))
  } catch {
    return []
  }
}

function buildSpec() {
  const entries = specList.value
    .filter((row) => row.key && row.value)
    .map((row) => [row.key, row.value])
  return entries.length ? JSON.stringify(Object.fromEntries(entries)) : null
}

/* ---------------- 提交 ---------------- */
async function handleSubmit() {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  const images = collectImageUrls()
  if (!images.length) {
    ElMessage.warning('请至少上传一张商品图片')
    return
  }

  const payload = {
    ...form,
    spec: buildSpec(),
    images
  }

  submitting.value = true
  try {
    if (isEdit.value) {
      await updateProduct(productId.value, payload)
      ElMessage.success('修改成功，等待平台重新审核')
    } else {
      await createProduct(payload)
      ElMessage.success('发布成功，等待平台审核')
    }
    router.push('/user/products')
  } catch {
    // 错误提示由请求拦截器统一处理
  } finally {
    submitting.value = false
  }
}

onMounted(async () => {
  await loadCategories()
  if (isEdit.value) {
    await loadProduct()
  }
})
</script>

<style scoped>
.publish-page {
  padding: 24px 16px 48px;
  max-width: 860px;
}

.page-head {
  margin-bottom: 16px;
}

.page-head h2 {
  margin: 0 0 6px;
  font-size: 20px;
}

.page-head p {
  margin: 0;
  font-size: 13px;
}

.section {
  margin-bottom: 16px;
}

.section-title {
  font-weight: 600;
}

.section-hint {
  margin-left: 10px;
  font-size: 12px;
  color: var(--app-text-secondary);
}

.form-tip {
  margin: -8px 0 8px 96px;
  font-size: 12px;
  color: var(--app-text-secondary);
}

.spec-editor {
  width: 100%;
}

.spec-row {
  display: flex;
  gap: 8px;
  margin-bottom: 8px;
}

.spec-key {
  width: 180px;
}

.spec-value {
  flex: 1;
}

.preset-row {
  margin-top: 12px;
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
  font-size: 12px;
}

.preset-tag {
  cursor: pointer;
}

.preset-tag:hover {
  color: var(--el-color-primary);
  border-color: var(--el-color-primary);
}

.actions {
  display: flex;
  justify-content: center;
  gap: 12px;
  margin-top: 24px;
}
</style>
