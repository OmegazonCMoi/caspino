export interface UserTable {
  id: string
  username: string
  password: string
  email: string
  is_active: boolean
  created_at: Date
  last_login: Date
  balance: number
}

export interface Database {
  users: UserTable
}
