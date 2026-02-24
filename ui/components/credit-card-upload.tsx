"use client"

import { useState, useRef } from "react"
import { useI18n } from "@/lib/i18n/i18n-context"
import { Button } from "@/components/ui/button"
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card"
import {
    AlertDialog,
    AlertDialogAction,
    AlertDialogCancel,
    AlertDialogContent,
    AlertDialogDescription,
    AlertDialogHeader,
    AlertDialogTitle,
} from "@/components/ui/alert-dialog"
import { Upload, File, Loader2, CheckCircle, AlertCircle, X } from "lucide-react"

const API_BASE: string = (process.env.NEXT_PUBLIC_API_URL as string) || (process.env.REACT_APP_API_BASE as string) || (process.env.VITE_API_BASE as string) || (process.env.API_BASE as string) || 'http://localhost:8080';

interface CreditCardUploadProps {
    cardId: number
    cardName: string
    onUploadSuccess?: () => void
}

export function CreditCardUpload({ cardId, cardName, onUploadSuccess }: CreditCardUploadProps) {
    const { t } = useI18n()
    const fileInputRef = useRef<HTMLInputElement>(null)
    const [selectedFile, setSelectedFile] = useState<File | null>(null)
    const [isUploading, setIsUploading] = useState(false)
    const [uploadSuccess, setUploadSuccess] = useState(false)
    const [uploadError, setUploadError] = useState("")
    const [showConfirm, setShowConfirm] = useState(false)

    const handleFileSelect = (e: React.ChangeEvent<HTMLInputElement>) => {
        const file = e.target.files?.[0]
        if (!file) return

        // Validate file type
        if (!file.name.toLowerCase().endsWith('.ofx') && !file.name.toLowerCase().endsWith('.txt')) {
            setUploadError("Please select a valid OFX file (.ofx or .txt)")
            return
        }

        setSelectedFile(file)
        setUploadError("")
        setUploadSuccess(false)
    }

    const handleDragOver = (e: React.DragEvent<HTMLDivElement>) => {
        e.preventDefault()
        e.currentTarget.classList.add("border-primary", "bg-primary/5")
    }

    const handleDragLeave = (e: React.DragEvent<HTMLDivElement>) => {
        e.currentTarget.classList.remove("border-primary", "bg-primary/5")
    }

    const handleDrop = (e: React.DragEvent<HTMLDivElement>) => {
        e.preventDefault()
        e.currentTarget.classList.remove("border-primary", "bg-primary/5")

        const file = e.dataTransfer.files?.[0]
        if (!file) return

        if (!file.name.toLowerCase().endsWith('.ofx') && !file.name.toLowerCase().endsWith('.txt')) {
            setUploadError("Please drop a valid OFX file (.ofx or .txt)")
            return
        }

        setSelectedFile(file)
        setUploadError("")
        setUploadSuccess(false)
    }

    const handleUpload = async () => {
        if (!selectedFile) return

        setIsUploading(true)
        setUploadError("")
        setUploadSuccess(false)

        try {
            const formData = new FormData()
            formData.append('file', selectedFile)

            const res = await fetch(`${API_BASE}/credit-cards/ofx/${cardId}`, {
                method: 'POST',
                body: formData,
                credentials: 'include'
            })

            if (res.ok || res.status === 202) {
                setUploadSuccess(true)
                setSelectedFile(null)
                if (fileInputRef.current) {
                    fileInputRef.current.value = ''
                }
                setShowConfirm(false)

                // Call the callback after a delay
                setTimeout(() => {
                    onUploadSuccess?.()
                }, 1500)
            } else if (res.status === 401) {
                setUploadError("Unauthorized. Please log in again.")
            } else if (res.status === 403) {
                setUploadError("You don't have permission to upload to this card.")
            } else if (res.status === 400) {
                setUploadError("Invalid file. Please select a valid OFX file.")
            } else {
                setUploadError("Failed to upload file. Please try again.")
            }
        } catch (error) {
            setUploadError("An error occurred during upload. Please try again.")
        } finally {
            setIsUploading(false)
        }
    }

    const removeFile = () => {
        setSelectedFile(null)
        if (fileInputRef.current) {
            fileInputRef.current.value = ''
        }
        setUploadError("")
        setUploadSuccess(false)
    }

    return (
        <Card>
            <CardHeader>
                <CardTitle className="text-lg flex items-center gap-2">
                    <Upload className="size-5" />
                    Import OFX Statement
                </CardTitle>
                <CardDescription>
                    Upload an OFX file to import transactions for {cardName}
                </CardDescription>
            </CardHeader>
            <CardContent>
                <div className="space-y-4">
                    {/* File Upload Area */}
                    <div
                        onDragOver={handleDragOver}
                        onDragLeave={handleDragLeave}
                        onDrop={handleDrop}
                        className="border-2 border-dashed border-muted-foreground/25 rounded-lg p-8 transition-colors cursor-pointer hover:border-primary/50"
                        onClick={() => fileInputRef.current?.click()}
                    >
                        <input
                            ref={fileInputRef}
                            type="file"
                            accept=".ofx,.txt"
                            onChange={handleFileSelect}
                            className="hidden"
                        />

                        <div className="flex flex-col items-center justify-center text-center">
                            {selectedFile ? (
                                <>
                                    <File className="size-12 text-primary mb-2" />
                                    <p className="font-medium text-foreground">{selectedFile.name}</p>
                                    <p className="text-sm text-muted-foreground">
                                        {(selectedFile.size / 1024).toFixed(2)} KB
                                    </p>
                                </>
                            ) : (
                                <>
                                    <Upload className="size-12 text-muted-foreground mb-2" />
                                    <p className="font-medium text-foreground">
                                        {t.dragAndDrop}
                                    </p>
                                    <p className="text-sm text-muted-foreground">
                                        {t.dragAndDropOr} <span className="text-primary font-medium">click to browse</span>
                                    </p>
                                </>
                            )}
                        </div>
                    </div>

                    {/* Success Message */}
                    {uploadSuccess && (
                        <div className="rounded-lg border border-green-200 bg-green-50 p-4 flex items-start gap-3">
                            <CheckCircle className="size-5 text-green-600 mt-0.5 shrink-0" />
                            <div>
                                <p className="font-medium text-green-900">
                                    File uploaded successfully!
                                </p>
                                <p className="text-sm text-green-800">
                                    Transactions are being processed and will appear in your statement shortly.
                                </p>
                            </div>
                        </div>
                    )}

                    {/* Error Message */}
                    {uploadError && (
                        <div className="rounded-lg border border-red-200 bg-red-50 p-4 flex items-start gap-3">
                            <AlertCircle className="size-5 text-red-600 mt-0.5 shrink-0" />
                            <div>
                                <p className="font-medium text-red-900">Upload failed</p>
                                <p className="text-sm text-red-800">{uploadError}</p>
                            </div>
                        </div>
                    )}

                    {/* Action Buttons */}
                    {selectedFile && !uploadSuccess && (
                        <div className="flex gap-2">
                            <Button
                                onClick={() => setShowConfirm(true)}
                                disabled={isUploading}
                                className="flex-1 gap-2"
                            >
                                {isUploading ? (
                                    <>
                                        <Loader2 className="size-4 animate-spin" />
                                        {t.uploading}
                                    </>
                                ) : (
                                    <>
                                        <Upload className="size-4" />
                                        Upload
                                    </>
                                )}
                            </Button>
                            <Button
                                variant="outline"
                                onClick={removeFile}
                                disabled={isUploading}
                                className="gap-2"
                            >
                                <X className="size-4" />
                                {t.removeFile}
                            </Button>
                        </div>
                    )}
                </div>

                {/* Confirmation Dialog */}
                <AlertDialog open={showConfirm} onOpenChange={setShowConfirm}>
                    <AlertDialogContent>
                        <AlertDialogHeader>
                            <AlertDialogTitle>Upload OFX File?</AlertDialogTitle>
                            <AlertDialogDescription>
                                This will import all transactions from the OFX file to {cardName}. This action cannot be undone.
                            </AlertDialogDescription>
                        </AlertDialogHeader>
                        <AlertDialogCancel>Cancel</AlertDialogCancel>
                        <AlertDialogAction onClick={handleUpload} disabled={isUploading}>
                            {isUploading ? (
                                <>
                                    <Loader2 className="size-4 animate-spin mr-2" />
                                    Uploading...
                                </>
                            ) : (
                                "Upload"
                            )}
                        </AlertDialogAction>
                    </AlertDialogContent>
                </AlertDialog>
            </CardContent>
        </Card>
    )
}


