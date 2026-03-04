import express, { type Request, type Response } from "express"
import bodyParser from "body-parser"
import bcrypt from "bcrypt"
import crypto from "crypto"
import jwt from "jsonwebtoken"
import { db } from "./db/index.ts"
import "dotenv/config"

const SALT_ROUNDS = 12

const JWT_SECRET = process.env.JWT_SECRET

if (!JWT_SECRET) {
  throw new Error("JWT Secret is missing !")
}

const app = express()
const PORT = 5500

app.use(bodyParser.json())

const authenticateJWT = (req: Request, res: Response, next: Function) => {
  const authHeader = req.headers.authorization

  if (!authHeader || !authHeader.startsWith("Bearer ")) {
    return res.sendStatus(401)
  }

  const token = authHeader.split(" ")[1]

  if (!token) {
    return res.sendStatus(401)
  }

  jwt.verify(token, JWT_SECRET, (err, user) => {
    if (err) return res.sendStatus(403)
    ;(req as any).user = user
    next()
  })
}

const hashPassword = (password: string): Promise<string> => {
  return bcrypt.hash(password, SALT_ROUNDS)
}

export const verifyPassword = (
  password: string,
  hash: string,
): Promise<boolean> => {
  return bcrypt.compare(password, hash)
}

app.post("/signup", async (req: Request, res: Response) => {
  const { username, password, email } = req.body

  const user = await db
    .selectFrom("users")
    .select(["username", "email"])
    .where((eb: any) =>
      eb.or([eb("email", "=", email), eb("username", "=", username)]),
    )
    .executeTakeFirst()

  if (user) {
    if (user.username)
      return res.status(409).json({ message: "Username already taken" })

    return res.status(409).json({ message: "Email address already taken" })
  }

  const hashedPassword = await hashPassword(password)

  await db
    .insertInto("users")
    .values({
      id: crypto.randomUUID(),
      username,
      password_hash: hashedPassword,
      email,
      is_active: true,
      last_login: new Date(),
      created_at: new Date(),
      balance: 0,
    })
    .execute()

  const token = jwt.sign({ username }, JWT_SECRET, {
    expiresIn: "7d",
  })

  res.status(200).json({ message: "Login successful", token })
})

app.post("/login", async (req: Request, res: Response) => {
  const { username, password } = req.body

  const { password_hash: hashedPasswordForUser } = (await db
    .selectFrom("users")
    .select("password_hash")
    .where("username", "=", username)
    .executeTakeFirst()) ?? { password: undefined }

  if (
    !hashedPasswordForUser ||
    !(await verifyPassword(password, hashedPasswordForUser))
  ) {
    return res.status(401).json({ message: "Invalid credentials" })
  }

  const token = jwt.sign({ username }, JWT_SECRET, {
    expiresIn: "7d",
  })

  res.status(200).json({ message: "Login successful", token })
})

app.post("/logout", (req: Request, res: Response) => {
  res
    .status(200)
    .json({ message: "Logout successful. Delete your token on client." })
})

app.get("/me", authenticateJWT, (req: Request, res: Response) => {
  res.status(200).json({ user: (req as any).user })
})

app.get("/health", (req: Request, res: Response) => {
  res.status(200).json({ status: "ok", service: "api-caspino" })
})

app.listen(PORT, "0.0.0.0", () => {
  console.log(`HTTP server running on http://0.0.0.0:${PORT}`)
})
