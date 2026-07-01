"use client";

import { useEffect, useRef } from "react";

// Amis editor is loaded dynamically to avoid SSR issues
export default function PageDesigner() {
  const containerRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    let editor: { unmount?: () => void } | null = null;

    async function initEditor() {
      if (!containerRef.current) return;
      try {
        const { Editor } = await import("amis-editor");
        const React = (await import("react")).default;
        const { createRoot } = await import("react-dom/client");

        const initialSchema = {
          type: "page",
          title: "New Page",
          body: [
            {
              type: "tpl",
              tpl: "Drag components from the panel on the left to build your page.",
              className: "text-muted",
            },
          ],
        };

        const root = createRoot(containerRef.current);
        root.render(
          React.createElement(Editor, {
            theme: "dark",
            preview: false,
            // amis-editor SchemaObject is a complex union; cast through unknown for schema literal
            // eslint-disable-next-line @typescript-eslint/no-explicit-any
            value: initialSchema as unknown as any,
            onChange: (schema: unknown) => {
              console.log("Schema updated:", schema);
            },
          })
        );

        editor = { unmount: () => root.unmount() };
      } catch (err) {
        console.error("Failed to load Amis editor:", err);
        if (containerRef.current) {
          containerRef.current.innerHTML =
            '<div style="padding:2rem;color:#9ca3af">Amis editor failed to load. Ensure amis-editor is installed.</div>';
        }
      }
    }

    initEditor();
    return () => editor?.unmount?.();
  }, []);

  return <div ref={containerRef} className="flex-1 overflow-hidden h-[calc(100vh-100px)]" />;
}
