import { NextRequest, NextResponse } from "next/server"

const API_BASE: string = (process.env.NEXT_PUBLIC_API_URL as string) || (process.env.REACT_APP_API_BASE as string) || (process.env.VITE_API_BASE as string) || (process.env.API_BASE as string) || 'http://localhost:8080';

export async function POST(request: NextRequest) {
  try {
    const body = await request.text()

    const backendRes = await fetch(`${API_BASE}/login`, {
      method: "POST",
      headers: {
        "Content-Type": "application/x-www-form-urlencoded",
      },
      body,
    })

    const data = await backendRes.json()

    const res = NextResponse.json(data, { status: backendRes.status })

    const setCookie = backendRes.headers.get("set-cookie")
    if (setCookie) {
      res.headers.set("set-cookie", setCookie)
    }

    return res
  } catch {
    return NextResponse.json(
      { error: "Unable to connect to the authentication server." },
      { status: 502 }
    )
  }
}
