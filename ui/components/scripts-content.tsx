"use client"

import { useState, useEffect, useCallback } from "react"
import dynamic from "next/dynamic"
import Link from "next/link"
import Image from "next/image"
import { useTheme } from "next-themes"
import { useAuth } from "@/lib/auth-context"
import { useI18n } from "@/lib/i18n/i18n-context"
import { LanguageSwitcher } from "@/components/language-switcher"
import { ThemeToggle } from "@/components/theme-toggle"
import { Button } from "@/components/ui/button"
import { Input } from "@/components/ui/input"
import { Label } from "@/components/ui/label"
import { Textarea } from "@/components/ui/textarea"
import {
    Dialog,
    DialogContent,
    DialogDescription,
    DialogFooter,
    DialogHeader,
    DialogTitle,
} from "@/components/ui/dialog"
import {
    AlertDialog,
    AlertDialogAction,
    AlertDialogCancel,
    AlertDialogContent,
    AlertDialogDescription,
    AlertDialogFooter,
    AlertDialogHeader,
    AlertDialogTitle,
} from "@/components/ui/alert-dialog"
import {
    ResizablePanelGroup,
    ResizablePanel,
    ResizableHandle,
} from "@/components/ui/resizable"
import {
    Tabs,
    TabsContent,
    TabsList,
    TabsTrigger,
} from "@/components/ui/tabs"
import {
    ArrowLeft,
    CheckCircle2,
    Clock,
    Code2,
    Loader2,
    LogOut,
    Package,
    Pencil,
    Play,
    Plus,
    Save,
    Terminal,
    Trash2,
    XCircle,
} from "lucide-react"

// Monaco editor loaded client-side only (uses web workers)
const Editor = dynamic(() => import("@monaco-editor/react"), {
    ssr: false,
    loading: () => (
        <div className="flex flex-1 items-center justify-center bg-muted/20">
            <Loader2 className="size-6 animate-spin text-muted-foreground" />
        </div>
    ),
})

const API_BASE: string =
    (process.env.NEXT_PUBLIC_API_URL as string) ||
    "http://localhost:8080"

// ── Types ───────────────────────────────────────────────────────────────────

interface PythonScript {
    id: number
    name: string
    description: string | null
    code: string
    createdAt: string
    updatedAt: string
}

interface ExecutionResult {
    output: string
    error: string
    exitCode: number
    executionTimeMs: number
    installLog: string | null
}

// ── Component ───────────────────────────────────────────────────────────────

