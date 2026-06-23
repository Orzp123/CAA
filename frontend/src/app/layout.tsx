import type { Metadata } from "next";
import { Geist, Geist_Mono } from "next/font/google";
import Link from "next/link";
import "./globals.css";

const geistSans = Geist({ variable: "--font-geist-sans", subsets: ["latin"] });
const geistMono = Geist_Mono({ variable: "--font-geist-mono", subsets: ["latin"] });

export const metadata: Metadata = {
  title: "CAA - Claude Agent Assembly",
  description: "Visual AI agent workflow builder powered by Spring AI and Temporal",
};

export default function RootLayout({ children }: { children: React.ReactNode }) {
  return (
    <html lang="en">
      <body className={`${geistSans.variable} ${geistMono.variable} antialiased bg-gray-950 text-gray-100 min-h-screen`}>
        <nav className="border-b border-gray-800 bg-gray-900 px-6 py-3 flex items-center gap-6">
          <span className="font-bold text-brand-500 text-lg tracking-tight">CAA</span>
          <Link href="/" className="text-sm text-gray-400 hover:text-white transition-colors">Home</Link>
          <Link href="/agents" className="text-sm text-gray-400 hover:text-white transition-colors">Agents</Link>
          <Link href="/workflow" className="text-sm text-gray-400 hover:text-white transition-colors">Workflow</Link>
          <Link href="/designer" className="text-sm text-gray-400 hover:text-white transition-colors">Designer</Link>
        </nav>
        <main className="flex-1">{children}</main>
      </body>
    </html>
  );
}
