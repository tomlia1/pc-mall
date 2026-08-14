export type ProductType = 'LAPTOP' | 'CPU' | 'MOTHERBOARD' | 'GPU' | 'RAM' | 'SSD' | 'PSU' | 'CASE'
export type BuildSlot = Exclude<ProductType, 'LAPTOP'>

export const BUILD_SLOTS: ReadonlyArray<{ type: BuildSlot; label: string }> = [
  { type: 'CPU', label: 'CPU' },
  { type: 'MOTHERBOARD', label: '主板' },
  { type: 'GPU', label: '显卡' },
  { type: 'RAM', label: '内存套装' },
  { type: 'SSD', label: 'SSD' },
  { type: 'PSU', label: '电源' },
  { type: 'CASE', label: '机箱' }
]

export interface Product {
  id: number
  name: string
  brand: string
  category: string
  productType: ProductType
  price: number
  cpu?: string
  gpu?: string
  memoryGb?: number
  storageGb?: number
  tags?: string
  coverImage?: string
  status?: number
}

export type ProductMatchType = 'EXACT' | 'ALTERNATIVE' | 'NONE'

export interface ProductSearchResult {
  matchType: ProductMatchType
  reason: string
  products: Product[]
}

export function isBuildSlot(type: ProductType): type is BuildSlot {
  return type !== 'LAPTOP'
}

export interface ChatMessage {
  id: string
  role: 'user' | 'assistant'
  content: string
  products: Product[]
  productSearchResult?: ProductSearchResult
  budgetResult?: BudgetFeasibilityResult
  stopped?: boolean
}

export interface LeadPayload {
  productId: number
  contactType: 'phone' | 'weixin' | 'email'
  contactValue: string
  privacyAgreed: boolean
  userBudget?: number
  userRemark?: string
}

export interface BuildCheckRequest {
  cpuId: number | null
  motherboardId: number | null
  gpuId: number | null
  ramId: number | null
  ssdId: number | null
  psuId: number | null
  caseId: number | null
}

export type BuildCheckStatus = 'INCOMPLETE' | 'ERROR' | 'WARNING' | 'PASS'

export interface BuildCheckIssue {
  level: 'ERROR' | 'WARNING'
  rule: string
  message: string
}

export interface BuildCheckResult {
  status: BuildCheckStatus
  missingSlots: string[]
  issues: BuildCheckIssue[]
  totalPrice: number
  estimatedPowerW: number | null
  recommendedPsuW: number | null
}

export interface BudgetBuildCandidate {
  build: BuildCheckRequest
  products: Product[]
  totalPrice: number
  check: BuildCheckResult
}

export interface BudgetFeasibilityResult {
  budget: number
  hasWithinBudgetPlan: boolean
  minimumFeasibleTotal: number | null
  budgetGap: number | null
  candidate: BudgetBuildCandidate | null
}
