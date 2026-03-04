export interface UserTable {
  id: string
  username: string
  password_hash: string
  email: string
  is_active: boolean
  created_at: Date
  last_login: Date | null
  balance: number
}

export interface Database {
  users: UserTable
}
