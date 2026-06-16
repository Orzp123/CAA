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
        const { Editor, ShortcutKey } = await import("amis-editor");
        const amis = await import("amis");
        const React = (await import("react")).default;
        const ReactDOM = (await import("react-dom")).default;

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

        ShortcutKey.bindShortCut();

        ReactDOM.render(
          React.createElement(Editor, {
            theme: "dark",
            preview: false,
            value: initialSchema,
            onChange: (schema: unknown) => {
              console.log("Schema updated:", schema);
            },
          }),
          containerRef.current
        );

        editor = { unmount: () => ReactDOM.unmountComponentAtNode(containerRef.current!) };
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

  return <div ref={containerRef} className="flex-1 overflow-hidden" style={{ height: "calc(100vh - 100px)" }} />;
}
