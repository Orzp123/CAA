export default function Home() {
  return (
    <div className="flex flex-col items-center justify-center min-h-[80vh] gap-8 px-4">
      <div className="text-center">
        <h1 className="text-5xl font-bold text-white mb-4">Claude Agent Assembly</h1>
        <p className="text-xl text-gray-400 max-w-2xl">
          Visual AI agent workflow builder powered by Spring AI and Temporal.
          Design, deploy and monitor AI agents with a drag-and-drop interface.
        </p>
      </div>
      <div className="grid grid-cols-1 md:grid-cols-3 gap-6 w-full max-w-4xl">
        <a href="/agents" className="block p-6 bg-gray-900 border border-gray-800 rounded-xl hover:border-brand-500 transition-colors">
          <h2 className="text-lg font-semibold mb-2">Agents</h2>
          <p className="text-gray-400 text-sm">Create and manage AI agents backed by OpenAI or Anthropic models.</p>
        </a>
        <a href="/workflow" className="block p-6 bg-gray-900 border border-gray-800 rounded-xl hover:border-brand-500 transition-colors">
          <h2 className="text-lg font-semibold mb-2">Workflows</h2>
          <p className="text-gray-400 text-sm">Design multi-step agent workflows with a visual canvas powered by React Flow.</p>
        </a>
        <a href="/designer" className="block p-6 bg-gray-900 border border-gray-800 rounded-xl hover:border-brand-500 transition-colors">
          <h2 className="text-lg font-semibold mb-2">Page Designer</h2>
          <p className="text-gray-400 text-sm">Build low-code pages using the Amis visual editor and deploy them instantly.</p>
        </a>
      </div>
    </div>
  );
}
