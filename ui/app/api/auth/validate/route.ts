import { NextRequest, NextResponse } from "next/server"

const API_BASE: string = (process.env.NEXT_PUBLIC_API_URL as string) || (process.env.REACT_APP_API_BASE as string) || (process.env.VITE_API_BASE as string) || (process.env.API_BASE as string) || 'http://localhost:8080';

export async function GET(request: NextRequest) {
  try {
    const sessionCookie = request.cookies.get("SESSION")

    const backendRes = await fetch(`${API_BASE}/login/validate`, {
      headers: {
        Cookie: sessionCookie ? `SESSION=${sessionCookie.value}` : "",
      },
    })

    if (!backendRes.ok) {
      return NextResponse.json(null, { status: 401 })
    }

    const data = await backendRes.json()
    return NextResponse.json(data)
  } catch {
    return NextResponse.json(null, { status: 502 })
  }
}
