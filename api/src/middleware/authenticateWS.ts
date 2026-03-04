import type { IncomingMessage } from "http"
import jwt from "jsonwebtoken"
import "dotenv/config"

const JWT_SECRET = process.env.JWT_SECRET

if (!JWT_SECRET) {
  throw new Error("JWT Secret is missing !")
}

export const authenticateWS = (req: IncomingMessage): jwt.JwtPayload | null => {
  const authHeader = req.headers.authorization

  if (!authHeader || !authHeader.startsWith("Bearer ")) {
    return null
  }

  const token = authHeader.split(" ")[1]

  if (!token) {
    return null
  }

  try {
    const decoded = jwt.verify(token, JWT_SECRET)
    return decoded as jwt.JwtPayload
  } catch {
    return null
  }
}