export function ScriptsContent() {
    const { user, logout } = useAuth()
    const { t } = useI18n()
    const { resolvedTheme } = useTheme()

    // Script list
    const [scripts, setScripts] = useState<PythonScript[]>([])
    const [isLoadingScripts, setIsLoadingScripts] = useState(true)

    // Selected script & editor state
    const [selectedScript, setSelectedScript] = useState<PythonScript | null>(null)
    const [editedCode, setEditedCode] = useState("")
    const [editedName, setEditedName] = useState("")
    const [editedDescription, setEditedDescription] = useState("")
    const [isDirty, setIsDirty] = useState(false)
    const [isEditingName, setIsEditingName] = useState(false)

    // Actions
    const [isSaving, setIsSaving] = useState(false)
    const [isRunning, setIsRunning] = useState(false)
    const [isLoggingOut, setIsLoggingOut] = useState(false)

    // Execution output
    const [executionResult, setExecutionResult] = useState<ExecutionResult | null>(null)

    // Requirements (pip packages, per-script, ephemeral)
    const [requirements, setRequirements] = useState("")

    // Create dialog
    const [showCreateDialog, setShowCreateDialog] = useState(false)
    const [newScriptName, setNewScriptName] = useState("")
    const [newScriptDescription, setNewScriptDescription] = useState("")
    const [isCreating, setIsCreating] = useState(false)
    const [createError, setCreateError] = useState("")

    // Delete dialog
    const [showDeleteDialog, setShowDeleteDialog] = useState(false)
    const [isDeleting, setIsDeleting] = useState(false)

    // ── Data fetching ──────────────────────────────────────────────────────

    const fetchScripts = useCallback(async () => {
        setIsLoadingScripts(true)
        try {
            const res = await fetch(`${API_BASE}/scripts`, { credentials: "include" })
            if (res.ok) {
                const data: PythonScript[] = await res.json()
                setScripts(data)
            }
        } catch (err) {
            console.error("Failed to fetch scripts", err)
        } finally {
            setIsLoadingScripts(false)
        }
    }, [])

    useEffect(() => {
        fetchScripts()
    }, [fetchScripts])

    // ── Script selection ───────────────────────────────────────────────────

    const handleSelectScript = (script: PythonScript) => {
        setSelectedScript(script)
        setEditedCode(script.code)
        setEditedName(script.name)
        setEditedDescription(script.description ?? "")
        setIsDirty(false)
        setExecutionResult(null)
        setRequirements("")
        setIsEditingName(false)
    }

    const handleCodeChange = (value: string | undefined) => {
        const code = value ?? ""
        setEditedCode(code)
        setIsDirty(code !== (selectedScript?.code ?? ""))
    }

    // ── Save ───────────────────────────────────────────────────────────────

    const handleSave = async () => {
        if (!selectedScript) return
        setIsSaving(true)
        try {
            const res = await fetch(`${API_BASE}/scripts/${selectedScript.id}`, {
                method: "PUT",
                headers: { "Content-Type": "application/json" },
                credentials: "include",
                body: JSON.stringify({
                    name: editedName,
                    description: editedDescription || null,
                    code: editedCode,
                }),
            })
            if (res.ok) {
                const updated: PythonScript = await res.json()
                setSelectedScript(updated)
                setScripts((prev) => prev.map((s) => (s.id === updated.id ? updated : s)))
                setIsDirty(false)
            }
        } catch (err) {
            console.error("Failed to save script", err)
        } finally {
            setIsSaving(false)
        }
    }

    // ── Run ────────────────────────────────────────────────────────────────

    const handleRun = async () => {
        if (!selectedScript) return
        // Auto-save if dirty before running
        if (isDirty) await handleSave()
        setIsRunning(true)
        setExecutionResult(null)
        try {
            const body = requirements.trim()
                ? JSON.stringify({ requirements: requirements.trim() })
                : undefined
            const res = await fetch(`${API_BASE}/scripts/${selectedScript.id}/execute`, {
                method: "POST",
                headers: body ? { "Content-Type": "application/json" } : undefined,
                credentials: "include",
                body,
            })
            if (res.ok) {
                const result: ExecutionResult = await res.json()
                setExecutionResult(result)
            }
        } catch (err) {
            console.error("Failed to run script", err)
        } finally {
            setIsRunning(false)
        }
    }

    // ── Create ─────────────────────────────────────────────────────────────

    const handleCreate = async () => {
        if (!newScriptName.trim()) {
            setCreateError(t.scriptNameRequired)
            return
        }
        setIsCreating(true)
        setCreateError("")
        try {
            const res = await fetch(`${API_BASE}/scripts`, {
                method: "POST",
                headers: { "Content-Type": "application/json" },
                credentials: "include",
                body: JSON.stringify({
                    name: newScriptName.trim(),
                    description: newScriptDescription.trim() || null,
                    code: DEFAULT_SCRIPT,
                }),
            })
            if (res.ok) {
                const created: PythonScript = await res.json()
                setScripts((prev) => [created, ...prev])
                handleSelectScript(created)
                setShowCreateDialog(false)
                setNewScriptName("")
                setNewScriptDescription("")
            } else {
                setCreateError(t.scriptCreateError)
            }
        } catch {
            setCreateError(t.scriptCreateError)
        } finally {
            setIsCreating(false)
        }
    }

    // ── Delete ─────────────────────────────────────────────────────────────

    const handleDelete = async () => {
        if (!selectedScript) return
        setIsDeleting(true)
        try {
            const res = await fetch(`${API_BASE}/scripts/${selectedScript.id}`, {
                method: "DELETE",
                credentials: "include",
            })
            if (res.ok) {
                setScripts((prev) => prev.filter((s) => s.id !== selectedScript.id))
                setSelectedScript(null)
                setEditedCode("")
                setExecutionResult(null)
                setShowDeleteDialog(false)
            }
        } catch (err) {
            console.error("Failed to delete script", err)
        } finally {
            setIsDeleting(false)
        }
    }

    // ── Logout ─────────────────────────────────────────────────────────────

    const handleLogout = async () => {
        setIsLoggingOut(true)
        try {
            await logout()
        } finally {
            setIsLoggingOut(false)
        }
    }

    // ── Derived values ─────────────────────────────────────────────────────

    const displayName = user?.completeUsername || user?.username || "User"
    const monacoTheme = resolvedTheme === "dark" ? "vs-dark" : "light"

    // ── Render ─────────────────────────────────────────────────────────────

    return (
        <div className="flex h-svh flex-col bg-background">
            {/* Header */}
            <header className="flex h-16 shrink-0 items-center justify-between border-b bg-background/95 px-4 backdrop-blur supports-[backdrop-filter]:bg-background/60 lg:px-8">
                <div className="flex items-center gap-4">
                    <Link href="/dashboard" className="flex items-center gap-2 text-muted-foreground hover:text-foreground transition-colors">
                        <ArrowLeft className="size-4" />
                        <span className="text-sm hidden sm:inline">{t.backToDashboard}</span>
                    </Link>
                    <div className="flex items-center gap-2">
                        <Image src="/logo.png" alt="Astazou" width={28} height={28} className="rounded" />
                        <div className="flex items-center gap-1.5">
                            <Code2 className="size-4 text-primary" />
                            <span className="font-semibold text-foreground">{t.pythonScripts}</span>
                        </div>
                    </div>
                </div>
                <div className="flex items-center gap-2">
                    <LanguageSwitcher />
                    <ThemeToggle />
                    <Button variant="outline" size="sm" onClick={handleLogout} disabled={isLoggingOut} className="gap-2">
                        {isLoggingOut ? <Loader2 className="size-4 animate-spin" /> : <LogOut className="size-4" />}
                        <span className="hidden sm:inline">{t.signOut}</span>
                    </Button>
                </div>
            </header>

            {/* Body: resizable panels */}
            <div className="flex flex-1 overflow-hidden">
                <ResizablePanelGroup direction="horizontal" className="flex-1">

                    {/* ── Left panel: script list ── */}
                    <ResizablePanel defaultSize={22} minSize={16} maxSize={40} className="flex flex-col border-r">
                        <div className="flex items-center justify-between px-3 py-3 border-b">
                            <span className="text-xs font-semibold uppercase tracking-widest text-muted-foreground">
                                {t.pythonScripts}
                            </span>
                            <Button size="sm" variant="ghost" className="size-7 p-0" onClick={() => setShowCreateDialog(true)}>
                                <Plus className="size-4" />
                            </Button>
                        </div>

                        <div className="flex-1 overflow-y-auto">
                            {isLoadingScripts ? (
                                <div className="flex items-center justify-center py-10">
                                    <Loader2 className="size-5 animate-spin text-muted-foreground" />
                                </div>
                            ) : scripts.length === 0 ? (
                                <div className="flex flex-col items-center justify-center gap-2 px-4 py-10 text-center">
                                    <Terminal className="size-8 text-muted-foreground/50" />
                                    <p className="text-xs text-muted-foreground">{t.noScripts}</p>
                                    <Button size="sm" variant="outline" className="mt-1 text-xs gap-1" onClick={() => setShowCreateDialog(true)}>
                                        <Plus className="size-3" /> {t.createScript}
                                    </Button>
                                </div>
                            ) : (
                                <ul className="py-1">
                                    {scripts.map((s) => (
                                        <li key={s.id}>
                                            <button
                                                onClick={() => handleSelectScript(s)}
                                                className={`w-full text-left px-3 py-2.5 transition-colors hover:bg-accent/50 ${selectedScript?.id === s.id ? "bg-accent" : ""}`}
                                            >
                                                <p className="text-sm font-medium text-foreground truncate">{s.name}</p>
                                                {s.description && (
                                                    <p className="text-xs text-muted-foreground truncate mt-0.5">{s.description}</p>
                                                )}
                                            </button>
                                        </li>
                                    ))}
                                </ul>
                            )}
                        </div>
                    </ResizablePanel>

                    <ResizableHandle withHandle />

                    {/* ── Right panel: editor + output ── */}
                    <ResizablePanel defaultSize={78} className="flex flex-col overflow-hidden">
                        {selectedScript ? (
                            <ResizablePanelGroup direction="vertical" className="flex-1">

                                {/* Editor panel */}
                                <ResizablePanel defaultSize={65} minSize={30} className="flex flex-col">
                                    {/* Editor toolbar */}
                                    <div className="flex items-center justify-between gap-2 border-b px-3 py-2 shrink-0">
                                        <div className="flex items-center gap-2 min-w-0">
                                            {isEditingName ? (
                                                <Input
                                                    className="h-7 text-sm font-semibold w-56"
                                                    value={editedName}
                                                    autoFocus
                                                    onChange={(e) => setEditedName(e.target.value)}
                                                    onBlur={() => setIsEditingName(false)}
                                                    onKeyDown={(e) => { if (e.key === "Enter" || e.key === "Escape") setIsEditingName(false) }}
                                                />
                                            ) : (
                                                <button
                                                    className="flex items-center gap-1.5 group"
                                                    onClick={() => setIsEditingName(true)}
                                                    title={t.editScriptName}
                                                >
                                                    <span className="text-sm font-semibold text-foreground truncate max-w-xs">{editedName}</span>
                                                    <Pencil className="size-3 text-muted-foreground opacity-0 group-hover:opacity-100 transition-opacity" />
                                                </button>
                                            )}
                                            {isDirty && (
                                                <span className="text-xs text-amber-500 font-medium shrink-0">● {t.unsavedChanges}</span>
                                            )}
                                        </div>

                                        <div className="flex items-center gap-1.5 shrink-0">
                                            <span className="text-xs text-muted-foreground hidden sm:inline">{t.availableVariables}</span>
                                            <Button
                                                size="sm"
                                                variant="outline"
                                                className="h-7 gap-1.5 text-xs"
                                                onClick={handleSave}
                                                disabled={isSaving || !isDirty}
                                            >
                                                {isSaving ? <Loader2 className="size-3 animate-spin" /> : <Save className="size-3" />}
                                                {t.saveScript}
                                            </Button>
                                            <Button
                                                size="sm"
                                                className="h-7 gap-1.5 text-xs"
                                                onClick={handleRun}
                                                disabled={isRunning}
                                            >
                                                {isRunning ? <Loader2 className="size-3 animate-spin" /> : <Play className="size-3" />}
                                                {isRunning ? t.scriptRunning : t.runScript}
                                            </Button>
                                            <Button
                                                size="sm"
                                                variant="ghost"
                                                className="h-7 w-7 p-0 text-destructive hover:text-destructive"
                                                onClick={() => setShowDeleteDialog(true)}
                                                title={t.deleteScript}
                                            >
                                                <Trash2 className="size-3.5" />
                                            </Button>
                                        </div>
                                    </div>

                                    {/* Monaco editor */}
                                    <div className="flex-1 overflow-hidden">
                                        <Editor
                                            language="python"
                                            theme={monacoTheme}
                                            value={editedCode}
                                            onChange={handleCodeChange}
                                            options={{
                                                minimap: { enabled: false },
                                                fontSize: 13,
                                                lineNumbers: "on",
                                                wordWrap: "on",
                                                automaticLayout: true,
                                                scrollBeyondLastLine: false,
                                                tabSize: 4,
                                                insertSpaces: true,
                                                padding: { top: 12, bottom: 12 },
                                                renderLineHighlight: "gutter",
                                            }}
                                        />
                                    </div>
                                </ResizablePanel>

                                <ResizableHandle withHandle />

                                {/* Output / Requirements tabbed panel */}
                                <ResizablePanel defaultSize={35} minSize={15} className="flex flex-col">
                                    <Tabs defaultValue="output" className="flex flex-col flex-1 overflow-hidden">
                                        {/* Tab bar */}
                                        <div className="flex items-center border-b px-3 shrink-0 gap-2">
                                            <TabsList className="h-9 bg-transparent p-0 gap-1">
                                                <TabsTrigger
                                                    value="output"
                                                    className="h-8 gap-1.5 rounded-none border-b-2 border-transparent data-[state=active]:border-primary data-[state=active]:bg-transparent text-xs px-2"
                                                >
                                                    <Terminal className="size-3" />
                                                    {t.scriptOutput}
                                                </TabsTrigger>
                                                <TabsTrigger
                                                    value="requirements"
                                                    className="h-8 gap-1.5 rounded-none border-b-2 border-transparent data-[state=active]:border-primary data-[state=active]:bg-transparent text-xs px-2"
                                                >
                                                    <Package className="size-3" />
                                                    {t.requirements}
                                                    {requirements.trim() && (
                                                        <span className="ml-1 size-1.5 rounded-full bg-amber-400 inline-block" />
                                                    )}
                                                </TabsTrigger>
                                            </TabsList>

                                            {/* Status badges – always visible */}
                                            {executionResult && (
                                                <div className="ml-auto flex items-center gap-3">
                                                    {executionResult.exitCode === 0 ? (
                                                        <span className="flex items-center gap-1 text-xs text-emerald-600 dark:text-emerald-400 font-medium">
                                                            <CheckCircle2 className="size-3.5" /> {t.scriptSuccess}
                                                        </span>
                                                    ) : (
                                                        <span className="flex items-center gap-1 text-xs text-destructive font-medium">
                                                            <XCircle className="size-3.5" /> {t.scriptFailed}
                                                        </span>
                                                    )}
                                                    <span className="flex items-center gap-1 text-xs text-muted-foreground">
                                                        <Clock className="size-3" /> {executionResult.executionTimeMs}ms
                                                    </span>
                                                </div>
                                            )}
                                            {isRunning && (
                                                <div className="ml-auto flex items-center gap-1.5 text-xs text-muted-foreground">
                                                    <Loader2 className="size-3 animate-spin" />
                                                    {requirements.trim() ? t.installingDeps : t.scriptRunning}
                                                </div>
                                            )}
                                        </div>

                                        {/* ── Output tab ── */}
                                        <TabsContent value="output" className="flex-1 overflow-hidden mt-0 data-[state=active]:flex flex-col">
                                            <div className="flex-1 overflow-y-auto bg-zinc-950 font-mono text-xs p-3">
                                                {!executionResult && !isRunning && (
                                                    <p className="text-zinc-600">
                                                        {`# ${t.scriptOutput} — ${t.runScript.toLowerCase()} a script to see results here`}
                                                    </p>
                                                )}
                                                {executionResult && (
                                                    <>
                                                        {/* Install log (amber) */}
                                                        {executionResult.installLog && (
                                                            <details open={executionResult.exitCode !== 0} className="mb-2">
                                                                <summary className="text-amber-400 cursor-pointer select-none mb-1">
                                                                    ▸ {t.installLog}
                                                                </summary>
                                                                <pre className="text-amber-300/80 whitespace-pre-wrap break-all pl-3">
                                                                    {executionResult.installLog}
                                                                </pre>
                                                            </details>
                                                        )}
                                                        {/* Script stdout */}
                                                        {executionResult.output && (
                                                            <pre className="text-zinc-200 whitespace-pre-wrap break-all">
                                                                {executionResult.output}
                                                            </pre>
                                                        )}
                                                        {/* Script stderr */}
                                                        {executionResult.error && (
                                                            <pre className="text-red-400 whitespace-pre-wrap break-all mt-1">
                                                                {executionResult.error}
                                                            </pre>
                                                        )}
                                                        {!executionResult.output && !executionResult.error && !executionResult.installLog && (
                                                            <p className="text-zinc-600"># (no output)</p>
                                                        )}
                                                    </>
                                                )}
                                            </div>
                                        </TabsContent>

                                        {/* ── Requirements tab ── */}
                                        <TabsContent value="requirements" className="flex-1 overflow-hidden mt-0 data-[state=active]:flex flex-col">
                                            <div className="flex flex-col flex-1 overflow-hidden p-3 gap-2">
                                                <p className="text-xs text-muted-foreground shrink-0">
                                                    {t.requirementsHint}
                                                </p>
                                                <Textarea
                                                    className="flex-1 resize-none font-mono text-xs bg-zinc-950 text-zinc-200 border-zinc-800 focus-visible:ring-zinc-700 placeholder:text-zinc-600"
                                                    placeholder={t.requirementsPlaceholder}
                                                    value={requirements}
                                                    onChange={(e) => setRequirements(e.target.value)}
                                                    spellCheck={false}
                                                />
                                            </div>
                                        </TabsContent>
                                    </Tabs>
                                </ResizablePanel>

                            </ResizablePanelGroup>
                        ) : (
                            /* No script selected placeholder */
                            <div className="flex flex-1 flex-col items-center justify-center gap-4 text-center p-8">
                                <div className="flex size-16 items-center justify-center rounded-2xl bg-primary/10 text-primary">
                                    <Code2 className="size-8" />
                                </div>
                                <div>
                                    <h2 className="font-semibold text-foreground">{t.noScriptSelected}</h2>
                                    <p className="mt-1 text-sm text-muted-foreground max-w-sm">{t.noScriptSelectedDescription}</p>
                                </div>
                                <Button className="gap-2 mt-2" onClick={() => setShowCreateDialog(true)}>
                                    <Plus className="size-4" /> {t.createScript}
                                </Button>
                            </div>
                        )}
                    </ResizablePanel>
                </ResizablePanelGroup>
            </div>

            {/* ── Create Script Dialog ── */}
            <Dialog open={showCreateDialog} onOpenChange={(open) => { setShowCreateDialog(open); if (!open) { setNewScriptName(""); setNewScriptDescription(""); setCreateError("") } }}>
                <DialogContent>
                    <DialogHeader>
                        <DialogTitle>{t.createScript}</DialogTitle>
                        <DialogDescription>{t.pythonScriptsDescription}</DialogDescription>
                    </DialogHeader>
                    <div className="flex flex-col gap-4 py-2">
                        <div className="flex flex-col gap-1.5">
                            <Label htmlFor="script-name">{t.scriptName}</Label>
                            <Input
                                id="script-name"
                                placeholder={t.scriptNamePlaceholder}
                                value={newScriptName}
                                onChange={(e) => { setNewScriptName(e.target.value); setCreateError("") }}
                                onKeyDown={(e) => e.key === "Enter" && handleCreate()}
                                autoFocus
                            />
                        </div>
                        <div className="flex flex-col gap-1.5">
                            <Label htmlFor="script-desc">{t.scriptDescription}</Label>
                            <Textarea
                                id="script-desc"
                                placeholder={t.scriptDescriptionPlaceholder}
                                value={newScriptDescription}
                                onChange={(e) => setNewScriptDescription(e.target.value)}
                                rows={2}
                            />
                        </div>
                        {createError && <p className="text-sm text-destructive">{createError}</p>}
                    </div>
                    <DialogFooter>
                        <Button variant="outline" onClick={() => setShowCreateDialog(false)}>{t.cancel}</Button>
                        <Button onClick={handleCreate} disabled={isCreating} className="gap-2">
                            {isCreating ? <Loader2 className="size-4 animate-spin" /> : <Plus className="size-4" />}
                            {isCreating ? t.creating : t.createScript}
                        </Button>
                    </DialogFooter>
                </DialogContent>
            </Dialog>

            {/* ── Delete Confirmation ── */}
            <AlertDialog open={showDeleteDialog} onOpenChange={setShowDeleteDialog}>
                <AlertDialogContent>
                    <AlertDialogHeader>
                        <AlertDialogTitle>{t.deleteScript}</AlertDialogTitle>
                        <AlertDialogDescription>{t.deleteScriptDescription}</AlertDialogDescription>
                    </AlertDialogHeader>
                    <AlertDialogFooter>
                        <AlertDialogCancel>{t.cancel}</AlertDialogCancel>
                        <AlertDialogAction
                            onClick={handleDelete}
                            disabled={isDeleting}
                            className="bg-destructive text-destructive-foreground hover:bg-destructive/90"
                        >
                            {isDeleting ? <Loader2 className="size-4 animate-spin mr-2" /> : null}
                            {t.delete}
                        </AlertDialogAction>
                    </AlertDialogFooter>
                </AlertDialogContent>
            </AlertDialog>
        </div>
    )
}

// ── Default starter script ───────────────────────────────────────────────────

const DEFAULT_SCRIPT = `# Available variables:
#   transactions  – list of dicts with keys:
#     id, date, description, amount, type, account_id, tags
#
#   accounts      – list of dicts with keys:
#     id, name, balance, currency
#
# Use print() to produce output.
# Need extra packages? Add them in the "Requirements" tab below.

# Example: total income vs expenses
credits = sum(t['amount'] for t in transactions if t['type'] == 'credit')
debits  = sum(abs(t['amount']) for t in transactions if t['type'] == 'debit')

print(f"Total income  : {credits:,.2f}")
print(f"Total expenses: {debits:,.2f}")
print(f"Net           : {credits - debits:,.2f}")
print(f"Transactions  : {len(transactions)}")
print(f"Accounts      : {len(accounts)}")
`








