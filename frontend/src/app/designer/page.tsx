"use client";

import dynamic from "next/dynamic";

const PageDesigner = dynamic(
  () => import("@/components/designer/PageDesigner"),
  { ssr: false, loading: () => <div className="p-8 text-gray-400">Loading designer...</div> }
);

export default function DesignerPage() {
  return (
    <div className="flex flex-col h-[calc(100vh-52px)]">
      <div className="flex items-center justify-between px-6 py-3 border-b border-gray-800 bg-gray-900">
        <h1 className="text-lg font-semibold">Page Designer</h1>
        <div className="flex gap-2">
          <button className="px-3 py-1.5 text-sm bg-gray-800 hover:bg-gray-700 rounded-lg transition-colors">
            Preview
          </button>
          <button className="px-3 py-1.5 text-sm bg-brand-500 hover:bg-brand-600 rounded-lg transition-colors">
            Publish
          </button>
        </div>
      </div>
      <PageDesigner />
    </div>
  );
}
