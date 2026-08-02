<script setup lang="ts">
/**
 * 知识库智能化页面
 * 整合 AI标签、学习路径推荐、知识图谱可视化三大能力
 */
import { ref, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import PageHeader from '@/components/core/PageHeader.vue'
import StatCard from '@/components/core/StatCard.vue'
import StatusTag from '@/components/core/StatusTag.vue'
import { knowledgeIntelligenceApi } from '@/api/knowledge-intelligence'
import {
  type AITag,
  type LearningPath,
  type KnowledgeGraph,
  type KnowledgeGraphNode,
  type LearningPathDifficulty,
  LearningPathDifficultyOptions,
  LearningPathDifficultyLabelMap,
  LearningPathStatusLabelMap,
} from '@/types/knowledge-intelligence'

/* ===== 数据状态 ===== */
const loading = ref(false)
const activeTab = ref('tags')
const aiTags = ref<AITag[]>([])
const learningPaths = ref<LearningPath[]>([])
const knowledgeGraph = ref<KnowledgeGraph>({ nodes: [], edges: [] })

/* ===== 统计 ===== */
const stats = computed<Array<{ icon: string; label: string; value: number; colorType: 'primary' | 'success' | 'warning' | 'error' | 'info' }>>(() => [
  { icon: 'PriceTag', label: 'AI标签数', value: aiTags.value.length, colorType: 'primary' },
  { icon: 'Guide', label: '学习路径', value: learningPaths.value.length, colorType: 'success' },
  { icon: 'Connection', label: '知识节点', value: knowledgeGraph.value.nodes.length, colorType: 'warning' },
  { icon: 'Share', label: '知识关联', value: knowledgeGraph.value.edges.length, colorType: 'info' },
])

/* ===== 学习路径过滤 ===== */
const difficultyFilter = ref<LearningPathDifficulty | ''>('')
const filteredPaths = computed(() => {
  if (!difficultyFilter.value) return learningPaths.value
  return learningPaths.value.filter(p => p.difficulty === difficultyFilter.value)
})

/* ===== 学习路径详情 ===== */
const pathDetailVisible = ref(false)
const currentPath = ref<LearningPath | null>(null)

function handleViewPath(path: LearningPath) {
  currentPath.value = path
  pathDetailVisible.value = true
}

/* ===== 加载数据 ===== */
async function loadData() {
  loading.value = true
  try {
    const [tags, paths, graph] = await Promise.all([
      knowledgeIntelligenceApi.getAITags(),
      knowledgeIntelligenceApi.getLearningPaths(),
      knowledgeIntelligenceApi.getKnowledgeGraph(),
    ])
    aiTags.value = tags
    learningPaths.value = paths
    knowledgeGraph.value = graph
  } catch (error: unknown) {
    if (error instanceof Error) ElMessage.error(error.message || '加载失败')
  } finally {
    loading.value = false
  }
}

/* ===== 工具函数 ===== */
function getDifficultyStatus(difficulty: LearningPathDifficulty): string {
  const map: Record<LearningPathDifficulty, string> = {
    beginner: 'success',
    intermediate: 'warning',
    advanced: 'error',
  }
  return map[difficulty] || 'info'
}

function getLearningPathStatus(status: string): string {
  const map: Record<string, string> = {
    not_started: 'info',
    in_progress: 'warning',
    completed: 'success',
  }
  return map[status] || 'info'
}

function formatDuration(minutes: number): string {
  if (minutes < 60) return `${minutes}分钟`
  const h = Math.floor(minutes / 60)
  const m = minutes % 60
  return m > 0 ? `${h}小时${m}分` : `${h}小时`
}

/** 获取节点类型标签 */
function getNodeTypeLabel(type: string): string {
  const map: Record<string, string> = {
    article: '文章',
    tag: '标签',
    category: '分类',
  }
  return map[type] || type
}

/** 获取节点类型颜色 */
function getNodeTypeColor(type: string): string {
  const map: Record<string, string> = {
    article: 'var(--fts-primary)',
    tag: 'var(--fts-success)',
    category: 'var(--fts-warning)',
  }
  return map[type] || 'var(--fts-text-tertiary)'
}

/** 知识图谱节点（按类型分组） */
const graphNodesByType = computed(() => {
  const groups: Record<string, KnowledgeGraphNode[]> = {
    category: [],
    tag: [],
    article: [],
  }
  for (const node of knowledgeGraph.value.nodes) {
    if (groups[node.type]) groups[node.type].push(node)
  }
  return groups
})

onMounted(() => loadData())
</script>

<template>
  <div class="modern-page">
    <PageHeader title="知识库智能" description="AI标签 · 学习路径推荐 · 知识图谱可视化" />

    <!-- 统计卡片 -->
    <section class="stats-section">
      <StatCard v-for="stat in stats" :key="stat.label" v-bind="stat" variant="bordered" />
    </section>

    <!-- Tab 区域 -->
    <el-tabs v-model="activeTab" class="intelligence-tabs" v-loading="loading">
      <!-- AI标签 Tab -->
      <el-tab-pane label="AI智能标签" name="tags">
        <div class="tab-desc">
          <el-alert type="info" :closable="false">
            <template #title>AI自动为知识库文章打标签，按分类聚合展示。置信度越高表示AI对该标签的判断越准确。</template>
          </el-alert>
        </div>
        <div class="tag-cloud">
          <div v-for="tag in aiTags" :key="tag.tagId" class="tag-item" :style="{ borderColor: tag.color }">
            <div class="tag-item__header">
              <span class="tag-item__name" :style="{ color: tag.color }">{{ tag.tagName }}</span>
              <span class="tag-item__category">{{ tag.category }}</span>
            </div>
            <div class="tag-item__stats">
              <span class="tag-item__count">{{ tag.articleCount }} 篇文章</span>
              <span class="tag-item__confidence">置信度 {{ tag.confidence }}%</span>
            </div>
            <div class="tag-item__bar">
              <div class="tag-item__bar-fill" :style="{ width: `${tag.confidence}%`, background: tag.color }"></div>
            </div>
          </div>
        </div>
      </el-tab-pane>

      <!-- 学习路径 Tab -->
      <el-tab-pane label="学习路径推荐" name="paths">
        <div class="filter-bar">
          <el-radio-group v-model="difficultyFilter" size="small">
            <el-radio-button value="">全部</el-radio-button>
            <el-radio-button v-for="opt in LearningPathDifficultyOptions" :key="opt.value" :value="opt.value">{{ opt.label }}</el-radio-button>
          </el-radio-group>
        </div>

        <div class="path-grid">
          <div v-for="path in filteredPaths" :key="path.pathId" class="path-card">
            <div class="path-card__header">
              <div class="path-card__title">{{ path.pathName }}</div>
              <StatusTag :status="getDifficultyStatus(path.difficulty)" :label="LearningPathDifficultyLabelMap[path.difficulty]" size="small" />
            </div>
            <div class="path-card__desc">{{ path.description }}</div>
            <div class="path-card__meta">
              <div class="meta-item">
                <el-icon><User /></el-icon>
                <span>{{ path.targetPosition }}</span>
              </div>
              <div class="meta-item">
                <el-icon><OfficeBuilding /></el-icon>
                <span>{{ path.targetDepartment }}</span>
              </div>
              <div class="meta-item">
                <el-icon><Clock /></el-icon>
                <span>{{ formatDuration(path.totalDuration) }}</span>
              </div>
            </div>
            <div class="path-card__progress">
              <div class="progress-info">
                <span>完成率</span>
                <span class="progress-value">{{ path.completionRate }}%</span>
              </div>
              <el-progress :percentage="path.completionRate" :stroke-width="6" :show-text="false" />
            </div>
            <div class="path-card__footer">
              <span class="path-card__learners">{{ path.learnerCount }} 人在学</span>
              <el-button link type="primary" size="small" @click="handleViewPath(path)">查看详情</el-button>
            </div>
          </div>
        </div>
      </el-tab-pane>

      <!-- 知识图谱 Tab -->
      <el-tab-pane label="知识图谱" name="graph">
        <div class="tab-desc">
          <el-alert type="info" :closable="false">
            <template #title>知识图谱展示文章、标签、分类之间的关联关系，帮助发现知识盲区和学习路径。</template>
          </el-alert>
        </div>

        <!-- 图谱节点统计 -->
        <div class="graph-stats">
          <div v-for="(nodes, type) in graphNodesByType" :key="type" class="graph-stat-item">
            <div class="graph-stat-dot" :style="{ background: getNodeTypeColor(type) }"></div>
            <span class="graph-stat-label">{{ getNodeTypeLabel(type) }}</span>
            <span class="graph-stat-count">{{ nodes.length }}</span>
          </div>
        </div>

        <!-- 图谱可视化（简化版：节点列表+关联展示） -->
        <div class="graph-visualization">
          <div class="graph-section">
            <h4 class="graph-section__title">核心标签节点</h4>
            <div class="graph-nodes">
              <div v-for="node in graphNodesByType.tag" :key="node.id" class="graph-node" :style="{ borderColor: getNodeTypeColor(node.type) }">
                <div class="graph-node__label">{{ node.label }}</div>
                <div class="graph-node__info">
                  <span v-if="node.articleCount" class="graph-node__count">{{ node.articleCount }}篇</span>
                </div>
              </div>
            </div>
          </div>

          <div class="graph-section">
            <h4 class="graph-section__title">关联关系</h4>
            <div class="graph-edges">
              <div v-for="(edge, index) in knowledgeGraph.edges.filter(e => e.relation === 'related_to')" :key="index" class="graph-edge">
                <span class="edge-source">{{ knowledgeGraph.nodes.find(n => n.id === edge.source)?.label }}</span>
                <el-icon class="edge-arrow"><Right /></el-icon>
                <span class="edge-target">{{ knowledgeGraph.nodes.find(n => n.id === edge.target)?.label }}</span>
                <span class="edge-weight">强度 {{ edge.weight }}/10</span>
              </div>
            </div>
          </div>
        </div>
      </el-tab-pane>

    </el-tabs>

    <!-- 学习路径详情对话框 -->
    <el-dialog v-model="pathDetailVisible" title="学习路径详情" width="680px" class="fts-dialog--md" destroy-on-close lock-scroll="false">
      <template v-if="currentPath">
        <el-descriptions :column="2" border size="small">
          <el-descriptions-item label="路径名称">{{ currentPath.pathName }}</el-descriptions-item>
          <el-descriptions-item label="难度">
            <StatusTag :status="getDifficultyStatus(currentPath.difficulty)" :label="LearningPathDifficultyLabelMap[currentPath.difficulty]" size="small" />
          </el-descriptions-item>
          <el-descriptions-item label="适用岗位">{{ currentPath.targetPosition }}</el-descriptions-item>
          <el-descriptions-item label="适用部门">{{ currentPath.targetDepartment }}</el-descriptions-item>
          <el-descriptions-item label="总时长">{{ formatDuration(currentPath.totalDuration) }}</el-descriptions-item>
          <el-descriptions-item label="学习人数">{{ currentPath.learnerCount }}人</el-descriptions-item>
          <el-descriptions-item label="完成率">{{ currentPath.completionRate }}%</el-descriptions-item>
          <el-descriptions-item label="状态">
            <StatusTag :status="getLearningPathStatus(currentPath.status)" :label="LearningPathStatusLabelMap[currentPath.status]" size="small" />
          </el-descriptions-item>
          <el-descriptions-item label="路径描述" :span="2">{{ currentPath.description }}</el-descriptions-item>
        </el-descriptions>

        <el-divider content-position="left">学习节点（{{ currentPath.nodes.length }}个）</el-divider>

        <el-timeline>
          <el-timeline-item v-for="node in currentPath.nodes" :key="node.nodeId" :type="node.required ? 'primary' : 'info'" :timestamp="`第${node.order}步 · ${formatDuration(node.duration)}`" placement="top">
            <div class="path-node">
              <span class="path-node__title">{{ node.articleTitle }}</span>
              <StatusTag v-if="node.required" status="error" label="必修" size="small" />
              <StatusTag v-else status="info" label="选修" size="small" />
              <StatusTag v-if="node.dependsOn?.length" status="warning" label="有前置依赖" size="small" />
            </div>
          </el-timeline-item>
        </el-timeline>
      </template>
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="pathDetailVisible = false">关闭</el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped lang="scss">
.stats-section { grid-template-columns: repeat(4, 1fr); }

.intelligence-tabs {
  background: var(--fts-bg-card);
  border-radius: var(--fts-radius-md);
  padding: var(--fts-space-4);
  box-shadow: var(--fts-shadow-sm);
}

.tab-desc { margin-bottom: var(--fts-space-4); }
.filter-bar { margin-bottom: var(--fts-space-4); }

/* AI标签云 */
.tag-cloud {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(220px, 1fr));
  gap: var(--fts-space-3);
}

