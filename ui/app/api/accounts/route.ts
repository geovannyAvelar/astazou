import { NextRequest, NextResponse } from "next/server"

const API_BASE: string = (process.env.NEXT_PUBLIC_API_URL as string) || (process.env.REACT_APP_API_BASE as string) || (process.env.VITE_API_BASE as string) || (process.env.API_BASE as string) || 'http://localhost:8080';

export async function POST(request: NextRequest) {
  try {
    const sessionCookie = request.cookies.get("SESSION")
    if (!sessionCookie) {
      return NextResponse.json({ error: "Unauthorized" }, { status: 401 })
    }

    const body = await request.json()

    const backendRes = await fetch(`${API_BASE}/accounts`, {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
        Cookie: `SESSION=${sessionCookie.value}`,
      },
      body: JSON.stringify(body),
    })

    const data = await backendRes.json()
    return NextResponse.json(data, { status: backendRes.status })
  } catch {
    return NextResponse.json(
      { error: "Unable to connect to the server." },
      { status: 502 }
    )
  }
}

export async function GET(request: NextRequest) {
  try {
    const sessionCookie = request.cookies.get("SESSION")
    if (!sessionCookie) {
      return NextResponse.json({ error: "Unauthorized" }, { status: 401 })
    }

    const { searchParams } = new URL(request.url)
    const page = searchParams.get("page") || "1"
    const itemsPerPage = searchParams.get("itemsPerPage") || "10"

    const backendRes = await fetch(
      `${API_BASE}/accounts?page=${page}&itemsPerPage=${itemsPerPage}`,
      {
        headers: {
          Cookie: `SESSION=${sessionCookie.value}`,
        },
      }
    )

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
