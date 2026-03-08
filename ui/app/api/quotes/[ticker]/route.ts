import { NextRequest, NextResponse } from "next/server"

const API_BASE: string =
  (process.env.NEXT_PUBLIC_API_URL as string) ||
  (process.env.API_BASE as string) ||
  "http://localhost:8080"

export async function GET(
  request: NextRequest,
  { params }: { params: Promise<{ ticker: string }> }
) {
  try {
    const sessionCookie = request.cookies.get("SESSION")
    if (!sessionCookie) {
      return NextResponse.json({ error: "Unauthorized" }, { status: 401 })
    }

    const { ticker } = await params

    const backendRes = await fetch(`${API_BASE}/quotes/${encodeURIComponent(ticker)}`, {
      headers: {
        Cookie: `SESSION=${sessionCookie.value}`,
      },
    })

    if (backendRes.status === 404) {
      return NextResponse.json({ error: "Ticker not found" }, { status: 404 })
    }

    if (!backendRes.ok) {
      return NextResponse.json(null, { status: backendRes.status })
    }

    const data = await backendRes.json()
    return NextResponse.json(data)
  } catch {
    return NextResponse.json(
      { error: "Unable to connect to the server." },
      { status: 502 }
    )
  }
}