.tag-item {
  background: var(--fts-bg-card);
  border: 2px solid;
  border-radius: var(--fts-radius-md);
  padding: var(--fts-space-3);
  transition: transform 0.2s;

  &:hover { transform: translateY(-2px); }

  &__header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: var(--fts-space-2);
  }

  &__name {
    font-size: var(--fts-font-size-base);
    font-weight: 600;
  }

  &__category {
    font-size: var(--fts-font-size-xs);
    color: var(--fts-text-tertiary);
    background: var(--fts-bg-secondary);
    padding: 2px 8px;
    border-radius: var(--fts-radius-sm);
  }

  &__stats {
    display: flex;
    justify-content: space-between;
    font-size: var(--fts-font-size-sm);
    color: var(--fts-text-secondary);
    margin-bottom: var(--fts-space-2);
  }

  &__bar {
    height: 4px;
    background: var(--fts-bg-secondary);
    border-radius: 2px;
    overflow: hidden;
  }

  &__bar-fill {
    height: 100%;
    border-radius: 2px;
    transition: width 0.3s;
  }
}

/* 学习路径 */
.path-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(340px, 1fr));
  gap: var(--fts-space-4);
}

.path-card {
  background: var(--fts-bg-card);
  border: 1px solid var(--fts-border-color);
  border-radius: var(--fts-radius-md);
  padding: var(--fts-space-4);
  display: flex;
  flex-direction: column;
  gap: var(--fts-space-2);
  transition: box-shadow 0.2s;

  &:hover { box-shadow: var(--fts-shadow); }

  &__header {
    display: flex;
    justify-content: space-between;
    align-items: center;
  }

  &__title {
    font-size: var(--fts-font-size-base);
    font-weight: 600;
    color: var(--fts-text-primary);
  }

  &__desc {
    font-size: var(--fts-font-size-sm);
    color: var(--fts-text-secondary);
    line-height: 1.5;
    min-height: 42px;
  }

  &__meta {
    display: flex;
    gap: var(--fts-space-3);
    font-size: var(--fts-font-size-sm);
    color: var(--fts-text-tertiary);

    .meta-item {
      display: flex;
      align-items: center;
      gap: 4px;
    }
  }

  &__progress {
    .progress-info {
      display: flex;
      justify-content: space-between;
      font-size: var(--fts-font-size-sm);
      margin-bottom: 4px;

      .progress-value {
        font-weight: 600;
        color: var(--fts-primary);
      }
    }
  }

  &__footer {
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding-top: var(--fts-space-2);
    border-top: 1px solid var(--fts-border-color);
  }

  &__learners {
    font-size: var(--fts-font-size-sm);
    color: var(--fts-text-tertiary);
  }
}

