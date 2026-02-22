import { NextRequest, NextResponse } from "next/server"

const API_BASE: string = (process.env.NEXT_PUBLIC_API_URL as string) || (process.env.REACT_APP_API_BASE as string) || (process.env.VITE_API_BASE as string) || (process.env.API_BASE as string) || 'http://localhost:8080';

export async function POST(request: NextRequest) {
  try {
    const sessionCookie = request.cookies.get("SESSION")

    await fetch(`${API_BASE}/login/revoke`, {
      method: "POST",
      headers: {
        Cookie: sessionCookie ? `SESSION=${sessionCookie.value}` : "",
      },
    })

    const res = NextResponse.json({ success: true })

    res.cookies.set("SESSION", "", {
      httpOnly: true,
      secure: true,
      path: "/",
      maxAge: 0,
      sameSite: "strict",
    })

    return res
  } catch {
    return NextResponse.json(
      { error: "Unable to connect to the server." },
      { status: 502 }
    )
  }
}
