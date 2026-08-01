export interface Product {
  id: number
  name: string
  brand: string
  category: string
  price: number
  cpu?: string
  gpu?: string
  memoryGb?: number
  storageGb?: number
  tags?: string
  coverImage?: string
  status?: number
}

export interface ChatMessage {
  id: string
  role: 'user' | 'assistant'
  content: string
  products: Product[]
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