/* 知识图谱 */
.graph-stats {
  display: flex;
  gap: var(--fts-space-4);
  margin-bottom: var(--fts-space-4);
}

.graph-stat-item {
  display: flex;
  align-items: center;
  gap: var(--fts-space-1);
  font-size: var(--fts-font-size-sm);
}

.graph-stat-dot {
  width: 10px;
  height: 10px;
  border-radius: 50%;
}

.graph-stat-label { color: var(--fts-text-secondary); }
.graph-stat-count { font-weight: 600; color: var(--fts-text-primary); }

.graph-visualization {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: var(--fts-space-4);
}

.graph-section {
  &__title {
    font-size: var(--fts-font-size-sm);
    font-weight: 600;
    color: var(--fts-text-primary);
    margin-bottom: var(--fts-space-3);
  }
}

.graph-nodes {
  display: flex;
  flex-wrap: wrap;
  gap: var(--fts-space-2);
}

.graph-node {
  border: 2px solid;
  border-radius: var(--fts-radius-md);
  padding: var(--fts-space-2) var(--fts-space-3);
  background: var(--fts-bg-card);

  &__label {
    font-size: var(--fts-font-size-sm);
    font-weight: 500;
    color: var(--fts-text-primary);
  }

  &__info {
    font-size: var(--fts-font-size-xs);
    color: var(--fts-text-tertiary);
    margin-top: 2px;
  }
}

.graph-edges {
  display: flex;
  flex-direction: column;
  gap: var(--fts-space-2);
}

.graph-edge {
  display: flex;
  align-items: center;
  gap: var(--fts-space-2);
  padding: var(--fts-space-2);
  background: var(--fts-bg-secondary);
  border-radius: var(--fts-radius-sm);
  font-size: var(--fts-font-size-sm);

  .edge-source { color: var(--fts-primary); font-weight: 500; }
  .edge-target { color: var(--fts-success); font-weight: 500; }
  .edge-arrow { color: var(--fts-text-tertiary); }
  .edge-weight { margin-left: auto; color: var(--fts-text-tertiary); font-size: var(--fts-font-size-xs); }
}

.path-node {
  display: flex;
  align-items: center;
  gap: var(--fts-space-2);

  &__title {
    font-weight: 500;
  }
}
</style>
